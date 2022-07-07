package idel.scriptformanual

import idel.tests.apiobject.EntityStorage
import idel.tests.apiobject.GroupsApi
import idel.tests.apiobject.JoinRequestsApi
import idel.tests.apiobject.User
import idel.tests.infrastructure.extractField
import idel.tests.infrastructure.extractId

fun main() {

    EntityStorage().clearAll()

    val userAdmin = User("userAdmin")
    val userB = User("userB","member")
    val userC = User("userC","non member")

    userAdmin.users.register(userAdmin)
    userAdmin.users.register(userB)
    userAdmin.users.register(userC)

    val response = userAdmin.groups.create(name = "architecture internal process", entryMode = GroupsApi.EntryMode.PRIVATE)
    val joiningKey = response.extractField(GroupsApi.Fields.JOINING_KEY)

    val joinRequestResponse = userB.joinRequests.create(joiningKey)
    val joinRequestId = joinRequestResponse.extractId()
    userAdmin.joinRequests.changeStatus(joinRequestId, JoinRequestsApi.APPROVED)
}