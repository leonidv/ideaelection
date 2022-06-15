package idel.domain

import arrow.core.Either
import arrow.core.computations.either
import idel.api.DataOrError
import mu.KLogger
import mu.KotlinLogging
import java.util.*

typealias UserId = UUID

enum class SubscriptionPlan { FREE, BASIC, ENTERPRISE }

interface IUserInfo {
    val id: UserId
    val email: String
    val externalId: String
    val displayName: String
    val avatar: String
    val subscriptionPlan: SubscriptionPlan
}

interface User : IUserInfo {
    val roles: Set<String>
    val domain: String
        get() = this.email.split('@').last()
}

/**
 * View of user info. Used for persists short information about user.
 */
data class UserInfo(
    override val externalId: String,
    override val id: UserId,
    override val email: String,
    override val displayName: String,
    override val avatar: String,
    override val subscriptionPlan: SubscriptionPlan
) : IUserInfo {
    companion object {
        fun ofUser(user: User): UserInfo {
            return UserInfo(
                id = user.id,
                externalId = user.externalId,
                avatar = user.avatar,
                email = normalizeEmail(email = user.email),
                displayName = user.displayName,
                subscriptionPlan = user.subscriptionPlan
            )
        }

        fun normalizeEmail(email: String) : String {
            return email.lowercase()
        }
    }
}



interface UserRepository {

    fun add(user: User): Either<DomainError, User>

    fun load(id: UUID): Either<DomainError, User>

    fun loadByExternalId(externalId: String): Either<DomainError, User>

    /**
     * Update user and return new updated value.
     *
     * @return [Either.Left] if some exception was occurred
     *         [Right] with updated user (usually same as input [user])
     */
    fun update(user: User): Either<DomainError, User>

    /**
     * Load list of users.
     */
    fun list(usernameFilter: String?, pagination: Repository.Pagination): Either<DomainError, List<User>>

    /**
     * Load all user that relative to each idea in the list.
     *
     * If two or more ideas contain the same user, the user will be returned only one time.
     */
    fun listById(ids: Set<UserId>): Either<DomainError, Iterable<User>>
}

class UserService(
    private val userRepository: UserRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val groupMembershipService: GroupMembershipService
) {
    private val log = KotlinLogging.logger {}

    /**
     * Register new user in the system.
     */
    fun register(user: User): Either<DomainError, User> {
        val result = fTransaction {
            either.eager {
                userRepository.add(user).bind()
                val userSettings = UserSettingsFactory().createDefault(user)
                userSettingsRepository.add(userSettings).bind()
                user
            }
        }

        // the main goal is create user, errors in linking user's and invites is ignored
        result.tap {
            // if error try to create delayed task on relink, but think about storage!
            groupMembershipService.convertPersonInviteToUserInvite(user)
        }
        return result;
    }
}


class UserSecurity {

    /**
     * Check that user is owner of resource.
     */
    fun <T> asHimSelf(userId: UUID, user: User, action: () -> Either<DomainError, T>): Either<DomainError, T> {
        val result = if (userId == user.id) {
            fTransaction {
                action()
            }
        } else {
            Either.Left(OperationNotPermitted())
        }

        return result
    }
}