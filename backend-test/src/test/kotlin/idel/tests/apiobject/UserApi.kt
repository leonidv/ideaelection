package idel.tests.apiobject

import com.fasterxml.jackson.databind.JsonNode
import idel.tests.Idel
import idel.tests.infrastructure.BodyFieldValueChecker
import jdk.jfr.Frequency
import mu.KLogger
import mu.KotlinLogging
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.LocalDateTime

interface NotificationFrequencyValues {
    val INSTANTLY: String
    val DAILY: String
    val WEEKLY: String
    val DISABLED: String
}


class UserApi(user: User, idelUrl: String = Idel.URL) : AbstractObjectApi(user, idelUrl, "users") {

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


    fun register(user: User, displayName: String = "${user.id} Registered from a test ${LocalDateTime.now()}", email: String = user.email, plan: String = PLAN_FREE): HttpResponse<JsonNode> {
        val body = """
            {
                "id": "${user.id}",
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

    /**
     * Load user's information by (users/me endpoint) using JWT token
     */
    fun jwtAuth(jwt : String) : HttpResponse<JsonNode> {
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

fun hasNotificationFrequency(value: String) =
    BodyFieldValueChecker.forField("settings.notificationsFrequency", value)

fun hasSubscribedToNews(value: Boolean) =
    BodyFieldValueChecker.forField("settings.subscribedToNews", value.toString())

fun hasMustReissueJwt(value: Boolean) = BodyFieldValueChecker.forField("mustReissueJwt", value.toString())
