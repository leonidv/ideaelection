package idel.tests.apiobject

import com.fasterxml.jackson.databind.JsonNode
import idel.tests.Idel
import idel.tests.infrastructure.IdelHttpAuthenticator
import idel.tests.infrastructure.ofJson
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

abstract class AbstractObjectApi(val username :String, val idelUrl : String = Idel.URL, val resource : String) {
    val resourceUri = URI.create("$idelUrl/$resource")

    protected val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(1))
        .authenticator(IdelHttpAuthenticator(username))
        .build()


    protected fun asJson(array: Collection<Any>) : String = array.map {""" "$it" """}. joinToString(prefix = "[", postfix = "]")

    protected fun requestBuilder() : HttpRequest.Builder =
                HttpRequest
                    .newBuilder(resourceUri)
                    .timeout(Duration.ofSeconds(1))
                    .header("Content-Type", "application/json")

    protected fun post(body : String): HttpResponse<JsonNode> {
        val request = requestBuilder()
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        return client.send(request, ofJson())!!
    }

}