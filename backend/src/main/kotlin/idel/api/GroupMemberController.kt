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



typealias GroupMemberAction<T> = (groupMember: GroupMember) -> Either<Exception, T>

class GroupMemberSecurity(private val securityService: SecurityService,
                          private val groupRepository: GroupRepository,
                          private val groupMemberRepository: GroupMemberRepository,
                          private val controllerLog: KLogger) {
    private val log = KotlinLogging.logger {}

    private fun <T> secure(
        groupId: String,
        memberUserId: String,
        user: IdelOAuth2User,
        requiredLevels: Set<GroupMemberAccessLevel>,
        action: GroupMemberAction<T>
    ): EntityOrError<T> {
        val result: Either<Exception, Either<Exception, T>> = Either.fx {
            //val (group) = groupRepository.load(memberGroupId)
            val (userLevels)  = securityService.groupAccessLevel(groupId, user)
            log.trace {"$user has $userLevels for $groupId"}
            if (!userLevels.contains(GroupAccessLevel.MEMBER)) {
                Either.left(OperationNotPermitted())
            } else {
                val (groupMember) = groupMemberRepository.load(groupId, memberUserId)

                val (levels) = securityService.groupMemberAccessLevels(groupMember, groupId, user)
                if (levels.intersect(requiredLevels).isNotEmpty()) {
                    action(groupMember)
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