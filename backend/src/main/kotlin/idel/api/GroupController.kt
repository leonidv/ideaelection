package idel.api

import arrow.core.Either
import arrow.core.computations.either
import arrow.core.flatMap
import arrow.core.flatten
import idel.domain.*

import mu.KotlinLogging
import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/groups")
class GroupController(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val groupService: GroupService,
    private val joinRequestRepository: JoinRequestRepository,
    private val inviteRepository: InviteRepository,
    private val security: ApiSecurity
) : DataOrErrorHelper {
    companion object {
        const val NAME_FILTER_MIN_SYMBOLS = 3
        private val notificationNameRegexp = "!!!N(\\d+)$".toRegex()
    }

    override val log = KotlinLogging.logger {}

    val factory = GroupFactory()

    // simple hack to debug notifications without notifications
    val notificationsFromGroupName = "!!!N(\\d+)$".toRegex()

    data class GroupsNotification(val groupId: GroupId, val count: Int)

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


    private fun enrichAvailableGroups(user: User, groups: List<Group>): Either<DomainError, GroupsPayload> {
        return either.eager {
            val joinRequests = groups
                .filter {
                    // public group hasn't joinrequests, because they are auto approved
                    it.entryMode != GroupEntryMode.PUBLIC
                }
                .mapNotNull {group ->
                    val eJoinRequest = joinRequestRepository.loadUnresolved(user, group)
                    notFoundToNull(eJoinRequest).bind()
                }

            val invites = groups.mapNotNull {group ->
                val eInvite = inviteRepository.loadUnresolved(user, group)
                notFoundToNull(eInvite).bind()
            }

            GroupsPayload(groups, joinRequests.toSet(), invites.toSet(), null)
        }
    }

    private fun enrichUsersGroups(user: User, groups: List<Group>): Either<DomainError, GroupsPayload> {
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
    ): ResponseDataOrError<Group> {
        return fTransaction {
            either.eager<DomainError, Group> {
                val group = factory.createGroup(UserInfo.ofUser(user), properties).bind()
                val creatorAsAdmin = GroupMember.of(group.id, user, GroupMemberRole.GROUP_ADMIN)
                groupRepository.add(group).bind()
                groupMemberRepository.add(creatorAsAdmin).bind()
                group
            }
        }.asHttpResponse()
    }


    @GetMapping("/{groupId}")
    fun load(
        @AuthenticationPrincipal user: User,
        @PathVariable groupId: GroupId
    ): ResponseDataOrError<Group> {
        val identity = GroupSecurity.IdGroupIdentity(groupId)
        return security.group.asDomainMemberOrCreator(identity, user) {
            fTransaction {
                val result = either.eager<DomainError, Either<DomainError, Group>> {
                    val group: Group = groupRepository.load(groupId).bind()
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
        }.asHttpResponse()
    }


    @GetMapping(params = ["key"])
    fun loadByJoiningKey(
        @AuthenticationPrincipal user: User,
        @RequestParam key: String
    ): ResponseDataOrError<Group> {
        val identity = GroupSecurity.JoiningKeyGroupIdentity(key)
        return security.group.asDomainMemberOrCreator(identity, user) {
            fTransaction {
                groupRepository.loadByJoiningKey(key)
            }
        }.asHttpResponse()
    }

    private fun <T> ifNameValid(name: String?, actionIfValid: () -> ResponseDataOrError<T>): ResponseDataOrError<T> {
        return if (!name.isNullOrBlank() && name.length < NAME_FILTER_MIN_SYMBOLS) {
            DataOrError.incorrectArgument("name", "name should contains minimum $NAME_FILTER_MIN_SYMBOLS symbols")
        } else {
            actionIfValid()
        }
    }

    @GetMapping(params = ["userId"])
    fun listByUser(
        @AuthenticationPrincipal user: User,
        @RequestParam userId: UUID,
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false, defaultValue = "") order: GroupOrdering,
        pagination: Repository.Pagination
    ): ResponseDataOrError<GroupsPayload> {
        return ifNameValid(name) {
            security.user.asHimSelf(userId, user) {
                groupRepository
                    .listByUser(userId, name, pagination, order)
                    .flatMap {groups -> enrichUsersGroups(user, groups)}
            }.asHttpResponse()
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
    ): ResponseDataOrError<GroupsPayload> {
        return ifNameValid(name) {
            fTransaction {
                groupRepository.listOnlyAvailable(
                    userId = user.id,
                    userDomain = user.domain,
                    partOfName = name,
                    pagination = pagination,
                    ordering = ordering
                ).flatMap {enrichAvailableGroups(user, it)}
            }.asHttpResponse()

        }
    }


    @GetMapping("/{groupId}/members")
    fun listUsers(
        @AuthenticationPrincipal user: User,
        @PathVariable groupId: GroupId,
        @RequestParam username: String?,
        pagination: Repository.Pagination
    ): ResponseDataOrError<List<GroupMember>> {
        return security.group.asMember(groupId, user) {_ ->
            groupMemberRepository.listByGroup(
                groupId = groupId,
                pagination = pagination,
                usernameFilter = username,
                roleFilter = null
            )
        }.asHttpResponse()
    }


    @PatchMapping("/{groupId}")
    fun updateInfo(
        @AuthenticationPrincipal user: User,
        @PathVariable groupId: GroupId,
        @RequestBody properties: GroupEditableProperties
    ): ResponseDataOrError<Group> {
        return security.group.asAdmin(groupId, user) {group ->
            either.eager {
                val nextGroup = group.update(properties).bind()
                groupRepository.update(nextGroup).bind()
                nextGroup
            }
        }.asHttpResponse()
    }


    @DeleteMapping("/{groupId}")
    fun archive(
        @AuthenticationPrincipal user: User,
        @PathVariable groupId: GroupId
    ): ResponseDataOrError<Group> {
        return security.group.asAdmin(groupId, user) {group ->
            groupRepository.update(group.delete())
        }.asHttpResponse()

    }

    @DeleteMapping("/{groupId}/joining-key")
    fun resetJoiningKey(
        @AuthenticationPrincipal user: User,
        @PathVariable groupId: GroupId
    ): ResponseDataOrError<Group> {
        return security.group.asAdmin(groupId, user) {group ->
            groupRepository.mutate(groupId) {it.regenerateJoiningKey()}
        }.asHttpResponse()
    }


    data class RolePatch(val roleInGroup: GroupMemberRole)

    @PatchMapping("/{groupId}/members/{userId}/role-in-group")
    fun changeMemberRole(
        @AuthenticationPrincipal user: User,
        @PathVariable groupId: GroupId,
        @PathVariable userId: UserId,
        @RequestBody rolePatch: RolePatch
    ): ResponseDataOrError<GroupMember> {
        return security.group.asAdmin(groupId, user) {_ ->
            groupService.changeRoleInGroup(groupId, userId, rolePatch.roleInGroup)
        }.asHttpResponse()
    }


    @DeleteMapping("/{groupId}/members/{memberId}")
    fun removeMember(
        @AuthenticationPrincipal user: User,
        @PathVariable groupId: GroupId,
        @PathVariable memberId: UserId
    ): ResponseDataOrError<String> {
        return security.groupMember.asAdminOrHimSelf(groupId, memberId, user) {
            groupService.removeUser(groupId, memberId).map {"OK"}
        }.asHttpResponse()
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