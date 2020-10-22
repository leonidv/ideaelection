package idel.infrastructure.repositories

import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Collection
import idel.domain.GroupMembership
import idel.domain.GroupMembershipRepository
import mu.KotlinLogging


class GroupMembershipCouchbaseRepository(cluster : Cluster, collection: Collection) :
    AbstractTypedCouchbaseRepository<GroupMembership>(cluster, collection, type = "groupmembership", GroupMembership::class.java),
    GroupMembershipRepository {
    override val log = KotlinLogging.logger {}

    override fun add(membership: GroupMembership) {
        collection.insert(membership.id, membership, insertOptions())
    }

}