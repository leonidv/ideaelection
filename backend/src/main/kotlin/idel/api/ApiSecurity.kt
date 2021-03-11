package idel.api

import arrow.core.Either
import arrow.core.extensions.either.monad.flatten
import arrow.core.extensions.fx
import idel.domain.*
import idel.infrastructure.security.IdelOAuth2User
import mu.KLogger
import org.springframework.stereotype.Service

typealias GroupAction<T> = (group: Group) -> Either<Exception, T>

typealias Action<T> = () -> Either<Exception, T>

class ApiSecurity(
        securityService: SecurityService,
        userRepository: UserRepository,
        groupRepository: GroupRepository,
        ideaRepository: IdeaRepository,
        groupMemberRepository: GroupMemberRepository,
        controllerLog: KLogger
) {
    val user = UserSecurity(controllerLog)
    val group = GroupSecurity(userRepository, securityService, groupRepository, controllerLog)
    val idea = IdeaSecurity(securityService, groupRepository, ideaRepository, controllerLog)
    val groupMember = GroupMemberSecurity(securityService, groupRepository, groupMemberRepository, controllerLog)
}

@Service
class ApiSecurityFactory(private val securityService: SecurityService,
                         private val userRepository: UserRepository,
                         private val groupRepository: GroupRepository,
                         private val ideaRepository: IdeaRepository,
                         private val groupMemberRepository: GroupMemberRepository) {
    fun create(controllerLog: KLogger) = ApiSecurity(
            securityService,
            userRepository,
            groupRepository,
            ideaRepository,
            groupMemberRepository,
            controllerLog
    )
}

class GroupSecurity(
        private val userRepository: UserRepository,
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
            action: Action<T>
    ): EntityOrError<T> {


        val result: Either<Exception, Either<Exception, T>> = Either.fx {
            //val (group) = groupRepository.load(groupId)
            val (accessLevels) = securityService.groupAccessLevel(groupId, user)
            if (accessLevels.intersect(requiredLevels).isNotEmpty()) {
                action()
            } else {
                Either.left(OperationNotPermitted())
            }
        }

        return DataOrError.fromEither(result.flatten(), controllerLog)
    }

    fun <T> asMember(groupId: String, user: IdelOAuth2User, action: Action<T>): EntityOrError<T> {
        return secure(groupId, user, memberLevel, action)
    }

    fun <T> asAdmin(groupId: String, user: IdelOAuth2User, action: Action<T>): EntityOrError<T> {
        return secure(groupId, user, adminLevel, action)
    }

    /**
     * Check that user is member of group.
     */
    fun isMember(groupId: String, userId: String): Either<Exception, Boolean> {
        return Either.fx<Exception, Boolean> {
            val (user) = userRepository.load(userId)
            val (levels) = securityService.groupAccessLevel(groupId, user)
            levels.contains(GroupAccessLevel.MEMBER)
        }
    }

}

typealias IdeaAction<T> = (idea: Idea) -> Either<Exception, T>
typealias IdeaActionWithLevels<T> = (idea: Idea, levels: Set<IdeaAccessLevel>) -> Either<Exception, T>

class IdeaSecurity(private val securityService: SecurityService,
                   private val groupRepository: GroupRepository,
                   private val ideaRepository: IdeaRepository,
                   private val controllerLog: KLogger) {


    fun <T> secure(
            ideaId: String,
            user: IdelOAuth2User,
            requiredLevels: Set<IdeaAccessLevel>,
            action: IdeaAction<T>
    ): EntityOrError<T> {
        val result: Either<Exception, Either<Exception, T>> = Either.fx {
            val (idea) = ideaRepository.load(ideaId)
            //val (group) = groupRepository.load(idea.groupId)
            val (levels) = securityService.ideaAccessLevels(idea, user)
            if (levels.intersect(requiredLevels).isNotEmpty()) {
                action(idea)
            } else {
                Either.left(OperationNotPermitted())
            }
        }

        return DataOrError.fromEither(result.flatten(), controllerLog)
    }

    fun <T> withLevels(ideaId: String, user: IdelOAuth2User, action: IdeaActionWithLevels<T>): EntityOrError<T> {
        val result: Either<Exception, Either<Exception, T>> = Either.fx {
            val (idea) = ideaRepository.load(ideaId)
            //val (group) = groupRepository.load(idea.groupId)
            val (levels) = securityService.ideaAccessLevels(idea, user)
            action(idea, levels)
        }

        return DataOrError.fromEither(result.flatten(), controllerLog)
    }

    fun <T> asMember(ideaId: String, user: IdelOAuth2User, action: IdeaAction<T>): EntityOrError<T> {
        return secure(ideaId, user, setOf(IdeaAccessLevel.GROUP_MEMBER), action)
    }

    fun <T> asEditor(ideaId: String, user: IdelOAuth2User, action: IdeaAction<T>): EntityOrError<T> {
        return secure(ideaId, user, setOf(IdeaAccessLevel.ASSIGNEE, IdeaAccessLevel.AUTHOR), action)
    }

}

