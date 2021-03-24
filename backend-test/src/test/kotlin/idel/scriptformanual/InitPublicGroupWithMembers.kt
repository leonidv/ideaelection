package idel.scriptformanual

import idel.tests.apiobject.Couchbase
import idel.tests.apiobject.GroupsApi
import idel.tests.apiobject.User
import idel.tests.infrastructure.extractId
import io.kotest.core.spec.style.DescribeSpec

fun main() {

    Couchbase().clearAll()

    val userAdmin = User("userAdmin")
    val users = ('A'..'E').map {User("user$it")}

    userAdmin.users.register(userAdmin.name)
    users.forEach {user -> userAdmin.users.register(user.name)}

    lateinit var groupId : String

    var response = userAdmin.groups.create(name = "architecture reports", entryMode = GroupsApi.PUBLIC)
    groupId = response.extractId()

    users.subList(0,3).forEach {it.joinRequests.create(groupId)}
}



