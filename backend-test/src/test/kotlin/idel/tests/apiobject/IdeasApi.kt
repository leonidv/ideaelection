package idel.tests.apiobject

import com.fasterxml.jackson.databind.JsonNode
import idel.tests.Idel
import idel.tests.infrastructure.BodyFieldCheck
import java.net.http.HttpResponse

class IdeasApi(username: String, idelUrl: String = Idel.URL) : AbstractObjectApi(username, idelUrl, "ideas") {

    /**
     * Add new idea.
     */
    fun add(groupId: String,
            summary: String = "Idea from test",
            description: String = "description of $summary"): HttpResponse<JsonNode> {
        val body = """
            {
                "groupId" : "$groupId",
                "summary" : "$summary",
                "description": "$description",
                "descriptionPlainText": "$description",
                "link": "http://somelink.io/$summary"
            }
        """.trimIndent()

        return post("", body)
    }

    fun update(ideaId: String,
               summary: String = "Edited title",
               description: String = "edited description"): HttpResponse<JsonNode> {
        val body = """{
                "summary" : "$summary",
                "description": "$description",
                "descriptionPlainText": "$description",
                "link": "http://somelink.io/$summary"
            }""".trimMargin()

        return patch("/$ideaId", body)
    }

    fun load(ideaId: String): HttpResponse<JsonNode> {
        return get("/$ideaId")
    }

    fun assign(ideaId: String, assignee: User): HttpResponse<JsonNode> {
        val body = """{
            "userId":"${assignee.id}"
        }""".trimIndent()

        return patch("/$ideaId/assignee", body)
    }

    fun removeAssignee(ideaId: String): HttpResponse<JsonNode> {
        val body = """{
          "userId": ""   
        }""".trimIndent()

        return patch("/$ideaId/assignee", body)
    }

    fun implemented(ideaId: String): HttpResponse<JsonNode> {
        val body = """{
                "implemented": "true"
            }""".trimIndent()

        return patch("/$ideaId/implemented", body)
    }

    fun notImplemented(ideaId: String): HttpResponse<JsonNode> {
        val body = """{
                "implemented": "false"
            }""".trimIndent()

        return patch("/$ideaId/implemented", body)
    }
}

fun ideaAssigneeIs(user: User) = BodyFieldCheck("assignee is $user", "$.data.assignee", user.id)
fun ideaNotAssigned() = BodyFieldCheck("idea is not assigned", "$.data.assignee", "")
val ideaIsImplemented = BodyFieldCheck("idea is implemented", "$.data.implemented","true")
val ideaIsNotImplemented = BodyFieldCheck("idea is not implemented", "$.data.implemented", "false")
