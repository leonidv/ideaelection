package idel.domain

import arrow.core.Either
import arrow.core.Option

typealias UserId = String


interface User {
    fun id() : UserId
    val email : String
    val displayName : String
    val avatar : String
    val roles : Set<String>
}


interface UserRepository {
    /**
     * Load user by id.
     */
    fun load(id : String) : Option<User>

    /**
     * Persists new user.
     */
    fun add(user : User)

    /**
     * Update user and return new updated value.
     *
     * @return [Left] if some exception was occurred
     *         [Right] with updated user (usually same as input [user])
     */
    fun update(user : User) : Either<Exception,User>

    /**
     * Load list of users.
     */
    fun load(first : Int, last : Int) : List<User>

}