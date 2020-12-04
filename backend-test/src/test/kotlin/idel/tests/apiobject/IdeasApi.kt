package idel.tests.apiobject

import com.fasterxml.jackson.databind.JsonNode
import idel.tests.Idel
import java.net.http.HttpResponse

class IdeasApi(username: String, idelUrl: String = Idel.URL) : AbstractObjectApi(username, idelUrl, "ideas")  {

    /**
     * Add new idea.
     */
    fun add(groupId : String,
            title : String = "Idea from test",
            description : String = "description of $title"): HttpResponse<JsonNode> {
        val body = """
            {
                "groupId" : "$groupId",
                "title" : "$title",
                "description": "$description",
                "link": "http://somelink.io/$title"
            }
        """.trimIndent()

        return post(body)
    }
}