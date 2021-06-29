package idel.api

import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Collection
import com.couchbase.client.java.query.QueryOptions
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@RestController
@RequestMapping("/probes")
class ProbesController(
    val cluster: Cluster,
    val collection: Collection
) {



    @GetMapping("/readiness")
    fun getBuildInfo() : String {
        val queryOptions = QueryOptions.queryOptions()
        queryOptions.timeout(Duration.ofSeconds(1))
        cluster.query("select 1;", queryOptions).rowsAsObject()
        return "OK"
    }
}