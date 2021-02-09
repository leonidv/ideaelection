package idel.tests.apiobject

import idel.tests.Idel
import idel.tests.infrastructure.asUserId

class User(val name: String, var role : String = "",  val idelUrl: String = Idel.URL) {
    companion object {
        val instanceAdmin = User("userAdmin")
    }

    val id = name.asUserId()
    val groups = GroupsApi(name, idelUrl)
    val joinRequests = JoinRequestsApi(name, idelUrl)
    val ideas = IdeasApi(name, idelUrl)
    val users = UserApi(name, idelUrl)
    override fun toString(): String {
        val fmtRole = if (role.isBlank()) {
            ""
        } else {
            " ($role)"
        }
        return "[$name]$fmtRole"
    }


}