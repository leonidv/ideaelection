package idel.scriptformanual

import idel.tests.apiobject.EntityStorage
import idel.tests.apiobject.User

fun main() {
    EntityStorage().clearAll()

    val userAdmin = User("userAdmin")
    val users = ('A'..'E').map {User("user$it")}

    userAdmin.users.register(userAdmin.name)
    users.forEach {user -> userAdmin.users.register(user.name)}
}