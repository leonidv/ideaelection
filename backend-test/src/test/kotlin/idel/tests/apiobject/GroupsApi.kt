package idel.tests.apiobject

import arrow.core.Option
import arrow.core.getOrElse
import com.fasterxml.jackson.databind.JsonNode
import idel.tests.Idel
import idel.tests.infrastructure.*
import mu.KotlinLogging
import java.net.http.HttpResponse

interface GroupsFields {
    val JOINING_KEY : String
}

class GroupsApi(username: String, idelUrl: String = Idel.URL) : AbstractObjectApi(username, idelUrl, "groups") {
    private val log = KotlinLogging.logger {}

    companion object {
        const val PUBLIC = "PUBLIC"
        const val CLOSED = "CLOSED"
        const val PRIVATE = "PRIVATE"

        const val MEMBER = "MEMBER"
        const val ADMIN = "GROUP_ADMIN"

        const val DELETED = "DELETED"
        const val ACTIVE = "ACTIVE"

        val Fields = object : GroupsFields{
            override val JOINING_KEY: String = "joiningKey"
        }
    }


    fun create(
        name: String,
        entryMode: String,
        description: String = "$name, $entryMode",
        entryQuestion: String = "",
        domainRestrictions : Array<String> = emptyArray()
    ): HttpResponse<JsonNode> {

        val domainRestrictionsJson = domainRestrictions.toJsonArray()

        val body = """
            {
                "name": "$name",
                "description": "$description",
                "logo": "data:image/png;base64,dGVzdA==",
                "entryMode" : "$entryMode",
                "entryQuestion" : "$entryQuestion",
                "domainRestrictions": $domainRestrictionsJson
            }
        """.trimIndent()

        return post("", body)
    }

    /**
     * Load group by id
     */
    fun load(groupId : String) : HttpResponse<JsonNode> = get("/$groupId")

    fun loadByLinkToJoin(joiningKey : String) = get("/?key=$joiningKey")

    fun delete(groupId: String) : HttpResponse<JsonNode> = super.delete("/$groupId","")

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
        return delete("/$groupId/members/$userId", "")
    }

    fun changeRoleInGroup(groupId: String, userId: String, nextRole: String): HttpResponse<JsonNode> {
        val body = """ { 
            "roleInGroup": "$nextRole" 
           }
        """.trimIndent()

        return patch("/$groupId/members/$userId/role-in-group", body)
    }


    fun changeProperties(
        groupId: String,
        name: String,
        description: String,
        entryMode: String,
        entryQuestion: String,
        domainRestrictions: Array<String>,
        logo: String = "data:image/png;base64,dGVzdA=="
    ): HttpResponse<JsonNode> {
        val domainRestrictionsJson = domainRestrictions.toJsonArray()
        val body = """ {
                "name": "$name",
                "description": "$description",
                "logo": "$logo",
                "entryMode" : "$entryMode",
                "entryQuestion" : "$entryQuestion",
                "domainRestrictions": $domainRestrictionsJson
        }    
        """.trimMargin()

        return patch("/$groupId", body)
    }

    fun regenerateJoiningKey(groupId: String) : HttpResponse<JsonNode> {
        return delete("/$groupId/joining-key","")
    }
}


fun groupHasName(name: String) = BodyFieldValueChecker.forField("name", name)
fun groupHasDescription(description: String) = BodyFieldValueChecker.forField("description", description)
fun groupHasEntryMode(entryMode: String) = BodyFieldValueChecker.forField("entryMode", entryMode)
fun groupHasEntryQuestion(question : String) = BodyFieldValueChecker.forField("entryQuestion", question)
fun groupHasCreator(user: User) = BodyFieldValueChecker.forField("creator.id", user.id)
fun groupHasDomainRestrictionsCount(restrictionCount : Int) = BodyArraySize(
    "has $restrictionCount domain restriction count",
    "$.data.domainRestrictions",restrictionCount)
fun groupHasDomainRestriction(domain : String) = BodyArrayElementExists("has domain restriction [${domain}]", "$.data.domainRestrictions", domain)
fun groupHasJoiningKey(joiningKey: String) = BodyFieldValueChecker.forField("joiningKey", joiningKey)

fun groupHasMembersCount(count : Int) = BodyFieldValueChecker.forField("membersCount",count.toString())
fun groupHasIdeasCount(count: Int) = BodyFieldValueChecker.forField("ideasCount", count.toString())

fun groupHasMemberWithRole(user: User, role: String) =
    BodyContainsObject(
        "has [$user] as [$role]",
        "$.data",
        fields = arrayOf(Pair("userId", user.id), Pair("roleInGroup", role))
    )

fun groupHasAdmin(user: User) = groupHasMemberWithRole(user, GroupsApi.ADMIN)
fun groupHasMember(user: User) = groupHasMemberWithRole(user, GroupsApi.MEMBER)

fun groupHasNotMember(user: User) = NotBodyContainsObject("has member $user", "$.data", arrayOf(Pair("id", user.id)))

fun noGroups() = BodyArraySize("no any groups", "$.data", 0)
fun includeGroup(groupId: String) =
    BodyContainsObject("includes groups $groupId", "$.data", arrayOf(Pair("id", groupId)))

fun notIncludeGroup(groupId: String) = NotBodyContainsObject("doesn't include group $groupId", "$.data", arrayOf(Pair("id", groupId)))

val groupHasStateDeleted = BodyFieldValueChecker.forField("state",GroupsApi.DELETED)