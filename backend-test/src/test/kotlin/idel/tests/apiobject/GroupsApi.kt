package idel.tests.apiobject

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import idel.tests.Idel
import idel.tests.infrastructure.IdelHttpAuthenticator
import idel.tests.infrastructure.ofJson
import mu.KotlinLogging
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration


class GroupsApi(username: String, idelUrl: String = Idel.URL) : AbstractObjectApi(username, idelUrl, "groups") {
    val log = KotlinLogging.logger {}

    companion object {
        val PUBLIC = "PUBLIC"
        val CLOSED = "CLOSED"
        val PRIVATE = "PRIVATE"
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

        return post(body)!!
    }

    fun loadAvailable() : {
        va
    }
}