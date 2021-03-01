package idel.tests.apiobject

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.orElse
import com.fasterxml.jackson.databind.JsonNode
import idel.tests.Idel
import idel.tests.infrastructure.*
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

    /**
     * Load members of group.
     *
     */
    fun loadMembers(
        groupId: String,
        usernameFilter: Option<String> = Option.empty(),
        first: Option<String> = Option.empty(),
        last: Option<String> = Option.empty()
    ): HttpResponse<JsonNode> {
        val params = listOf(
            usernameFilter.map {"username=$it"}.getOrElse {""},
            first.map {"first=$it"}.getOrElse {""},
            last.map {"last=$it"}.getOrElse {""}
        )
            .filterNot {it.isEmpty()}
            .joinToString(prefix = "?", separator = "&")

        return get("/$groupId/members/$params")
    }

    fun deleteMember(
        groupId: String,
        userId: String
    ): HttpResponse<JsonNode> {
        return delete("/$groupId/members/$userId","")
    }
}


fun groupHasName(name: String) = BodyFieldValueChecker.forField("name", name)
fun groupHasDescription(description: String) = BodyFieldValueChecker.forField("description", description)
fun groupHasEntryMode(entryMode: String) = BodyFieldValueChecker.forField("entryMode", entryMode)
fun groupHasAdmin(user: User) = BodyArrayElementExists("has admin $user", "$.data.administrators", "id", user.id)
fun groupHasMember(user: User) = BodyArrayElementExists("has member $user", "$.data", "id", user.id)
fun groupHasNotMember(user: User) = NotBodyArrayElementExists("has member $user", "$.data", "id", user.id)

fun noGroups() = BodyArraySize("no any groups", "$.data", 0)
fun includeGroup(groupId: String) = BodyArrayElementExists("includes groups $groupId", "$.data", "id", groupId)


