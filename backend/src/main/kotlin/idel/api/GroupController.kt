package idel.api

import arrow.core.Either
import arrow.core.None
import arrow.core.computations.either
import arrow.core.flatten
import idel.domain.*
import idel.infrastructure.repositories.CouchbaseTransactions
import idel.infrastructure.security.IdelOAuth2User
import mu.KotlinLogging
import org.springframework.core.convert.converter.Converter
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.lang.RuntimeException
import java.util.*

@RestController
@RequestMapping("/groups")
class GroupController(
    private val couchbaseTransactions: CouchbaseTransactions,
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val groupService: GroupService,
    apiSecurityFactory: ApiSecurityFactory
) {
    companion object {
       const val NAME_FILTER_MIN_SYMBOLS = 3
    }

    val log = KotlinLogging.logger {}

    val factory = GroupFactory()

    val security = apiSecurityFactory.create(log)

    @PostMapping
    fun create(
        @RequestBody properties: GroupEditableProperties,
        @AuthenticationPrincipal user: IdelOAuth2User
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
        @AuthenticationPrincipal user: IdelOAuth2User,
        @PathVariable groupId: String
    ): EntityOrError<Group> {
        val identity = GroupSecurity.IdGroupIdentity(groupId)
        return security.group.asDomainMember(identity, user) {
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
        @AuthenticationPrincipal user: IdelOAuth2User,
        @RequestParam key: String
    ): EntityOrError<Group> {
        val identity = GroupSecurity.JoiningKeyGroupIdentity(key)
        return security.group.asDomainMember(identity, user) {
            groupRepository.loadByJoiningKey(key)
        }
    }

    private fun <T>ifNameValid(name : String?, actionIfValid : () -> EntityOrError<T>) : EntityOrError<T> {
        return if (!name.isNullOrBlank() && name.length < NAME_FILTER_MIN_SYMBOLS) {
            DataOrError.incorrectArgument("name","name should contains minimum $NAME_FILTER_MIN_SYMBOLS symbols")
        } else {
            actionIfValid()
        }
    }

    @GetMapping(params = ["userId"])
    fun loadByUser(
        @AuthenticationPrincipal user: IdelOAuth2User,
        @RequestParam userId: String,
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false, defaultValue = "") order: GroupOrdering,
        pagination: Repository.Pagination
    ): EntityOrError<List<Group>> {
        return ifNameValid(name) {
            security.user.asHimSelf(userId, user) {
                groupRepository.loadByUser(userId, name, pagination, order)
            }
        }
    }

    @GetMapping(params = ["onlyAvailable"])
    fun loadAvailableForUser(
        @AuthenticationPrincipal user: IdelOAuth2User,
        @RequestParam(defaultValue = "true") onlyAvailable: Boolean,
        @RequestParam(required = false) name : String?,
        @RequestParam(defaultValue = "") ordering: GroupOrdering,
        pagination: Repository.Pagination
    ): EntityOrError<List<Group>> {
        return ifNameValid(name) {
            val result = groupRepository.loadOnlyAvailable(
                userId = user.id,
                userDomain = user.domain,
                partOfName = name,
                pagination = pagination,
                ordering = ordering
            )
            DataOrError.fromEither(result, log)
        }
    }


    @GetMapping("/{groupId}/members")
    fun loadUsers(
        @AuthenticationPrincipal user: IdelOAuth2User,
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
        @AuthenticationPrincipal user: IdelOAuth2User,
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
        @AuthenticationPrincipal user: IdelOAuth2User,
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
        @AuthenticationPrincipal user: IdelOAuth2User,
        @PathVariable groupId: String
    ) : EntityOrError<Group> {
        return security.group.asAdmin(groupId, user) {
            groupRepository.mutate(groupId) {entity: Group ->
                entity.regenerateJoiningKey()
            }
        }
    }


    data class RolePatch(val roleInGroup: GroupMemberRole)

    @PatchMapping("/{groupId}/members/{userId}/role-in-group")
    fun changeMemberRole(
        @AuthenticationPrincipal user: IdelOAuth2User,
        @PathVariable groupId: String,
        @PathVariable userId: String,
        @RequestBody rolePatch: RolePatch
    ): EntityOrError<GroupMember> {
        return security.group.asAdmin(groupId, user) {
            groupService.changeRoleInGroup(groupId, userId, rolePatch.roleInGroup)
        }
    }


    @DeleteMapping("/{groupId}/members/{memberId}")
    fun removeMember(
        @AuthenticationPrincipal user: IdelOAuth2User,
        @PathVariable groupId: String,
        @PathVariable memberId: String
    ): EntityOrError<String> {
        return security.groupMember.asAdminOrHimSelf(groupId, memberId, user) {
            groupMemberRepository.removeFromGroup(groupId, memberId).map {"ok"}
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