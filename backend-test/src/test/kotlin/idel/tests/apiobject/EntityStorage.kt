package idel.tests.apiobject

import idel.tests.TestConfig
import mu.KotlinLogging
import java.net.HttpURLConnection


class EntityStorage(idelUrl: String = TestConfig.backendUrl) : AbstractObjectApi(User.instanceAdmin, idelUrl, "storage") {

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
        deleteEntity("flush")
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
    fun clearUserSettings() {
        this.deleteEntity("userSettings")
    }
}
