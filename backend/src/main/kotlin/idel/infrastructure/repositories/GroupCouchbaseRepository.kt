package idel.infrastructure.repositories

import arrow.core.Either
import arrow.core.Option
import arrow.core.Some
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Collection
import com.couchbase.client.java.json.JsonObject
import idel.domain.*
import mu.KotlinLogging
import org.springframework.stereotype.Repository

class GroupCouchbaseRepository(
        cluster: Cluster,
        collection: Collection
) : AbstractTypedCouchbaseRepository<Group>(cluster, collection, type = "group", Group::class.java), GroupRepository {

     override val log = KotlinLogging.logger {}

    override fun add(group: Group) {
        collection.insert(group.id, group, insertOptions())
    }


    override fun load(first: Int, last: Int, sorting: GroupSorting, filtering: GroupFiltering): List<Group> {
        val params = JsonObject.create()

        val ordering = when (sorting) {
            GroupSorting.CTIME_ASC -> "ctime asc"
            GroupSorting.CTIME_DESC -> "ctime desc"
        }

        var filterQueryParts = mutableListOf<String>()


        if(filtering.onlyAvailable) {
            filterQueryParts.add("""entryMode IN ["${GroupEntryMode.PUBLIC}","${GroupEntryMode.CLOSED}"]""")
        }

        return super.load(filterQueryParts, ordering, params, first, last)

    }


    override fun replace(group: Group) {
        TODO("Not yet implemented")
    }
}