package idel.infrastructure.repositories

import arrow.core.Option
import arrow.core.Some
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Collection
import com.couchbase.client.java.json.JsonObject
import idel.domain.*
import mu.KotlinLogging
import org.springframework.stereotype.Repository

@Repository
class GroupCouchbaseRepository(
        cluster: Cluster,
        collection: Collection
) : AbstractTypedCouchbaseRepository<Group>(cluster, collection, type = "group", Group::class.java), GroupRepository {

    private val log = KotlinLogging.logger {}

    override fun add(group: Group) {
        collection.insert(group.id, group, insertOptions())
    }


    override fun load(first: Int, last: Int, sorting: GroupSorting, filtering: GroupFiltering): List<Group> {
        val params = JsonObject.create()

        val ordering = when (sorting) {
            GroupSorting.CTIME_ASC -> "ctime asc"
            GroupSorting.CTIME_DESC -> "ctime desc"
        }

        var filterQueryParts = emptyList<String>()

        if (filtering.availableForJoiningEmail is Some) {
            params.put("userEmail", filtering.availableForJoiningEmail.t)
            val queryPart = """
                ANY userRestriction IN usersRestrictions SATISFIES 
                        REGEXP_LIKE("${'$'}userEmail",userRestriction) 
                    END
            """.trimIndent()
            filterQueryParts = filterQueryParts + queryPart
        }

        return super.load(filterQueryParts, ordering, params, first, last)

    }

    override fun findAvailableForJoining(user: User): List<Group> {
        val userEmailParam = "userEmail"
        val query = """
            select * from `ideaelection` as ie where _type = "group" 
                and ANY userRestriction IN usersRestrictions SATISFIES 
                        REGEXP_LIKE("${'$'}$userEmailParam",userRestriction) 
                    END
        """.trimIndent()

        val params = JsonObject.create(1);
        params.put(userEmailParam, user.email)
        val queryOptions = queryOptions(params)
        log.trace {"query: [$query], params: [$params]"}

        val q = cluster.query(query, queryOptions)
        return q.rowsAs(typedClass)
    }

    override fun load(id: String): Option<Group> {
        TODO("Not yet implemented")
    }

    override fun replace(group: Group) {
        TODO("Not yet implemented")
    }
}