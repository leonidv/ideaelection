package idel.api

import arrow.core.Either
import arrow.core.extensions.either.monad.flatten
import arrow.core.extensions.fx
import idel.domain.*
import idel.infrastructure.security.IdelOAuth2User
import mu.KLogger
import org.springframework.stereotype.Service

typealias GroupAction<T> = (group: Group) -> Either<Exception, T>

class ApiSecurity(
        securityService: SecurityService,
        groupRepository: GroupRepository,
        ideaRepository: IdeaRepository,
        groupMemberRepository: GroupMemberRepository,
        controllerLog: KLogger
) {
    val user = UserSecurity(controllerLog)
    val group = GroupSecurity(securityService, groupRepository, controllerLog)
    val idea = IdeaSecurity(securityService, groupRepository, ideaRepository, controllerLog)
    val groupMember = GroupMemberSecurity(securityService, groupRepository, groupMemberRepository, controllerLog)
}

@Service
class ApiSecurityFactory(private val securityService: SecurityService,
                         private val groupRepository: GroupRepository,
                         private val ideaRepository: IdeaRepository,
                         private val groupMemberRepository: GroupMemberRepository) {
    fun create(controllerLog: KLogger) = ApiSecurity(
            securityService,
            groupRepository,
            ideaRepository,
            groupMemberRepository,
            controllerLog
    )
}

class GroupSecurity(
        private val securityService: SecurityService,
        private val groupRepository: GroupRepository,
        private val controllerLog: KLogger
) {
    private val memberLevel = setOf(GroupAccessLevel.MEMBER, GroupAccessLevel.ADMIN)

    private val adminLevel = setOf(GroupAccessLevel.ADMIN)


    fun <T> secure(
            groupId: String,
            user: IdelOAuth2User,
            requiredLevels: Set<GroupAccessLevel>,
            action: GroupAction<T>
    ): EntityOrError<T> {
        val result: Either<Exception, Either<Exception, T>> = Either.fx {
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

    fun <T> asMember(groupId: String, user: IdelOAuth2User, action: GroupAction<T>): EntityOrError<T> {
        return secure(groupId, user, memberLevel, action)
    }

    fun <T> asAdmin(groupId: String, user: IdelOAuth2User, action: GroupAction<T>): EntityOrError<T> {
        return secure(groupId, user, adminLevel, action)
    }

}

typealias IdeaAction<T> = (group: Group, idea: Idea) -> Either<Exception, T>

class IdeaSecurity(private val securityService: SecurityService,
                   private val groupRepository: GroupRepository,
                   private val ideaRepository: IdeaRepository,
                   private val controllerLog: KLogger) {


    fun <T> secure(
            groupId: String,
            ideaId: String,
            user: IdelOAuth2User,
            requiredLevels: Set<IdeaAccessLevel>,
            action: IdeaAction<T>
    ): EntityOrError<T> {
        val result: Either<Exception, Either<Exception, T>> = Either.fx {
            val (group) = groupRepository.load(groupId)
            val (idea) = ideaRepository.load(ideaId)
            val (levels) = securityService.ideaAccessLevels(group, idea, user)
            if (levels.intersect(requiredLevels).isNotEmpty()) {
                action(group, idea)
            } else {
                Either.left(OperationNotPermitted())
            }
        }

        return DataOrError.fromEither(result.flatten(), controllerLog)
    }
}
