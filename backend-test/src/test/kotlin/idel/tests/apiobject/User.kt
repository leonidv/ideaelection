package idel.tests.apiobject

import idel.tests.TestConfig
import idel.tests.infrastructure.asUserExternalId
import java.util.*

class User(val name: String, var role : String = "", val domain : String = DEFAULT_DOMAIN, idelUrl: String = TestConfig.backendUrl) {
    companion object {
        val instanceAdmin = User("userAdmin")
    }

    val id = UUID.randomUUID().toString()

    val externalId = name.asUserExternalId()

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