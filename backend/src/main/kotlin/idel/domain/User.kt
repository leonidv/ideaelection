package idel.domain

import arrow.core.Either
import arrow.core.Option

typealias UserId = String

interface IUserInfo : Identifiable {
    val email : String
    val displayName : String
    val avatar : String
}

interface User : IUserInfo {
    val roles : Set<String>
}

/**
 * View of user info. Used for persists short information about user.
 */
data class UserInfo(
        override val id: String,
        override val email: String,
        override val displayName: String,
        override val avatar: String
) : IUserInfo {
    companion object {
        fun ofUser(user : User) : UserInfo {
            return UserInfo(
                    id = user.id,
                    avatar = user.avatar,
                    email = user.email,
                    displayName = user.displayName
            )
        }
    }
}

interface UserRepository {
    /**
     * Load user by id.
     */
    fun load(id : String) : Either<Exception,User>

    /**
     * Check that user is exists without loading from database.
     */
    fun exists(id : String) : Either<Exception,Boolean>

    /**
     * Load users by their id. Ignore id if user is not found.
     */
    fun loadUserInfo(ids : List<UserId>) : Either<Exception,List<UserInfo>>

    /**
     * Persists new user.
     */
    fun add(user : User)

    /**
     * Update user and return new updated value.
     *
     * @return [Either.Left] if some exception was occurred
     *         [Right] with updated user (usually same as input [user])
     */
    fun update(user : User) : Either<Exception,User>

    /**
     * Load list of users.
     */
    fun load(first : Int, last : Int) : List<User>

    /**
     * Load user's from group.
     *
     * In fact, the best place for this method is [GroupMemberRepository], but it's required too hard refactoring.
     */
    fun loadByGroup(groupId : String, pagination: Repository.Pagination, usernameFilter : Option<String>) : Either<Exception, List<User>>
}