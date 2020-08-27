package idel.tests

import assertk.Assert
import assertk.assertions.support.expected
import assertk.assertions.support.fail

fun <T> Assert<T>.isUser(login: T, provider: String = "httpbasic") = given {actual ->
    if (actual !is String) {
        expected("userId should be String", expected = "String")
    }
    val userId = "$login@$provider"
    if (actual == userId) {
        return
    } else {
        fail(userId, actual)
    }
}