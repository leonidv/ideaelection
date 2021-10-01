package idel.tests.apiobject

import com.fasterxml.jackson.databind.JsonNode
import idel.tests.Idel
import idel.tests.infrastructure.toJsonArray
import java.net.http.HttpResponse
import java.time.LocalDateTime

class InvitesStatuses {
    val APPROVED: String = "APPROVED"
    val DECLINED: String = "DECLINED"
}

class InvitesApi(user: User, idelUrl: String = Idel.URL) : AbstractObjectApi(user, idelUrl, "invites") {

    companion object {
        val Status = InvitesStatuses()
    }

    fun create(
        groupId: String,
        registeredUsersIds: Array<User>,
        newUsersEmails: Array<User>,
        message: String = "Created from tests ${LocalDateTime.now()}",
    ): HttpResponse<JsonNode> {
        val body = """
            {
                "groupId" : "$groupId",
                "message" : "$message",
                "registeredUsersIds" : ${registeredUsersIds.map {it.id}.toJsonArray()},
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