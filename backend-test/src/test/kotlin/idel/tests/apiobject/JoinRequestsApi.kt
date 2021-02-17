package idel.tests.apiobject

import com.fasterxml.jackson.databind.JsonNode
import idel.tests.Idel
import idel.tests.infrastructure.BodyFieldValueChecker
import mu.KotlinLogging
import java.net.http.HttpResponse

class JoinRequestsApi(username : String, idelUrl : String = Idel.URL) : AbstractObjectApi(username, idelUrl, "joinrequests") {

    private val log = KotlinLogging.logger {}

    companion object {
        val APPROVED = "APPROVED"
        val REJECTED = "REJECTED"
        val UNRESOLVED = "UNRESOLVED"
    }


    fun create(groupId : String, message : String = "I want to joint into $groupId") : HttpResponse<JsonNode> {
        val body = """
            {
                "groupId" : "$groupId",
                "message" : "$message"
            }
        """.trimIndent()


        return post("",body)
    }
}

fun joinRequestIsApproved() = BodyFieldValueChecker("join request is approved", "$.data.status", JoinRequestsApi.APPROVED)