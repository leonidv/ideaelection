package idel.tests.infrastructure

fun String.asUserId(provider : String = "httpbasic") : String {
    return "$this@$provider"
}