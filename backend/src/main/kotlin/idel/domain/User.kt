package idel.domain

typealias UserId = String


interface User {
    fun id() : String
    val email : String
    val profileName : String
    val profilePhoto : String
}