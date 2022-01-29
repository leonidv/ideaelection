package idel.api

import arrow.core.Either
import arrow.core.None
import arrow.core.computations.either
import arrow.core.flatMap
import arrow.core.flatten
import idel.domain.*
import idel.infrastructure.repositories.CouchbaseTransactions
import mu.KotlinLogging
import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/groups")
class GroupController(
    private val couchbaseTransactions: CouchbaseTransactions,
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val groupService: GroupService,
    private val joinRequestRepository: JoinRequestRepository,
    private val inviteRepository: InviteRepository,
    apiSecurityFactory: ApiSecurityFactory
) {
    companion object {
        const val NAME_FILTER_MIN_SYMBOLS = 3
        private val notificationNameRegexp = "!!!N(\\d+)$".toRegex()
    }

    val log = KotlinLogging.logger {}

    val factory = GroupFactory()

    val security = apiSecurityFactory.create(log)

    // simple hack to debug notifications without notifications
    val notificationsFromGroupName = "!!!N(\\d+)$".toRegex()

    data class GroupsNotification(val groupId: String, val count: Int)

    data class GroupsPayload(
        val groups: List<Group>,
        val joinRequests: Set<JoinRequest>?,
        val invites: Set<Invite>?,
        val notifications: Set<GroupsNotification>?
    ) {
        companion object {
            fun onlyGroups(groups: List<Group>) = GroupsPayload(groups, null, null, null)
        }
    }


    private fun enrichAvailableGroups(user: User, groups: List<Group>): Either<Exception, GroupsPayload> {
        return either.eager {
            val joinRequests = groups
                .filter {
                    // public group hasn't joinrequests, because they are auto approved
                    it.entryMode != GroupEntryMode.PUBLIC
                }
                .mapNotNull {group ->
                    val eJoinRequest = joinRequestRepository.load(user, group)
                    notFoundToNull(eJoinRequest).bind()
                }

            val invites = groups.mapNotNull {group ->
                val eInvite = inviteRepository.load(user, group)
                notFoundToNull(eInvite).bind()
            }

            GroupsPayload(groups, joinRequests.toSet(), invites.toSet(), null)
        }
    }

    private fun enrichUsersGroups(user: User, groups: List<Group>): Either<Exception, GroupsPayload> {
        val notifications = groups.map {group ->
            notificationsFromGroupName.find(group.name)?.let {it.groupValues[1].toIntOrNull()}
        }.zip(groups).mapNotNull {(count, group) ->
            if (count != null) {
                GroupsNotification(group.id, count)
            } else {
                null
            }
        }.toSet()
        val payload = GroupsPayload(
            groups = groups,
            joinRequests = null,
            invites = null,
            notifications = notifications
        )
        return Either.Right(payload)
    }

    @PostMapping
    fun create(
        @RequestBody properties: GroupEditableProperties,
        @AuthenticationPrincipal user: User
    ): EntityOrError<Group> {
        val x = either.eager<Exception, Group> {
            val group = factory.createGroup(UserInfo.ofUser(user), properties).bind()

            val creatorAsAdmin = GroupMember.of(group.id, user, GroupMemberRole.GROUP_ADMIN)
            couchbaseTransactions.transaction {ctx ->
                groupRepository.add(group, ctx)
                groupMemberRepository.add(creatorAsAdmin, ctx)
            }.bind()

            group
        }

        return DataOrError.fromEither(x, log)
    }


    @GetMapping("/{groupId}")
    fun load(
        @AuthenticationPrincipal user: User,
        @PathVariable groupId: String
    ): EntityOrError<Group> {
        val identity = GroupSecurity.IdGroupIdentity(groupId)
        return security.group.asDomainMemberOrCreator(identity, user) {
            val result = either.eager<Exception, Either<Exception, Group>> {
                val group = groupRepository.load(groupId).bind()
                if (group.entryMode == GroupEntryMode.PRIVATE) {
                    val isMember = groupMemberRepository.isMember(groupId, user.id).bind()
                    if (isMember) {
                        Either.Right(group)
                    } else {
                        Either.Left(EntityNotFound("group", groupId))
                    }
                } else {
                    Either.Right(group)
                }
            }.flatten()
            result
        }
    }


    @GetMapping(params = ["key"])
    fun loadByJoiningKey(
        @AuthenticationPrincipal user: User,
        @RequestParam key: String
    ): EntityOrError<Group> {
        val identity = GroupSecurity.JoiningKeyGroupIdentity(key)
        return security.group.asDomainMemberOrCreator(identity, user) {
            groupRepository.loadByJoiningKey(key)
        }
    }

    private fun <T> ifNameValid(name: String?, actionIfValid: () -> EntityOrError<T>): EntityOrError<T> {
        return if (!name.isNullOrBlank() && name.length < NAME_FILTER_MIN_SYMBOLS) {
            DataOrError.incorrectArgument("name", "name should contains minimum $NAME_FILTER_MIN_SYMBOLS symbols")
        } else {
            actionIfValid()
        }
    }

    @GetMapping(params = ["userId"])
    fun listByUser(
        @AuthenticationPrincipal user: User,
        @RequestParam userId: String,
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false, defaultValue = "") order: GroupOrdering,
        pagination: Repository.Pagination
    ): EntityOrError<GroupsPayload> {
        return ifNameValid(name) {
            security.user.asHimSelf(userId, user) {
                groupRepository
                    .loadByUser(userId, name, pagination, order)
                    .flatMap {groups -> enrichUsersGroups(user, groups)}
            }
        }
    }

    @GetMapping(params = ["onlyAvailable"])
    fun listAvailableForUser(
        @AuthenticationPrincipal user: User,
        @RequestParam(defaultValue = "true") onlyAvailable: Boolean,
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false, defaultValue = "false") onlyWithJoinRequest: Boolean,
        @RequestParam(required = false, defaultValue = "false") onlyWithInvites: Boolean,
        @RequestParam(defaultValue = "") ordering: GroupOrdering,
        pagination: Repository.Pagination
    ): EntityOrError<GroupsPayload> {
        return ifNameValid(name) {
            val result = groupRepository.loadOnlyAvailable(
                userId = user.id,
                userDomain = user.domain,
                partOfName = name,
                pagination = pagination,
                ordering = ordering
            ).flatMap {enrichAvailableGroups(user, it)}

            DataOrError.fromEither(result, log)
        }
    }


    @GetMapping("/{groupId}/members")
    fun listUsers(
        @AuthenticationPrincipal user: User,
        @PathVariable groupId: String,
        @RequestParam username: Optional<String>,
        pagination: Repository.Pagination
    ): EntityOrError<List<GroupMember>> {
        return security.group.asMember(groupId, user) {
            groupMemberRepository.loadByGroup(groupId, pagination, username.asOption(), roleFilter = None)
        }
    }


    @PatchMapping("/{groupId}")
    fun updateInfo(
        @AuthenticationPrincipal user: User,
        @PathVariable groupId: String,
        @RequestBody properties: GroupEditableProperties
    ): EntityOrError<Group> {
        return security.group.asAdmin(groupId, user) {
            groupRepository.possibleMutate(groupId) {group ->
                group.update(properties)
            }
        }
    }

    @DeleteMapping("/{groupId}")
    fun archive(
        @AuthenticationPrincipal user: User,
        @PathVariable groupId: String
    ): EntityOrError<Group> {
        return security.group.asAdmin(groupId, user) {
            either.eager {
                val group = groupRepository.load(groupId).bind()
                val nextGroup = groupRepository.mutate(groupId) {
                    group.delete()
                }.bind()
                nextGroup
            }
        }
    }

    @DeleteMapping("/{groupId}/joining-key")
    fun resetJoiningKey(
        @AuthenticationPrincipal user: User,
        @PathVariable groupId: String
    ): EntityOrError<Group> {
        return security.group.asAdmin(groupId, user) {
            groupRepository.mutate(groupId) {entity: Group ->
                entity.regenerateJoiningKey()
            }
        }
    }


    data class RolePatch(val roleInGroup: GroupMemberRole)

    @PatchMapping("/{groupId}/members/{userId}/role-in-group")
    fun changeMemberRole(
        @AuthenticationPrincipal user: User,
        @PathVariable groupId: String,
        @PathVariable userId: String,
        @RequestBody rolePatch: RolePatch
    ): EntityOrError<GroupMember> {
        return security.group.asAdmin(groupId, user) {
            groupService.changeRoleInGroup(groupId, userId, rolePatch.roleInGroup)
        }
    }


    @DeleteMapping("/{groupId}/members/{removedUserId}")
    fun removeMember(
        @AuthenticationPrincipal user: User,
        @PathVariable groupId: String,
        @PathVariable removedUserId: String
    ): EntityOrError<String> {
        return security.groupMember.asAdminOrHimSelf(groupId, removedUserId, user) {
            groupService.removeUser(groupId, removedUserId).map {"OK"}
        }
    }
}

class StringToGroupSortingConverter : Converter<String, GroupOrdering> {
    companion object {
        val DEFAULT = GroupOrdering.CTIME_DESC
    }

    override fun convert(source: String): GroupOrdering {
        @Suppress("UselessCallOnNotNull")
        return if (source.isNullOrBlank()) {
            DEFAULT
        } else {
            try {
                GroupOrdering.valueOf(source.uppercase(Locale.getDefault()))
            } catch (ex: IllegalArgumentException) {
                DEFAULT
            }
        }
    }
}