package idel.tests.apiobject

import arrow.core.Option
import arrow.core.getOrElse
import com.fasterxml.jackson.databind.JsonNode
import idel.tests.Idel
import idel.tests.infrastructure.*
import java.net.http.HttpResponse

class IdeasApi(username: String, idelUrl: String = Idel.URL) : AbstractObjectApi(username, idelUrl, "ideas") {

    companion object {
        const val ORDER_CTIME_ASC = "ctime_asc"
        const val ORDER_CTIME_DESC = "ctime_desc"
        const val ORDER_VOTES_DESC = "votes_desc"

    }

    /**
     * Add new idea.
     */
    fun add(
        groupId: String,
        summary: String = "Idea from test",
        description: String = "description of $summary",
        descriptionPlainText: String = description,
        link: String = "http://somelink.io/"
    ): HttpResponse<JsonNode> {
        val body = """
            {
                "groupId" : "$groupId",
                "summary" : "$summary",
                "description": "$description",
                "descriptionPlainText": "$descriptionPlainText",
                "link": "$link"
            }
        """.trimIndent()

        return post("", body)
    }

    fun quickAdd(groupId: String, version: CharSequence): HttpResponse<JsonNode> {
        return add(
            groupId = groupId,
            summary = "summary $version",
            description = "description [b]$version[/b]",
            descriptionPlainText = "description $version",
            link = "http://somelink.io/$version"
        )
    }
    /**
     * Update idea by template. All field contains version. Use it for quick tests,
     * when values of field are not important.
     */
    fun quickEdit(
        ideaId: String,
        version: CharSequence
    ): HttpResponse<JsonNode> {
        return edit(
            ideaId,
            summary = "summary $version",
            description = "description [b]$version[/b]",
            descriptionPlainText = "description $version",
            link = "http://somelink.io/$version"
        )
    }

    fun edit(
        ideaId: String,
        summary: String,
        description: String,
        descriptionPlainText: String,
        link: String
    ): HttpResponse<JsonNode> {
        val body = """{
                "summary" : "$summary",
                "description": "$description",
                "descriptionPlainText": "$descriptionPlainText",
                "link": "$link"
            }""".trimMargin()

        return patch("/$ideaId", body)

    }

    fun load(
        ideaId: String,
    ): HttpResponse<JsonNode> {
        return get("/$ideaId")
    }

    fun list(groupId: String,
             ordering : Option<String> = Option.empty(),
             offeredBy : Option<String> = Option.empty(),
             assignee: Option<String> = Option.empty(),
             implemented: Option<String> = Option.empty(),
             text : Option<String> = Option.empty()
    ): HttpResponse<JsonNode> {
        val params = listOf(
            Option.just("groupId=$groupId"),
            ordering.map {"ordering=$it"},
            offeredBy.map {"offered-by=$it"},
            assignee.map {"assignee=$it"},
            implemented.map {"implemented=$it"},
            text.map {"text=$it"}
        )
            .map {it.getOrElse {""}}
            .filter {it.isNotEmpty()}
            .joinToString(separator = "&")


        return get("?$params")
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

    fun vote(ideaId: String) : HttpResponse<JsonNode> {
        return post("/$ideaId/voters","")
    }

    fun devote(ideaId: String) : HttpResponse<JsonNode> {
        return delete("/$ideaId/voters","")
    }
}

/**
 * Fields checks
 */
fun ideaAssigneeIs(user: User) = BodyFieldValueChecker.forField("assignee",user.id)
fun ideaNotAssigned() = BodyFieldValueChecker("idea is not assigned", "$.data.assignee", "")
val ideaIsImplemented = BodyFieldValueChecker("idea is implemented", "$.data.implemented", "true")
val ideaIsNotImplemented = BodyFieldValueChecker("idea is not implemented", "$.data.implemented", "false")
fun ideaHasSummary(summary: String) = BodyFieldValueChecker.forField("summary", summary)
fun ideaHasDescription(description: String) = BodyFieldValueChecker.forField("description", description)

fun ideaHasDescriptionPlainText(description: String) = BodyFieldValueChecker.forField("descriptionPlainText", description)

fun ideaHasLink(link: String) = BodyFieldValueChecker.forField("link", link)

fun containsIdeaWithSummary(summary: String) = BodyContainsObject(
    "contains idea with summary [$summary]", "$.data", arrayOf(Pair("summary", summary))
)

fun ideaHasVoterCount(votersCount : Int) = BodyArraySize("has $votersCount voters", "$.data.voters",votersCount)
fun ideaHasVoter(user : User) = BodyArrayElementExists("has voter [${user.id}]", "$.data.voters", user.id)

fun ideasOrder(ids : Array<String>) = BodyArrayOrder("has correct order", "$.data", "id", ids)