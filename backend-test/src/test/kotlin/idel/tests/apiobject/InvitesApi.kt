package idel.tests.apiobject

import arrow.core.getOrElse
import com.fasterxml.jackson.databind.JsonNode
import idel.tests.Idel
import idel.tests.infrastructure.*
import idel.tests.infrastructure.JsonNodeExtensions.queryString
import java.net.http.HttpResponse
import java.time.LocalDateTime
import java.util.*

class InvitesStatuses {
    val APPROVED: String = "APPROVED"
    val DECLINED: String = "DECLINED"
}

class EmailStatus  {
    val WAIT_TO_SENT: String = "WAIT_TO_SENT"
    val SENT: String = "SENT"
}

class InvitesApi(user: User, idelUrl: String = Idel.URL) : AbstractObjectApi(user, idelUrl, "invites") {

    companion object {
        val Status = InvitesStatuses()
        val EmailStatus = EmailStatus()
    }

    fun create(
        groupId: String,
        registeredUsers: Array<User>,
        newUsersEmails: Array<String>,
        message: String = "Created from tests ${LocalDateTime.now()}",
    ): HttpResponse<JsonNode> {
        val body = """
            {
                "groupId" : "$groupId",
                "message" : "$message",
                "registeredUsersIds" : ${registeredUsers.map {it.id}.toJsonArray()},
                "newUsersEmails" : ${newUsersEmails.toJsonArray()}
            }
        """.trimIndent()

        return post("", body)
    }

    fun load(inviteId: String): HttpResponse<JsonNode> {
        return get("/$inviteId")
    }

    fun loadForUser(userId: String = user.id): HttpResponse<JsonNode> {
        return get("?userId=$userId")
    }

    fun loadForGroup(groupId: String): HttpResponse<JsonNode> {
        return get("?groupId=$groupId")
    }

    private fun changeStatus(inviteId: String, status: String): HttpResponse<JsonNode> {
        val body = """
           {
            "status": "$status"
           } 
        """.trimIndent()

        return patch("/$inviteId/status", body)
    }

    fun approve(inviteId: String): HttpResponse<JsonNode> {
        return changeStatus(inviteId, Status.APPROVED)
    }

    fun decline(inviteId: String): HttpResponse<JsonNode> {
        return changeStatus(inviteId, Status.DECLINED)
    }

    fun revoke(inviteId: String): HttpResponse<JsonNode> {
        return delete("/$inviteId", "")
    }
}

fun extractInviteId(user: User, groupId: String, response: HttpResponse<JsonNode>): String {
    val query = "$.data.invites[?(@.userId=='${user.id}' && @.groupId=='$groupId')].id"
    return response.body()!!.queryString(query).getOrElse {ValueNotExists.throwForQuery(query)}
}


fun hasInvitesCount(count: Int) = BodyArraySize("invites count", "$.data.invites", count)

fun hasInvitesToGroups(groupsIds: Array<String>) = BodyArrayContainsObjects(
    testName = "include invites to groups [$groupsIds]",
    arrayPath = "$.data.invites",
    field = "groupId",
    values = groupsIds.toSet()
)

fun inviteUserSelectorFields(groupId: String, user: User) : Array<Pair<String, String?>> =
    arrayOf(
        Pair("groupId", groupId),
        Pair("userId", user.id),
        Pair("userEmail", user.email.lowercase(Locale.getDefault()))
    )

fun invitePersonSelectorFields(groupId: String, email: String) =
    arrayOf(
        Pair("groupId", groupId),
        Pair("userId", null),
        Pair("userEmail", email.lowercase(Locale.getDefault()))
    )

fun includeInvite(inviteId: String) = BodyContainsObject(
    testName = "include invite id = $inviteId",
    objectPath = "$.data.invites",
    fields = arrayOf(Pair("id", inviteId))
)

fun hasInviteForUser(groupId: String, user: User) = BodyContainsObject(
    testName = "include invite for user $user",
    objectPath = "$.data.invites",
    fields = inviteUserSelectorFields(groupId, user)
)

fun hasNotInviteForUser(groupId: String, user: User) = NotBodyContainsObject(
    testName = "doesn't`t include invite for user",
    objectPath = "$.data.invites",
    fields = inviteUserSelectorFields(groupId, user)
)

fun hasInviteForPerson(groupId: String, email: String) = BodyContainsObject(
    testName = "include invite for person, email = $email, groupId = $groupId",
    objectPath = "$.data.invites",
    fields = invitePersonSelectorFields(groupId, email)
)

fun hasNotInviteForPerson(groupId: String, email: String) = NotBodyContainsObject(
    testName = "doesn't include invite for person, email = $email",
    objectPath = "$.data.invites",
    fields = invitePersonSelectorFields(groupId, email)
)