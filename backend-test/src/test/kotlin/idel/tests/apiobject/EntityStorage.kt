package idel.tests.apiobject

import idel.tests.Idel
import mu.KotlinLogging
import java.net.HttpURLConnection


class EntityStorage(idelUrl: String = Idel.URL) : AbstractObjectApi(User.instanceAdmin, idelUrl, "configs/storage") {

    val log = KotlinLogging.logger {}


    private fun deleteEntity(entityType: String) {
        val response = delete("/$entityType", "")


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
        clearInvites()
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
    fun clearInvites() {
        this.deleteEntity("invite")
    }
}
