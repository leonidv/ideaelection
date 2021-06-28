package idel.domain

fun main() {
    val id = GroupMember.calculateId(groupId = "93f1ff08cda54f76875f5a886963ed5b",userId = "userB@httpbasic")
    println(id)
}