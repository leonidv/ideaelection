package idel.api

import arrow.core.Either
import arrow.core.extensions.either.monad.flatten
import arrow.core.extensions.fx
import arrow.core.flatMap
import idel.domain.*
import idel.infrastructure.security.IdelOAuth2User
import mu.KLogger

class ApiSecurity(
        private val securityService: SecurityService,
        private val groupRepository: GroupRepository,
        private val userRepository: UserRepository,
        private val controllerLog : KLogger) {
    val group = GroupSecurity(securityService, groupRepository, controllerLog)

}

typealias ActionOnGroup<T> = (group : Group) -> Either<Exception,T>

class GroupSecurity(private val securityService: SecurityService, private val groupRepository: GroupRepository, private val controllerLog : KLogger) {
    private val memberLevel = setOf(GroupAccessLevel.MEMBER, GroupAccessLevel.ADMIN)

    private val adminLevel = setOf(GroupAccessLevel.ADMIN)

    private fun <T> secure(
            groupId: String,
            user: IdelOAuth2User,
            requiredLevels: Set<GroupAccessLevel>,
            action : ActionOnGroup<T>
    ) : EntityOrError<T> {
        val result : Either<Exception, Either<Exception,T>> = Either.fx {
            val (group) = groupRepository.load(groupId)
            val (accessLevels) = securityService.groupAccessLevel(group, user)
            if (accessLevels.intersect(requiredLevels).isNotEmpty()) {
                action(group)
            } else {
                Either.left(OperationNotPermitted())
            }
        }

        return DataOrError.fromEither(result.flatten(), controllerLog)
    }

    fun <T> asMember(groupId: String, user: IdelOAuth2User, action: ActionOnGroup<T>) : EntityOrError<T> {
        return secure(groupId,user, memberLevel, action)
    }

    private fun <T> asAdmin(groupId: String, user: IdelOAuth2User, action: ActionOnGroup<T>): EntityOrError<T> {
        return secure(groupId, user, adminLevel, action)
    }

//    fun <T> asHimselfOrAdmin(groupId: String, user: IdelOAuth2User, subjectUserId: String, action: () -> Either<Exception,T>) : EntityOrError<T> {
//        return if(user.id == subjectUserId) {
//            DataOrError.fromEither(action(), controllerLog)
//        } else {
//            asAdmin(groupId, user, action)
//        }
//    }
}

