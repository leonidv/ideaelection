package idel.tests.apiobject

import com.fasterxml.jackson.databind.JsonNode
import idel.tests.Idel
import mu.KotlinLogging
import java.net.http.HttpResponse


class GroupsApi(username: String, idelUrl: String = Idel.URL) : AbstractObjectApi(username, idelUrl, "groups") {
    private val log = KotlinLogging.logger {}

    companion object {
        const val PUBLIC = "PUBLIC"
        const val CLOSED = "CLOSED"
        const val PRIVATE = "PRIVATE"
    }


    fun create(title: String,
               entryMode: String,
               description: String = "$title, $entryMode",
               admins: Set<String> = setOf("$username@httpbasic"),
               members: Set<String> = setOf()
               ): HttpResponse<JsonNode> {
        val body = """
            {
                "title": "$title",
                "description": "$description",
                "logo": "data:image/png;base64,dGVzdA==",
                "entryMode" : "$entryMode",
                "administrators": ${asJson(admins)},
                "members": ${asJson(members)}
            }
        """.trimIndent()

        log.trace {"GroupsApi.add body = $body"}

        return post(body)
    }

//    fun loadAvailable() : {
//        va
//    }
}