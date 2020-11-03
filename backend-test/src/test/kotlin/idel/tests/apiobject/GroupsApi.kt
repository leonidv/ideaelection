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


class GroupsApi(val username: String, val idelUrl: String = Idel.URL) {
    val log = KotlinLogging.logger {}

    companion object {
        val PUBLIC = "PUBLIC"
        val CLOSED = "CLOSED"
        val PRIVATE = "PRIVATE"
    }

    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(1))
        .authenticator(IdelHttpAuthenticator(username))
        .build()

    val resourceUri = URI.create("$idelUrl/groups")


    fun create(title: String,
               entryMode: String,
               description: String = "$title, $entryMode",
               admins: Set<String> = setOf("$username@httpbasic")): HttpResponse<JsonNode> {
        val body = """
            {
                "title": "$title",
                "description": "$description",
                "entryMode" : "$entryMode",
                "administrators": ${admins.map {""" "$it" """}. joinToString(prefix = "[", postfix = "]")}
            }
        """.trimIndent()

        log.trace {"GroupsApi.add body = $body"}

        val request = HttpRequest
            .newBuilder(resourceUri)
            .timeout(Duration.ofSeconds(1))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        return client.send(request, ofJson())!!
    }
}