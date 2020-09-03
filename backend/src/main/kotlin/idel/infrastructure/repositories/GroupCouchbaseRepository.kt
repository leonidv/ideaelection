package idel.infrastructure.repositories

import arrow.core.Option
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Collection
import idel.domain.Group
import idel.domain.GroupRepository
import org.springframework.stereotype.Repository

@Repository
class GroupCouchbaseRepository(
        cluster: Cluster,
        collection: Collection
) : AbstractTypedCouchbaseRepository<Group>(cluster, collection, type = "group", Group::class.java), GroupRepository {

    override fun add(group: Group) {
        collection.insert(group.id, group, insertOptions())
    }

    override fun load(id: String): Option<Group> {
        TODO("Not yet implemented")
    }

    override fun update(id: String, group: Group) {
        TODO("Not yet implemented")
    }
}