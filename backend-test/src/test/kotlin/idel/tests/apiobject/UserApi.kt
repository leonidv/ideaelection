package idel.tests.apiobject

import com.fasterxml.jackson.databind.JsonNode
import idel.tests.Idel
import mu.KLogger
import mu.KotlinLogging
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDateTime

class UserApi (userName : String, idelUrl: String = Idel.URL) : AbstractObjectApi(userName, idelUrl, "users") {
    private val log = KotlinLogging.logger {}



    fun register(userName: String) : HttpResponse<JsonNode> {
        val body = """
            {
                "id": "$userName@httpbasic",
                "email": "$userName@mail.fake",
                "displayName": "Registered from test ${LocalDateTime.now()}",
                "avatar": "",
                "roles": [
                    "ROLE_USER"
                ]
            }
        """.trimIndent()

        log.trace {"UsersApi.register body = $body"}

        return post("",body)
    }
}