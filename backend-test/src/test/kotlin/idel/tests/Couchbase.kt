package idel.tests

import com.couchbase.client.java.Collection
import com.couchbase.client.java.Bucket
import com.couchbase.client.java.Cluster
import com.typesafe.config.ConfigFactory
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When

class Couchbase(host: String, username: String, password: String, private val bucketName: String) {
    private val cluster: Cluster = Cluster.connect(host, username, password)
    private val bucket: Bucket = cluster.bucket(bucketName)
    private val collection: Collection = bucket.defaultCollection()

    companion object {
        private fun init(ideaelUrl: String): Couchbase {
            val r = Given {
                auth().preemptive().basic("admin", "admin")
            } When {
                get("$ideaelUrl/configs/couchbase")
            } Then {
                statusCode(200)
            }

            val json = r.extract().body().jsonPath()
            val host = json.getString("host")
            val username = json.getString("username")
            val password = json.getString("password")
            val bucketName = json.getString("bucket")

            return Couchbase(host, username, password, bucketName)
        }

        /**
         * Load settings of couchbase service from IdeaElection application.
         * Application MUST be in test mode, see testing.md in backend folder.
         */
        val Helper = init(ConfigFactory.load().getString("ideael.url"))
    }

    init {
          println("Connect to couchbase on host = [${host}] as user = [${username}], bucket = [${bucket}]")
    }


    /**
     * Remove all documents from bucket.
     *
     * Use direct connection to couchbase server.
     */
    fun clearIdeas() {
        val result = cluster.query("""select id from `$bucketName` where _type = "idea" """)
        result.rowsAsObject().forEach {
            val id = it.getString("id")
            println("delete document id = [$id]")
            collection.remove(id)
        }
    }
}

fun main() {
    Couchbase.Helper.clearIdeas()
}