package idel.infrastructure.repositories

import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Collection
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CouchbaseConfig {

    private val log = LoggerFactory.getLogger(CouchbaseConfig::class.java)

    @Bean("couchbaseCluster") fun cluster(props : CouchbaseProperties) : Cluster {
        val cluster = Cluster.connect(props.host, props.username, props.password)
        log.info("Connect to couchbase on host = [${props.host}] as user = [${props.username}]")
        return cluster
    }

    @Bean("couchbaseCollection")
    fun collection(cluster: Cluster, props: CouchbaseProperties) : Collection {
        val bucket = cluster.bucket(props.bucket)
        val collection = bucket.defaultCollection()
        log.info("Couchbase bucket is [${props.bucket}]")
        return collection
    }
}