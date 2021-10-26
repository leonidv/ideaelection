package idel.tests.apiobject

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.getOrElse
import com.fasterxml.jackson.databind.JsonNode
import idel.tests.Idel
import idel.tests.infrastructure.*
import idel.tests.infrastructure.JsonNodeExtensions.queryString
import java.net.http.HttpResponse

class IdeasApi(user: User, idelUrl: String = Idel.URL) : AbstractObjectApi(user, idelUrl, "ideas") {

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

    fun list(
        groupId: String,
        ordering: String? = null,
        offeredBy: String? = null,
        assignee: String? = null,
        implemented: String? = null,
        text: String? = null,
        archived: String? = null
    ): HttpResponse<JsonNode> {
        val params = listOfNotNull(
            "groupId=$groupId",
            ordering?.let {"ordering=$it"},
            offeredBy?.let {"offered-by=$it"},
            assignee?.let {"assignee=$it"},
            implemented?.let {"implemented=$it"},
            text?.let {"text=$it"},
            archived?.let {"archived=$it"}
        ).joinToString(separator = "&")

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

    fun vote(ideaId: String): HttpResponse<JsonNode> {
        return post("/$ideaId/voters", "")
    }

    fun devote(ideaId: String): HttpResponse<JsonNode> {
        return delete("/$ideaId/voters", "")
    }

    fun delete(ideaId: String): HttpResponse<JsonNode> {
        return delete("/$ideaId", "")
    }


    fun changeArchived(ideaId: String, archived: Boolean): HttpResponse<JsonNode> {
        val body = """{
            | "archived" : $archived
            |}
        """.trimMargin()
        return patch("/$ideaId/archived", body)
    }

    fun changeGroup(ideaId: String, groupId: String): HttpResponse<JsonNode> {
        val body = """{
            | "groupId" : "$groupId"
            | }
        """.trimMargin()

        return patch("/$ideaId/group", body)
    }

}


/**
 * Fields checks
 */
fun ideaHasGroup(groupId: String) = BodyFieldValueChecker.forField("idea.groupId", groupId)
fun ideaAssigneeIs(user: User) = BodyFieldValueChecker.forField("idea.assignee", user.id)
fun ideaNotAssigned() = BodyFieldValueChecker("idea is not assigned", "$.data.idea.assignee", "")
val ideaIsImplemented = BodyFieldValueChecker("idea is implemented", "$.data.idea.implemented", "true")
val ideaIsNotImplemented = BodyFieldValueChecker("idea is not implemented", "$.data.idea.implemented", "false")
fun ideaHasSummary(summary: String) = BodyFieldValueChecker.forField("idea.summary", summary)
fun ideaHasDescription(description: String) = BodyFieldValueChecker.forField("idea.description", description)
fun ideaHasDescriptionPlainText(description: String) =
    BodyFieldValueChecker.forField("idea.descriptionPlainText", description)

fun ideaHasLink(link: String) = BodyFieldValueChecker.forField("idea.link", link)
fun ideaHasVoterCount(votersCount: Int) = BodyArraySize("has $votersCount voters", "$.data.idea.voters", votersCount)
fun ideaHasVoter(user: User) = BodyArrayElementExists("has voter [${user.id}]", "$.data.idea.voters", user.id)



val ideaIsDeleted = BodyFieldValueChecker.forField("idea.deleted", "true")

val ideaIsArchived = BodyFieldValueChecker.forField("idea.archived", "true")
val ideaIsNotArchived = BodyFieldValueChecker.forField("idea.archived", "false")

fun ideasCount(count: Int) = BodyArraySize("response contains $count ideas", "$.data.ideas", count)
fun ideasContainsIdeaWithSummary(summary: String) = BodyContainsObject(
    "contains idea with summary [$summary]", "$.data.ideas", arrayOf(Pair("summary", summary))
)

fun includeIdea(id: String) = BodyContainsObject(
    "contains idea [$id]", "$.data.ideas", arrayOf(Pair("id", id))
)

fun notIncludesIdea(id: String) = NotBodyContainsObject(
    "not contains idea [$id]", "$.data.ideas", arrayOf(Pair("id", id))
)

fun ideasOrder(ids: Array<String>) = BodyArrayOrder("has correct order", "$.data.ideas", "id", ids)

fun usersInfoCount(count: Int) = BodyArraySize("response contains $count users", "$.data.users", count)
fun usersInfoContains(user: Set<User>) =
    BodyArrayContainsObjects("has user's info", "$.data.users", "id", user.map {it.id}.toSet())



