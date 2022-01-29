package idel.api

import arrow.core.Either
import arrow.core.computations.either
import arrow.core.flatten
import idel.domain.*
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


    private fun <T> secure(
            groupId: String,
            user: User,
            requiredLevels: Set<GroupAccessLevel>,
            action: Action<T>
    ): EntityOrError<T> {


        val result: Either<Exception, Either<Exception, T>> = either.eager {
            val accessLevels = securityService.groupAccessLevel(groupId, user).bind()
            if (accessLevels.intersect(requiredLevels).isNotEmpty()) {
                action()
            } else {
                Either.Left(OperationNotPermitted())
            }
        }

        return DataOrError.fromEither(result.flatten(), controllerLog)
    }

    fun <T> asMember(groupId: String, user: User, action: Action<T>): EntityOrError<T> {
        return secure(groupId, user, memberLevel, action)
    }

    fun <T> asAdmin(groupId: String, user: User, action: Action<T>): EntityOrError<T> {
        return secure(groupId, user, adminLevel, action)
    }

    sealed class GroupIdentity(val errorMessage : String) {
        companion object {
            fun id(value : String) = IdGroupIdentity(value)
            fun joiningKey(value : String) = JoiningKeyGroupIdentity(value)
        }
    }
    class IdGroupIdentity(val id: String) : GroupIdentity(errorMessage = id)
    class JoiningKeyGroupIdentity(val key : String) : GroupIdentity(errorMessage = "joiningKey = $key")

    fun <T> asDomainMemberOrCreator(groupIdentity: GroupIdentity, user: User, action: Action<T>) : EntityOrError<T> {
       val result : Either<Exception, Either<Exception, T>> = either.eager {
            val group = when(groupIdentity) {
                is IdGroupIdentity -> groupRepository.load(groupIdentity.id)
                is JoiningKeyGroupIdentity -> groupRepository.loadByJoiningKey(groupIdentity.key)
            }.bind()

            if (group.userDomainAllowed(user.domain) || (group.creator.id == user.id)) {
                action()
            } else {
                Either.Left(EntityNotFound("group", groupIdentity.errorMessage))
            }
        }

        return DataOrError.fromEither(result.flatten(), controllerLog)
    }
    /**
     * Check that user is member of group.
     */
    fun isMember(groupId: String, userId: String): Either<Exception, Boolean> {
        return either.eager {
            val user = userRepository.load(userId).bind()
            val levels = securityService.groupAccessLevel(groupId, user).bind()
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
            user: User,
            requiredLevels: Set<IdeaAccessLevel>,
            action: IdeaAction<T>
    ): EntityOrError<T> {
        val result: Either<Exception, Either<Exception, T>> = either.eager {
            val idea = ideaRepository.load(ideaId).bind()
            //val (group) = groupRepository.load(idea.groupId)
            val levels = securityService.ideaAccessLevels(idea, user).bind()
            if (levels.intersect(requiredLevels).isNotEmpty()) {
                action(idea)
            } else {
                Either.Left(OperationNotPermitted())
            }
        }

        return DataOrError.fromEither(result.flatten(), controllerLog)
    }

    fun <T> withLevels(ideaId: String, user: User, action: IdeaActionWithLevels<T>): EntityOrError<T> {
        val result: Either<Exception, Either<Exception, T>> = either.eager {
            val idea = ideaRepository.load(ideaId).bind()
            val levels = securityService.ideaAccessLevels(idea, user).bind()
            action(idea, levels)
        }

        return DataOrError.fromEither(result.flatten(), controllerLog)
    }

    fun calculateLevels(ideaId: String, user: User) : Either<Exception, Set<IdeaAccessLevel>> {
        return either.eager {
            val idea = ideaRepository.load(ideaId).bind()
            securityService.ideaAccessLevels(idea, user).bind()
        }
    }

    fun <T> asMember(ideaId: String, user: User, action: IdeaAction<T>): EntityOrError<T> {
        return secure(ideaId, user, setOf(IdeaAccessLevel.GROUP_MEMBER), action)
    }

    fun <T> asEditor(ideaId: String, user: User, action: IdeaAction<T>): EntityOrError<T> {
        return secure(ideaId, user, setOf(IdeaAccessLevel.ASSIGNEE, IdeaAccessLevel.AUTHOR), action)
    }

}

