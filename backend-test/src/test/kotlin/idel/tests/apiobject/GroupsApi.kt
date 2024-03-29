package idel.tests.apiobject

import arrow.core.None
import arrow.core.Option
import arrow.core.getOrElse
import com.fasterxml.jackson.databind.JsonNode
import idel.tests.TestConfig
import idel.tests.infrastructure.*
import java.net.http.HttpResponse

interface GroupsFields {
    val JOINING_KEY : String
}

class GroupsEntryMode {
    val PUBLIC = "PUBLIC"
    val CLOSED = "CLOSED"
    val PRIVATE = "PRIVATE"
}

class GroupsApi(user: User, idelUrl: String = TestConfig.backendUrl) : AbstractObjectApi(user, idelUrl, "groups") {

    companion object {
        val EntryMode = GroupsEntryMode()

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
    fun loadAvailable(userId: String = user.id, name: Option<String> = None) : HttpResponse<JsonNode> {
      val nameArg = name.map {"&name=$it"}.getOrElse {""}
      return get("?onlyAvailable$nameArg")
    }

    /**
     * Load user's groups.
     */
    fun loadForUser(
        userId: String = user.id,
        name: Option<String> = None
    ) : HttpResponse<JsonNode> {
        val nameArg = name.map {"&name=$it"}.getOrElse {""}
        return get("?userId=$userId$nameArg")
    }

    /**
     * Load members of group.
     *
     */
    fun loadMembers(
        groupId: String,
        usernameFilter: String? = null,
        skip: Int? = null,
        count: Int? = null
    ): HttpResponse<JsonNode> {
        val params = listOf(
            usernameFilter?.let {"username=$it"},
            skip?.let {"skip=$it"},
            count?.let {"count=$it"}
        )
            .filterNotNull()
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

fun hasGroupsCount(count : Int) = BodyArraySize("groups count", "$.data.groups", count)
fun noGroups() = BodyArraySize("no any groups", "$.data.groups", 0)
fun includeGroup(groupId: String, groupInTestName : String? = null) =
    BodyContainsObject("includes group [${groupInTestName?:groupId}]", "$.data.groups", arrayOf(Pair("id", groupId)))

fun notIncludeGroup(groupId: String) = NotBodyContainsObject("doesn't include group $groupId", "$.data.groups", arrayOf(Pair("id", groupId)))

val groupHasStateDeleted = BodyFieldValueChecker.forField("state",GroupsApi.DELETED)