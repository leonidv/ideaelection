package idel.tests.apiobject

import com.fasterxml.jackson.databind.JsonNode
import idel.tests.Idel
import idel.tests.infrastructure.BodyArrayElementExists
import idel.tests.infrastructure.BodyArrayObjectWithFields
import idel.tests.infrastructure.BodyFieldValueChecker
import idel.tests.infrastructure.asUserId
import mu.KotlinLogging
import java.net.http.HttpResponse

class JoinRequestsApi(username : String, idelUrl : String = Idel.URL) : AbstractObjectApi(username, idelUrl, "joinrequests") {

    private val log = KotlinLogging.logger {}

    companion object {
        val APPROVED = "APPROVED"
        val DECLINED = "DECLINED"
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

    fun loadForUser(userId: String = username.asUserId()) = get("?userId=$userId")

    fun loadForGroup(groupId: String) = get("?groupId=$groupId")

    fun changeStatus(joinRequestId: String, status : String) : HttpResponse<JsonNode> {
        val body = """
            {
                "status" : "$status"
            }
        """.trimIndent()

        return patch("/$joinRequestId/status", body)
    }
}

fun joinRequestHasStatus(status : String) = BodyFieldValueChecker.forField("status",status)
val joinRequestIsApproved = BodyFieldValueChecker("join request is approved", "$.data.status", JoinRequestsApi.APPROVED)
val joinRequestIsUnresolved = BodyFieldValueChecker("join request is unresolved", "$.data.status", JoinRequestsApi.UNRESOLVED)
val joinRequestIsDeclined = BodyFieldValueChecker("join request is declined","$.data.status",JoinRequestsApi.DECLINED)
fun joinRequestHasGroupId(groupId : String) = BodyFieldValueChecker.forField("groupId",groupId)
fun joinRequestHasUserId(userId : String) = BodyFieldValueChecker.forField("userId",userId)
fun joinRequestHasMessage(msg : String) = BodyFieldValueChecker.forField("message", msg)


fun includeJoinRequest(joinRequestId : String) =
    BodyArrayElementExists("include joinRequest $joinRequestId", "$.data","id",joinRequestId)

fun includeJoinRequestWithStatus(joinRequestId: String, status: String) =
    BodyArrayObjectWithFields("include joinRequest $joinRequestId with status $status", "$.data",
        arrayOf(Pair("id",joinRequestId),Pair("status",status))
    )