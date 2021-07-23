package idel.tests.apiobject

import com.fasterxml.jackson.databind.JsonNode
import idel.tests.Idel
import idel.tests.infrastructure.asUserId
import java.net.http.HttpResponse

class GroupMembersApi(user: User, idelUrl: String = Idel.URL) : AbstractObjectApi(user, idelUrl, "groupmembers") {

    fun remove(groupId : String, userId: String = user.id) : HttpResponse<JsonNode> {
        val body = """
            {
                "groupId":"$groupId",
                "userId":"$userId"
            }
        """.trimIndent()


       return delete("", body)
    }

}