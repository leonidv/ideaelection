package idel.tests.apiobject

import com.fasterxml.jackson.databind.JsonNode
import idel.tests.TestConfig
import idel.tests.infrastructure.BodyArrayOrder
import idel.tests.infrastructure.BodyArraySize
import idel.tests.infrastructure.BodyFieldValueChecker
import idel.tests.infrastructure.ResponseChecker
import mu.KotlinLogging
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

interface NotificationFrequencyValues {
    val INSTANTLY: String
    val DAILY: String
    val WEEKLY: String
    val DISABLED: String
}


class UserApi(user: User, idelUrl: String = TestConfig.backendUrl) : AbstractObjectApi(user, idelUrl, "users") {

    companion object {
        const val PLAN_FREE = "FREE"
        const val PLAN_BASIC = "BASIC"
        const val PLAN_ENTERPRISE = "ENTERPRISE"

        val NotificationFrequency = object : NotificationFrequencyValues {
            override val INSTANTLY = "INSTANTLY"
            override val DAILY = "DAILY"
            override val WEEKLY = "WEEKLY"
            override val DISABLED = "DISABLED"
        }
    }

    private val log = KotlinLogging.logger {}


    fun register(
        user: User,
        displayName: String = "${user.id} Registered from a test ${LocalDateTime.now()}",
        email: String = user.email,
        plan: String = PLAN_FREE
    ): HttpResponse<JsonNode> {
        val body = """
            {
                "id": "${user.id}",
                "externalId" :"${user.externalId}",
                "email": "$email",
                "displayName": "$displayName",
                "avatar": "",
                "roles": [
                    "ROLE_USER"
                ],
                "subscriptionPlan": "$plan"
            }
        """.trimIndent()

        return post("", body)
    }

    fun loadSettings(): HttpResponse<JsonNode> {
        return get("/settings")
    }

    fun updateSettings(
        displayName: String,
        subscriptionPlan: String,
        notificationFrequency: String,
        subscribedToNews: Boolean
    ): HttpResponse<JsonNode> {
        val body = """{
             "displayName": "$displayName",
             "subscriptionPlan": "$subscriptionPlan",
             "settings": {
                "notificationsFrequency": "$notificationFrequency",
                "subscribedToNews": $subscribedToNews
             }
        }""".trimIndent()

        return put("/settings", body)
    }

    fun load(userId: String): HttpResponse<JsonNode> {
        return get("/$userId")
    }

    fun list(filter: String? = null, skip: Int? = null, count: Int? = null): HttpResponse<JsonNode> {
        val params = mutableListOf<String>()
        filter?.let {params.add("filter=$it")}
        skip?.let {params.add("skip=$it")}
        count?.let {params.add("count=$it")}
        val query = params.joinToString(separator = "&", prefix = "?", postfix = "")
        return get(query)
    }

    /**
     * Load user's information by (users/me endpoint) using JWT token
     */
    fun jwtAuth(jwt: String): HttpResponse<JsonNode> {
        val uri = URI.create("$resourceUri/me")

        val request = HttpRequest
            .newBuilder(uri)
            .timeout(Duration.ofHours(1))
            .header("Authorization", "Bearer $jwt")
            .header("Content-Type", "application/json")
            .GET().build()

        return send(request, "")
    }
}

const val DEFAULT_DOMAIN = "mail.fake"

object UsersResponseChecks {


    fun hasNotificationFrequency(value: String) =
        BodyFieldValueChecker.forField("settings.notificationsFrequency", value)

    fun hasSubscribedToNews(value: Boolean) =
        BodyFieldValueChecker.forField("settings.subscribedToNews", value.toString())

    fun hasMustReissueJwt(value: Boolean) = BodyFieldValueChecker.forField("mustReissueJwt", value.toString())

    fun hasId(userId: String) = BodyFieldValueChecker.forField("id", userId)

    fun hasEmail(value: String) = BodyFieldValueChecker.forField("email", value.lowercase(Locale.getDefault()))

    fun hasDisplayName(value: String) = BodyFieldValueChecker.forField("displayName", value)

    fun hasSubscriptionPlan(value: String) = BodyFieldValueChecker.forField("subscriptionPlan", value)

    fun hasDomain(value: String) = BodyFieldValueChecker.forField("domain", value)
}

object UsersListChecks {
    fun hasUsersInOrder(users: List<User>): Array<ResponseChecker> {
        val values = users.map {it.id}
        return arrayOf(
            BodyArraySize("user's list size is ${users.size}", "$.data", users.size),
            BodyArrayOrder(
                "user's list is ${users.map {it.name}}", "$.data", "id", values.toTypedArray()
            )
        )
    }
}