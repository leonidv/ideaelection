package idel.tests.apiobject

import com.fasterxml.jackson.databind.JsonNode
import idel.tests.Idel
import idel.tests.infrastructure.ofJson
import mu.KotlinLogging
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.*

abstract class AbstractObjectApi(
    val user: User,
    val idelUrl: String = Idel.URL,
    /**
     * Without trailing slash
     */
    resource: String) {
    private val log = KotlinLogging.logger {}

    protected val resourceUri: URI = URI.create("$idelUrl/$resource")

    protected val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(1))
        .version(HttpClient.Version.HTTP_1_1)
        .build()


    private fun basicAuth(username: String, password: String): String {
        return "Basic " + Base64.getEncoder().encodeToString("$username:$password".toByteArray())
    }

    protected fun requestBuilder(params: String): HttpRequest.Builder {
        val uri = URI.create("$resourceUri$params")

        log.trace {"Create http client with URI $uri"}

        return HttpRequest
            .newBuilder(uri)
            .timeout(Duration.ofHours(1))
            .header("Authorization", basicAuth(user.name, user.name))
            .header("Content-Type", "application/json")

    }

    protected fun send(request: HttpRequest, bodyForLog: String): HttpResponse<JsonNode> {
        val bodyMsg = if (bodyForLog.length < 2000) {
            bodyForLog
        } else {
            "body is too big for log"
        }
        log.trace {"\n${user.id} ${user.domain} ${request.method()} ${request.uri()} \n$bodyMsg"}
        return client.send(request, ofJson())!!
    }

    protected fun post(params: String, body: String): HttpResponse<JsonNode> {
        val request = requestBuilder(params)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        return send(request, body)
    }

    protected fun put(params: String, body: String): HttpResponse<JsonNode> {
        val request = requestBuilder(params)
            .PUT(HttpRequest.BodyPublishers.ofString(body))
            .build()


        return send(request, body)
    }

    protected fun patch(params: String, body: String): HttpResponse<JsonNode> {
        val request = requestBuilder(params)
            .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
            .build()

        return send(request, body)
    }

    protected fun get(params: String): HttpResponse<JsonNode> {
        val request = requestBuilder(params).GET().build()

        return send(request, "")
    }

    protected fun delete(params: String, body: String): HttpResponse<JsonNode> {
        val request = requestBuilder(params)
            .method("DELETE", HttpRequest.BodyPublishers.ofString(body))
            .build()

        return send(request, body)
    }
}