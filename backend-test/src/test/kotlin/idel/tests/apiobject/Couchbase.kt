package idel.tests.apiobject

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import idel.tests.Idel
import idel.tests.infrastructure.IdelHttpAuthenticator
import mu.KotlinLogging
import java.net.HttpURLConnection
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration


class Couchbase(val idelUrl  : String = Idel.URL) {

    val log = KotlinLogging.logger {}

    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(1))
        .authenticator(IdelHttpAuthenticator("test"))
        .build()

    private fun deleteEntity(entityType : String) {
        val request = HttpRequest
            .newBuilder()
            .DELETE()
            .uri(URI.create("$idelUrl/configs/couchbase/$entityType"))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        val json = jacksonObjectMapper().readValue(response.body()) as JsonNode

        if (response.statusCode() == HttpURLConnection.HTTP_OK) {
            log.info("Clear $entityType. ${json["data"]}")
        } else {
            log.warn("Something is wrong, ${json["error"]}")
            throw RuntimeException("Can't clear $entityType. ${response.body()}")
        }

    }

    fun clear() {
        clearIdeas()
        clearUsers()
        clearGroups()
        clearJoinRequests()
    }
    /**
     * Remove all ideas from bucket.
     *
     * Use direct connection to couchbase server.
     */
    fun clearIdeas() {
        this.deleteEntity("idea")
    }

    fun clearUsers() {
        this.deleteEntity("user")
    }

    fun clearJoinRequests() {
        this.deleteEntity("joinrequest")
    }

    fun clearGroups() {
        this.deleteEntity("group")
    }
}
