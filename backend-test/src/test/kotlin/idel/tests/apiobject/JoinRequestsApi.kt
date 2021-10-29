package idel.tests.apiobject

import arrow.core.getOrElse
import com.fasterxml.jackson.databind.JsonNode
import idel.tests.Idel
import idel.tests.infrastructure.*
import idel.tests.infrastructure.JsonNodeExtensions.queryString
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

    fun delete(joinRequestId: String) = super.delete("/$joinRequestId","")
}

/*
 * fun extractInviteId(user: User, groupId: String, response : HttpResponse<JsonNode>) : String {
val query = "$.data.invites[?(@.userId=='${user.id}' && @.groupId=='$groupId')].id"
return response.body()!!.queryString(query).getOrElse {ValueNotExists.throwForQuery(query)}
 */

fun extractJoinRequestId(response: HttpResponse<JsonNode>) : String {
    val path = "$.data.joinRequest.id"
    return response.body()!!.queryString(path).getOrElse {ValueNotExists.`throw`(path)}
}

fun joinRequestHasStatus(status : String) = BodyFieldValueChecker.forField("joinRequest.status",status)
val joinRequestIsApproved = joinRequestHasStatus(JoinRequestsApi.APPROVED)
val joinRequestIsUnresolved = joinRequestHasStatus(JoinRequestsApi.UNRESOLVED)
val joinRequestIsDeclined = joinRequestHasStatus(JoinRequestsApi.DECLINED)
fun joinRequestHasGroupId(groupId : String) = BodyFieldValueChecker.forField("joinRequest.groupId",groupId)
fun joinRequestHasUserId(userId : String) = BodyFieldValueChecker.forField("joinRequest.userId",userId)
fun joinRequestHasMessage(msg : String) = BodyFieldValueChecker.forField("joinRequest.message", msg)

fun hasJoinRequestsCount(count : Int) = BodyArraySize("joinRequests count", "$.data.joinRequests", count)

fun includeJoinRequest(joinRequestId : String) =
    BodyContainsObject("include joinRequest $joinRequestId", "$.data.joinRequests", arrayOf(Pair("id",joinRequestId)))

fun notIncludeJoinRequest(joinRequestId: String) =
    NotBodyContainsObject("not include joinRequest $joinRequestId", "$.data.joinRequests", arrayOf(Pair("id", joinRequestId)))

fun includeJoinRequestWithStatus(joinRequestId: String, status: String) =
    BodyContainsObject("include joinRequest $joinRequestId with status $status", "$.data.joinRequests",
        arrayOf(Pair("id",joinRequestId),Pair("status",status))
    )