package ideael.api

import ideael.infrastructure.repositories.CouchbaseProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import javax.annotation.PostConstruct

/**
 * Share config of application.
 * Passwords shares only in test mode (see
 */
@RestController
@RequestMapping("/configs")
class ConfigController(
    ) {
    private val log = LoggerFactory.getLogger(ConfigController::class.java)

    @Value("\${security.configapi.exposepassword}")
    private var exposePassword = false

    @Autowired
    lateinit var params : CouchbaseProperties

    @PostConstruct
    fun postInit() {
        if (exposePassword) {
            log.warn("Expose password! Don't use in production")
        }
    }

    @GetMapping("/couchbase")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    fun couchbase() : CouchbaseProperties {
       return if (exposePassword) {
           params
       } else {
           params.copy(password = "")
       }
    }
}