package idel.tests.infrastructure

fun String.asUserExternalId(provider : String = "httpbasic") : String {
    return "$this@$provider"
}