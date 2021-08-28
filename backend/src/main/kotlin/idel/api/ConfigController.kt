package idel.api

import com.couchbase.client.java.Cluster
import idel.infrastructure.repositories.CouchbaseProperties
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.annotation.PostConstruct

/**
 * Share config of application.
 * Passwords shares only in test mode (see
 */
@RestController
@RequestMapping("/configs")
class ConfigController {
    private val log = KotlinLogging.logger {}

    @Value("\${testmode}")
    private var testMode = false

    @Autowired
    lateinit var params : CouchbaseProperties

    @Autowired
    lateinit var cluster: Cluster

    @PostConstruct
    fun postInit() {
        if (testMode) {
            log.warn("Test mode! Don't use in production - you can loose all your data!")
        }
    }

    @GetMapping("/couchbase")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    fun couchbase() : CouchbaseProperties {
       return if (testMode) {
           params
       } else {
           params.copy(password = "")
       }
    }

    @DeleteMapping("/storage/{type}")
    fun  deleteEntities(@PathVariable type : String) : ResponseEntity<DataOrError<String>> {
       return if (testMode) {
           try {
              val result = cluster.query("""delete from `${params.bucket}` where _type = "${type}" returning *""")
              val deleted = result.rowsAsObject().size
               DataOrError.ok("Deleted ${deleted} documents")
           } catch (e : Exception) {
               log.error("can't delete entities", e)
               DataOrError.internal(e, log)
           }
        } else {
            DataOrError.notFound("not found")
        }
    }
}