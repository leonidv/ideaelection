package idel.tests.apiobject

import com.fasterxml.jackson.databind.JsonNode
import idel.tests.TestConfig
import java.net.http.HttpResponse

class GroupMembersApi(user: User, idelUrl: String = TestConfig.backendUrl) : AbstractObjectApi(user, idelUrl, "groupmembers") {

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