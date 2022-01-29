package idel.tests.apiobject

import arrow.core.getOrElse
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import idel.tests.Idel
import idel.tests.infrastructure.BodyFieldValueChecker
import idel.tests.infrastructure.JsonNodeExtensions.queryString
import idel.tests.infrastructure.ValueNotExists
import java.net.http.HttpResponse
import java.util.*

class JwtApi(user: User, idelUrl: String = Idel.URL) : AbstractObjectApi(user, idelUrl, "token") {
    companion object {
        fun extractPayload(httpResponse: HttpResponse<JsonNode>): JsonNode {
            val body = httpResponse.body()!!
            val token = body.queryString("data").getOrElse {ValueNotExists.`throw`("data")}
            val chunks = token.splitToSequence(".").toList()
            val payload = Base64.getDecoder().decode(chunks[1])
            return jacksonObjectMapper().readValue(payload) as JsonNode
        }
    }

    fun issue() = get("")

}

fun jwtHasUserIdAsSub(userId: String) = BodyFieldValueChecker.forField("sub", userId, inData = false)
fun jwtHasDisplayName(value: String) = BodyFieldValueChecker.forField("displayName", value, inData = false)
fun jwtHasSubscriptionPlan(value: String) = BodyFieldValueChecker.forField("subscriptionPlan", value, inData = false)
fun jwtHasEmail(value: String) = BodyFieldValueChecker.forField("email", value, inData = false)
val jwtHasIssuerSaedi = BodyFieldValueChecker.forField("iss", "saedi", inData = false)
