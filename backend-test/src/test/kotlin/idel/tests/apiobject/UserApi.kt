package idel.tests.apiobject

import com.fasterxml.jackson.databind.JsonNode
import idel.tests.Idel
import mu.KLogger
import mu.KotlinLogging
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDateTime


class UserApi (user : User, idelUrl: String = Idel.URL) : AbstractObjectApi(user, idelUrl, "users") {

    companion object {
        const val PLAN_FREE = "FREE"
        const val PLAN_BASIC = "BASIC"
        const val PLAN_ENTERPRISE = "ENTERPRISE"
    }

    private val log = KotlinLogging.logger {}



    fun register(user: User, email : String = user.email, plan : String = PLAN_FREE) : HttpResponse<JsonNode> {
        val body = """
            {
                "id": "${user.id}",
                "email": "$email",
                "displayName": "${user.id} Registered from a test ${LocalDateTime.now()}",
                "avatar": "",
                "roles": [
                    "ROLE_USER"
                ],
                "subscriptionPlan": "$plan"
            }
        """.trimIndent()

        return post("",body)
    }
}

const val DEFAULT_DOMAIN = "mail.fake"