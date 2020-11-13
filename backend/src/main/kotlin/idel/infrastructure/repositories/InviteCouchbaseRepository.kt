package idel.infrastructure.repositories

import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Collection
import idel.domain.Invite
import idel.domain.InviteRepository
import mu.KLogger
import mu.KotlinLogging

class InviteCouchbaseRepository(cluster : Cluster, collection : Collection) :
        AbstractTypedCouchbaseRepository<Invite>(cluster, collection, type = "invite", Invite::class.java), InviteRepository {
    override val log = KotlinLogging.logger {}

}