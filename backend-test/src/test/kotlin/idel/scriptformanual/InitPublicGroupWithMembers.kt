package idel.scriptformanual

import idel.tests.apiobject.EntityStorage
import idel.tests.apiobject.GroupsApi
import idel.tests.apiobject.User
import idel.tests.infrastructure.extractField

fun main() {

    EntityStorage().clearAll()

    val userAdmin = User("userAdmin")
    val users = ('A'..'E').map {User("user$it")}

    userAdmin.users.register(userAdmin)
    users.forEach {user -> userAdmin.users.register(user)}

    lateinit var joiningKey : String


    val response = userAdmin.groups.create(name = "architecture reports", entryMode = GroupsApi.PUBLIC)
    joiningKey = response.extractField(GroupsApi.Fields.JOINING_KEY)

    users.subList(0,3).forEach {it.joinRequests.create(joiningKey)}
}



