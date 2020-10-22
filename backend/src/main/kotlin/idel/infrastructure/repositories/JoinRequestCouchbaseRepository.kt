package idel.infrastructure.repositories

import arrow.core.Either
import arrow.core.Option
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Collection
import idel.domain.*
import mu.KotlinLogging
import org.springframework.stereotype.Repository


class JoinRequestCouchbaseRepository(
        cluster: Cluster,
        collection: Collection
) : AbstractTypedCouchbaseRepository<JoinRequest>(cluster, collection, type = "joinrequest", JoinRequest::class.java), JoinRequestRepository {

    override val log = KotlinLogging.logger {}

    override fun add(request: JoinRequest) {
        collection.insert(request.id,request,insertOptions())
    }

    override fun replace(invite: JoinRequest) {
        TODO("Not yet implemented")
    }

}