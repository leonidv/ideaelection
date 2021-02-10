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

class JoinRequestsApi(username : String, idelUrl : String = Idel.URL) : AbstractObjectApi(username, idelUrl, "joinrequests") {

    private val log = KotlinLogging.logger {}

    companion object {
        val APPROVED = "APPROVED"
        val REJECTED = "REJECTED"
        val UNRESOLVED = "UNRESOLVED"
    }


    fun create(groupId : String) : HttpResponse<JsonNode> {
        val body = """
            {
                "groupId" : "$groupId"
            }
        """.trimIndent()


        return post("",body)
    }
}

fun checkJoinRequestIsApproved() = BodyFieldCheck("join request is approved", "$.data.status", JoinRequestsApi.APPROVED)