package idel.api

import com.couchbase.client.java.Cluster
import idel.infrastructure.repositories.CouchbaseProperties
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.annotation.PostConstruct

/**
 * Share config of application.
 * Passwords shares only in test mode (see
 */
@RestController
@RequestMapping("/storage")
class StorageController {
    private val log = KotlinLogging.logger {}

    @Value("\${testmode}")
    private var testMode = false

    @Autowired
    lateinit var params: CouchbaseProperties

    @Autowired
    lateinit var cluster: Cluster

    val nonExistsType = UUID.randomUUID().toString()

    @PostConstruct
    fun postInit() {
        if (testMode) {
            log.warn("Test mode! Don't use in production - you can loose all your data!")
        }
    }


    @DeleteMapping("/{type}")
    fun deleteEntities(@PathVariable type: String): ResponseEntity<DataOrError<String>> {
        return deleteWhere("""_type = "$type" """)
    }

    private fun deleteWhere(condition: String): ResponseEntity<DataOrError<String>> {
        return if (testMode) {
            try {
                val result = cluster.query("""delete from `${params.bucket}` where $condition returning *""")
                val deleted = result.rowsAsObject().size
                DataOrError.ok("Deleted ${deleted} documents")
            } catch (e: Exception) {
                log.error("can't delete entities", e)
                DataOrError.internal(e, log)
            }
        } else {
            DataOrError.notFound("not found")
        }
    }

    @DeleteMapping("/flush")
    fun flush(): EntityOrError<String> {
        return deleteWhere("""_type != "$nonExistsType" """)
    }
}