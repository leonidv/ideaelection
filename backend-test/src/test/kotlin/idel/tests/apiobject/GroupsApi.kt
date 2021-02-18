package idel.tests.apiobject

import com.fasterxml.jackson.databind.JsonNode
import idel.tests.Idel
import idel.tests.infrastructure.BodyArrayElementExists
import idel.tests.infrastructure.BodyArraySize
import idel.tests.infrastructure.BodyFieldValueChecker
import idel.tests.infrastructure.asUserId
import mu.KotlinLogging
import java.net.http.HttpResponse


class GroupsApi(username: String, idelUrl: String = Idel.URL) : AbstractObjectApi(username, idelUrl, "groups") {
    private val log = KotlinLogging.logger {}

    companion object {
        const val PUBLIC = "PUBLIC"
        const val CLOSED = "CLOSED"
        const val PRIVATE = "PRIVATE"
    }


    fun create(
        name: String,
        entryMode: String,
        description: String = "$name, $entryMode",
        admins: Set<String> = setOf("$username@httpbasic")
    ): HttpResponse<JsonNode> {
        val body = """
            {
                "name": "$name",
                "description": "$description",
                "logo": "data:image/png;base64,dGVzdA==",
                "entryMode" : "$entryMode",
                "administrators": ${asJson(admins)}
            }
        """.trimIndent()

        return post("", body)
    }

    /**
     * Return available groups
     */
    fun loadAvailable() = get("?onlyAvailable")

    /**
     * Load user's groups.
     */
    fun loadForUser(userId: String = username.asUserId()) = get("?userId=$userId")
}

fun groupHasName(name: String) = BodyFieldValueChecker.forField("name", name)
fun groupHasDescription(description: String) = BodyFieldValueChecker.forField("description", description)
fun groupHasEntryMode(entryMode: String) = BodyFieldValueChecker.forField("entryMode", entryMode)
fun groupHasAdmin(user: User) = BodyArrayElementExists("has admin $user", "$.data.administrators", "id", user.id)

fun noGroups() = BodyArraySize("no any groups", "$.data", 0)
fun includeGroup(groupId: String) = BodyArrayElementExists("includes groups $groupId", "$.data", "id", groupId)


