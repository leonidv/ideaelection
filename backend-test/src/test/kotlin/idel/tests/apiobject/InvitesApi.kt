package idel.tests.apiobject

import com.fasterxml.jackson.databind.JsonNode
import idel.tests.Idel
import java.net.http.HttpResponse

class InvitesApi(user: User, idelUrl: String = Idel.URL) : AbstractObjectApi(user, idelUrl, "invites")  {
//    fun create(
//        groupId : String,
//        message : String,
//        registeredUsersIds : List<String>,
//        newUsersEmails : List<String>
//    ) : HttpResponse<JsonNode> {
//        val body = """
//            {
//            }
//
//        """.trimIndent()
//    }
}