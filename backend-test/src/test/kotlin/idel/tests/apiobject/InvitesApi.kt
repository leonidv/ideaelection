package idel.tests.apiobject

import com.fasterxml.jackson.databind.JsonNode
import idel.tests.Idel
import idel.tests.infrastructure.BodyArrayContainsObjects
import idel.tests.infrastructure.BodyContainsObject
import idel.tests.infrastructure.NotBodyContainsObject
import idel.tests.infrastructure.toJsonArray
import java.net.http.HttpResponse
import java.time.LocalDateTime

class InvitesStatuses {
    val APPROVED: String = "APPROVED"
    val DECLINED: String = "DECLINED"
}

class InvitesField

class InvitesApi(user: User, idelUrl: String = Idel.URL) : AbstractObjectApi(user, idelUrl, "invites") {

    companion object {
        val Status = InvitesStatuses()
    }

    fun create(
        groupId: String,
        registeredUsers: Array<User>,
        newUsersEmails: Array<User>,
        message: String = "Created from tests ${LocalDateTime.now()}",
    ): HttpResponse<JsonNode> {
        val body = """
            {
                "groupId" : "$groupId",
                "message" : "$message",
                "registeredUsersIds" : ${registeredUsers.map {it.id}.toJsonArray()},
                "newUsersEmails" : ${newUsersEmails.map {it.id}.toJsonArray()}
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

fun hasInvitesToGroups(groupsIds: Array<String>) = BodyArrayContainsObjects(
    testName = "include invites to groups [$groupsIds]",
    arrayPath = "$.data.invites",
    field = "groupId",
    values = groupsIds.toSet()
)

fun inviteUserSelectorFields(groupId: String, userId: String) =
    arrayOf(
        Pair("groupId", groupId),
        Pair("userId", userId),
        Pair("userEmail", null),
        Pair("emailWasSent", null)
    )

fun hasInviteForUser(groupId: String, user: User) = BodyContainsObject(
    testName = "include invite for user",
    objectPath = "$.data.invites",
    fields = inviteUserSelectorFields(groupId, user.id)
)

fun hasNotInviteForUser(groupId: String, user: User) = NotBodyContainsObject(
    testName = "don't include invite for user",
    objectPath = "$.data.invites",
    fields = inviteUserSelectorFields(groupId, user.id)
)