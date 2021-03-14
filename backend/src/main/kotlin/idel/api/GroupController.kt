package idel.api

import arrow.core.*
import arrow.core.extensions.fx
import idel.domain.*
import idel.infrastructure.repositories.CouchbaseTransactions
import idel.infrastructure.security.IdelOAuth2User
import mu.KotlinLogging
import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.Optional
import kotlin.IllegalArgumentException

@RestController
@RequestMapping("/groups")
class GroupController(
        private val couchbaseTransactions: CouchbaseTransactions,
        private val groupRepository: GroupRepository,
        private val groupMemberRepository: GroupMemberRepository,
        private val groupService: GroupService,
        apiSecurityFactory: ApiSecurityFactory
) {
    val log = KotlinLogging.logger {}

    val factory = GroupFactory()

    val security = apiSecurityFactory.create(log)

    @PostMapping
    fun create(
            @RequestBody properties: GroupEditableProperties,
            @AuthenticationPrincipal user: IdelOAuth2User
    ): EntityOrError<Group> {


        val x = Either.fx<Exception,Group> {
            val (group) = factory.createGroup(UserInfo.ofUser(user),properties)

            val creatorAsAdmin = GroupMember.of(group.id,user, GroupMemberRole.GROUP_ADMIN)
            couchbaseTransactions.transaction { ctx ->
                groupRepository.add(group, ctx)
                groupMemberRepository.add(creatorAsAdmin, ctx)
            }

            group
        }

        return DataOrError.fromEither(x,log)
    }


    @GetMapping("/{groupId}")
    fun load(@AuthenticationPrincipal user: IdelOAuth2User,
             @PathVariable groupId: String) : EntityOrError<Group> {
        return DataOrError.fromEither( groupRepository.load(groupId), log)
    }


    @GetMapping(params = ["userId"])
    fun loadByUser(
            @AuthenticationPrincipal user: IdelOAuth2User,
            @RequestParam userId : String,
            @RequestParam(required = false, defaultValue = "") order: GroupOrdering,
            pagination: Repository.Pagination
    ) : EntityOrError<List<Group>> {
        return security.user.asHimSelf(userId, user) {
            groupRepository.loadByUser(userId,pagination, order)
        }
    }

    @GetMapping(params = ["onlyAvailable"])
    fun loadAvailableForUser(
            @AuthenticationPrincipal user : IdelOAuth2User,
            @RequestParam(defaultValue = "true") onlyAvailable: Boolean,
            @RequestParam(defaultValue = "") ordering: GroupOrdering,
            pagination: Repository.Pagination
    ) : EntityOrError<List<Group>> {
        val result = groupRepository.loadOnlyAvailable(pagination, ordering)
        return DataOrError.fromEither(result, log)
    }

    @GetMapping("/{groupId}/members")
    fun loadUsers(
        @AuthenticationPrincipal user : IdelOAuth2User,
        @PathVariable groupId: String,
        @RequestParam username : Optional<String>,
        pagination: Repository.Pagination
    ) : EntityOrError<List<GroupMember>> {
        return security.group.asMember(groupId, user) {
            groupMemberRepository.loadByGroup(groupId,pagination,username.asOption(), roleFilter = Option.empty())
        }
    }



    data class RolePatch(val roleInGroup : GroupMemberRole)

    @PatchMapping("/{groupId}/members/{userId}/role-in-group")
    fun changeMemberRole(
        @AuthenticationPrincipal user : IdelOAuth2User,
        @PathVariable groupId: String,
        @PathVariable userId: String,
        @RequestBody rolePatch: RolePatch
    ) : EntityOrError<GroupMember> {
        return security.group.asAdmin(groupId, user) {
            groupService.changeRole(groupId, userId, rolePatch.roleInGroup)
        }
    }



    @DeleteMapping("/{groupId}/members/{memberId}")
    fun removeMember(
        @AuthenticationPrincipal user : IdelOAuth2User,
        @PathVariable groupId: String,
        @PathVariable memberId : String
    ) : EntityOrError<String> {
        return security.groupMember.asAdminOrHimSelf(groupId, memberId, user) {
            groupMemberRepository.removeFromGroup(groupId, memberId).map {"ok"}
        }
    }
}

class StringToGroupSortingConverter : Converter<String, GroupOrdering> {
    val DEFAULT = GroupOrdering.CTIME_DESC

    override fun convert(source: String): GroupOrdering {
        return if (source.isNullOrBlank()) {
            DEFAULT
        } else {
            try {
                GroupOrdering.valueOf(source.toUpperCase())
            } catch (ex: IllegalArgumentException) {
                DEFAULT
            }
        }
    }
}