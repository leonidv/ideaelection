package idel.tests.apiobject

import idel.tests.Idel
import idel.tests.infrastructure.asUserId

class User(val name: String, var role : String = "", val domain : String = DEFAULT_DOMAIN, idelUrl: String = Idel.URL) {
    companion object {
        val instanceAdmin = User("userAdmin")
    }

    val id = name.asUserId()

    val email = "$name@$domain"

    val groups = GroupsApi(this, idelUrl)
    val joinRequests = JoinRequestsApi(this, idelUrl)
    val invites = InvitesApi(this, idelUrl)
    val ideas = IdeasApi(this, idelUrl)
    val users = UserApi(this, idelUrl)
    val jwt = JwtApi(this, idelUrl)

    override fun toString(): String {
        val fmtRole = if (role.isBlank()) {
            ""
        } else {
            " ($role)"
        }

        val fmtDomain = if (domain == DEFAULT_DOMAIN) {
            ""
        } else {
            " ($domain)"
        }
        return "[$name]$fmtRole$fmtDomain".replace(") (",", ")
    }


}