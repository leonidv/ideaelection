package idel.tests.apiobject

import com.fasterxml.jackson.databind.JsonNode
import idel.tests.Idel
import idel.tests.infrastructure.IdelHttpAuthenticator
import idel.tests.infrastructure.ofJson
import mu.KotlinLogging
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

abstract class AbstractObjectApi(val username: String, val idelUrl: String = Idel.URL, val resource: String) {
    private val log = KotlinLogging.logger {}

    val resourceUri = URI.create("$idelUrl/$resource")

    protected val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(1))
        .authenticator(IdelHttpAuthenticator(username))
        .build()


    protected fun asJson(array: Collection<Any>): String = array.map {""" "$it" """}
        .joinToString(prefix = "[", postfix = "]")


    protected fun requestBuilder(params: String): HttpRequest.Builder {
        val uri = URI.create("$resourceUri$params")

        log.trace {"Create http client with URI $uri"}

        return HttpRequest
            .newBuilder(uri)
            .timeout(Duration.ofHours(1))
            .header("Content-Type", "application/json")

    }

    protected fun post(params: String, body: String): HttpResponse<JsonNode> {
        val request = requestBuilder(params)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        log.trace {"${request.method()} ${request.uri()} $body"}
        return client.send(request, ofJson())!!
    }

    protected fun put(params: String, body: String): HttpResponse<JsonNode> {
        val request = requestBuilder(params)
            .PUT(HttpRequest.BodyPublishers.ofString(body))
            .build()

        log.trace {"${request.method()} ${request.uri()} $body"}
        return client.send(request, ofJson())
    }

    protected fun patch(params: String, body: String): HttpResponse<JsonNode> {
        val request = requestBuilder(params)
            .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
            .build()
        log.trace {"${request.method()} ${request.uri()} $body"}
        return client.send(request, ofJson())
    }

    protected fun get(params: String): HttpResponse<JsonNode> {
        val request = requestBuilder(params).GET().build()
        log.trace {"GET ${request.uri()}"}
        return client.send(request, ofJson())!!
    }

}