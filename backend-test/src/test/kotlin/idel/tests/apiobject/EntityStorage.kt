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


class EntityStorage(idelUrl  : String = Idel.URL) : AbstractObjectApi(User.instanceAdmin,idelUrl, "/configs/storage") {

    val log = KotlinLogging.logger {}


    private fun deleteEntity(entityType : String) {
        val response = delete("/$entityType","")


        if (response.statusCode() == HttpURLConnection.HTTP_OK) {
            log.info("Clear $entityType. ${response.body().toPrettyString()}")
        } else {
            log.warn("Something is wrong, ${response.body().toPrettyString()}")
            throw RuntimeException("Can't clear $entityType. ${response.body()}")
        }

    }

    fun clearAll() {
        clearIdeas()
        clearUsers()
        clearGroups()
        clearJoinRequests()
        clearGroupMembers()
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
        this.deleteEntity("joinRequest")
    }

    fun clearGroups() {
        this.deleteEntity("group")
    }

    fun clearGroupMembers() {
        this.deleteEntity("groupMember")
    }
}
