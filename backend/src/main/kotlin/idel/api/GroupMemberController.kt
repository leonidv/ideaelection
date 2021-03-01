package idel.api

import arrow.core.Either
import arrow.core.extensions.either.monad.flatten
import arrow.core.extensions.fx
import idel.domain.*
import idel.infrastructure.security.IdelOAuth2User
import mu.KLogger
import mu.KotlinLogging
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/groupmembers")
class GroupMemberController(
        val groupMemberRepository: GroupMemberRepository,
        val apiSecurityFactory: ApiSecurityFactory
) {
    private val log = KotlinLogging.logger {}

    private val security = apiSecurityFactory.create(log)


    private val userSecurity = UserSecurity(log)

    data class KickRequest(val groupId : String, val userId : String)

    @DeleteMapping
    fun remove(@AuthenticationPrincipal user: IdelOAuth2User, @RequestBody request: KickRequest) : EntityOrError<String> {
        return security.groupMember.asAdminOrHimSelf(request.groupId, request.userId, user) {_, _ ->
            val result = groupMemberRepository.removeFromGroup(request.groupId, request.userId)
            result.map {"ok"}
        }
    }

    @GetMapping(params = ["userId"])
    fun loadByUser(
            @AuthenticationPrincipal user : IdelOAuth2User,
            @RequestParam(required = true) userId : String,
            pagination: Repository.Pagination
    ) : EntityOrError<Repository.Pagination> {
        return userSecurity.asHimSelf(userId, user) {
            Either.right(pagination)
        }

    }

}


typealias GroupMemberAction<T> = (groupMember: GroupMember, group: Group) -> Either<Exception, T>

class GroupMemberSecurity(private val securityService: SecurityService,
                          private val groupRepository: GroupRepository,
                          private val groupMemberRepository: GroupMemberRepository,
                          private val controllerLog: KLogger) {
    private val log = KotlinLogging.logger {}

    private fun <T> secure(
            memberGroupId: String,
            memberUserId: String,
            user: IdelOAuth2User,
            requiredLevels: Set<GroupMemberAccessLevel>,
            action: GroupMemberAction<T>
    ): EntityOrError<T> {
        val result: Either<Exception, Either<Exception, T>> = Either.fx {
            val (group) = groupRepository.load(memberGroupId)
            val (userLevels)  = securityService.groupAccessLevel(group, user)
            log.trace {"$user has $userLevels for $group"}
            if (!userLevels.contains(GroupAccessLevel.MEMBER)) {
                Either.left(OperationNotPermitted())
            } else {
                val (groupMember) = groupMemberRepository.load(memberGroupId, memberUserId)

                val (levels) = securityService.groupMemberAccessLevels(groupMember, group, user)
                if (levels.intersect(requiredLevels).isNotEmpty()) {
                    action(groupMember, group)
                } else {
                    Either.left(OperationNotPermitted())
                }
            }
        }

        return DataOrError.fromEither(result.flatten(), controllerLog)
    }

    /**
     * Call an action if a [user] is an admin of the group or a groupmember belongs to him.
     */
    fun <T> asAdminOrHimSelf(memberGroupId: String,
                             memberUserId: String,
                             user: IdelOAuth2User,
                             action: GroupMemberAction<T>): EntityOrError<T> {
        val requiredLevels = setOf(GroupMemberAccessLevel.HIM_SELF, GroupMemberAccessLevel.GROUP_ADMIN)
        return secure(memberGroupId, memberUserId, user, requiredLevels, action)
    }

    /**
     * Call an action if a [user] is a member of the group.
     */
//    fun <T> asMember(memberGroupId: String,
//                     memberUserId: String,
//                     user: IdelOAuth2User,
//                     action: GroupMemberAction<T>): EntityOrError<T> {
//        val requiredLevels = setOf(GroupMemberAccessLevel.GROUP_MEMBER)
//        return secure(memberGroupId, memberUserId, user, requiredLevels, action)
//    }

}