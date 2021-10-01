package idel.tests.infrastructure

fun Array<String>.toJsonArray() : String {
    return this.joinToString(prefix = "[", postfix = "]") {""" "$it" """}
}

fun Collection<String>.toJsonArray() : String {
    return this.joinToString(prefix = "[", postfix = "]") {""" "$it" """}
}