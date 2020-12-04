package idel.tests.apiobject

import idel.tests.Idel
import idel.tests.infrastructure.asUserId

class User(val name : String, val idelUrl : String = Idel.URL) {
    val id = name.asUserId()
    val groups = GroupsApi(name, idelUrl)
    val joinRequests = JoinRequestsApi(name, idelUrl)
    val ideas = IdeasApi(name, idelUrl)
    val users = UserApi(name, idelUrl)
}