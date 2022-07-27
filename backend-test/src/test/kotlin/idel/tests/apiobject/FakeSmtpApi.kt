package idel.tests.apiobject

import com.fasterxml.jackson.databind.JsonNode
import idel.tests.TestConfig
import idel.tests.infrastructure.BodyContainsObject
import idel.tests.infrastructure.ofJson
import mu.KotlinLogging
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * API Object for https://github.com/gessnerfl/fake-smtp-server
 */
class FakeSmtpApi(baseUrl : String = TestConfig.fakeSmtpUrl)  {
    private val client : HttpClient = HttpClient.newBuilder().apply {
        connectTimeout(Duration.ofSeconds(1))
        version(HttpClient.Version.HTTP_1_1)
    }.build()

    private val log = KotlinLogging.logger {};

    private val resourceUrl = "$baseUrl/api/email"

    fun list(page : Int = 0, size :Int = 100) : HttpResponse<JsonNode> {
        val params = "?page=$page&size=$size"

        val uri = URI.create(resourceUrl + params)

        val request = HttpRequest
            .newBuilder(uri)
            .GET()
            .build()

        log.trace {"GET ${request.uri()}"}

        return client.send(request, ofJson())!!
    }

    fun deleteAll() : HttpResponse<Void> {
        val uri = URI.create(resourceUrl)

        val request = HttpRequest
            .newBuilder(uri)
            .DELETE()
            .build();

        log.trace {"DELETE ${request.uri()}"}

        return client.send(request, HttpResponse.BodyHandlers.discarding())
    }
}

object FakeSmtpCheckers {
    fun hasEmailFor(address: String) = BodyContainsObject(
        testName = "Has email",
        objectPath = "$",
        fields = arrayOf(Pair("toAddress", address))
    )
}