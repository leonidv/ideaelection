package idel.tests.apiobject

import com.fasterxml.jackson.databind.JsonNode
import idel.tests.Idel
import idel.tests.infrastructure.BodyContainsObject
import idel.tests.infrastructure.BodyFieldValueChecker
import idel.tests.infrastructure.asUserId
import mu.KotlinLogging
import java.net.http.HttpResponse

class JoinRequestsApi(user : User, idelUrl : String = Idel.URL) : AbstractObjectApi(user, idelUrl, "joinrequests") {

    private val log = KotlinLogging.logger {}

    companion object {
        const val APPROVED = "APPROVED"
        const val DECLINED = "DECLINED"
        const val UNRESOLVED = "UNRESOLVED"
    }


    fun create(joiningKey : String, message : String = "I want to join with $joiningKey") : HttpResponse<JsonNode> {
        val body = """
            {
                "joiningKey" : "$joiningKey",
                "message" : "$message"
            }
        """.trimIndent()


        return post("",body)
    }

    fun loadForUser(userId: String = user.id) = get("?userId=$userId")

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
    BodyContainsObject("include joinRequest $joinRequestId", "$.data", arrayOf(Pair("id",joinRequestId)))

fun includeJoinRequestWithStatus(joinRequestId: String, status: String) =
    BodyContainsObject("include joinRequest $joinRequestId with status $status", "$.data",
        arrayOf(Pair("id",joinRequestId),Pair("status",status))
    )