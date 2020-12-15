package idel.tests.apiobject

import com.fasterxml.jackson.databind.JsonNode
import idel.tests.Idel
import idel.tests.infrastructure.BodyFieldCheck
import idel.tests.infrastructure.IdelHttpAuthenticator
import idel.tests.infrastructure.ofJson
import mu.KotlinLogging
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class JoinRequestsApi(val username : String, val idelUrl : String = Idel.URL) {

    private val log = KotlinLogging.logger {}

    companion object {
        val APPROVED = "APPROVED"
        val REJECTED = "REJECTED"
        val UNRESOLVED = "UNRESOLVED"
    }

    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(1))
        .authenticator(IdelHttpAuthenticator(username))
        .build()



    val resourceUri = URI.create("$idelUrl/joinrequests")

    fun create(groupId : String) : HttpResponse<JsonNode> {
        val body = """
            {
                "groupId" : "$groupId"
            }
        """.trimIndent()

        log.trace {"JoinRequestsApi.add body = $body"}

        val request = HttpRequest
            .newBuilder(resourceUri)
            .timeout(Duration.ofSeconds(1))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        return client.send(request, ofJson())!!
    }
}

fun checkJoinRequestIsApproved() = BodyFieldCheck("join request is approved", "$.data.status", JoinRequestsApi.APPROVED)