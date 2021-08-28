package idel.api

import arrow.core.Either
import arrow.core.computations.either
import arrow.core.flatten
import idel.domain.*
import mu.KLogger
import mu.KotlinLogging


typealias GroupMemberAction<T> = (groupMember: GroupMember) -> Either<Exception, T>

class GroupMemberSecurity(private val securityService: SecurityService,
                          private val groupRepository: GroupRepository,
                          private val groupMemberRepository: GroupMemberRepository,
                          private val controllerLog: KLogger) {
    private val log = KotlinLogging.logger {}

    private fun <T> secure(
        groupId: String,
        memberUserId: String,
        user: User,
        requiredLevels: Set<GroupMemberAccessLevel>,
        action: GroupMemberAction<T>
    ): EntityOrError<T> {
        val result: Either<Exception, Either<Exception, T>> = either.eager {
            //val (group) = groupRepository.load(memberGroupId)
            val userLevels = securityService.groupAccessLevel(groupId, user).bind()
            log.trace {"$user has $userLevels for $groupId"}
            if (!userLevels.contains(GroupAccessLevel.MEMBER)) {
                Either.Left(OperationNotPermitted())
            } else {
                val groupMember = groupMemberRepository.load(groupId, memberUserId).bind()

                val levels = securityService.groupMemberAccessLevels(groupMember, groupId, user).bind()
                if (levels.intersect(requiredLevels).isNotEmpty()) {
                    action(groupMember)
                } else {
                    Either.Left(OperationNotPermitted())
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
                             user: User,
                             action: GroupMemberAction<T>): EntityOrError<T> {
        val requiredLevels = setOf(GroupMemberAccessLevel.HIM_SELF, GroupMemberAccessLevel.GROUP_ADMIN)
        return secure(memberGroupId, memberUserId, user, requiredLevels, action)
    }
}