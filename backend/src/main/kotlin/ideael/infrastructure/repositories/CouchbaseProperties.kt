package ideael.infrastructure.repositories

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding


@ConstructorBinding
@ConfigurationProperties("couchbase")
data class CouchbaseProperties(
    val host : String,
    val username : String,
    val password : String,
    val bucket : String ) {
}