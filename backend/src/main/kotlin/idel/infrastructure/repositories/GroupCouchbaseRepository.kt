package idel.infrastructure.repositories


import arrow.core.Either
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Collection
import com.couchbase.client.java.codec.JacksonJsonSerializer
import com.couchbase.client.java.json.JsonObject
import com.couchbase.client.java.query.QueryOptions
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import idel.domain.Repository
import idel.domain.*
import mu.KotlinLogging

class GroupCouchbaseRepository(
        cluster: Cluster,
        collection: Collection
) : AbstractTypedCouchbaseRepository<Group>(cluster, collection, type = "group", Group::class.java), GroupRepository {

    override val log = KotlinLogging.logger {}

    override fun load(first: Int, last: Int, sorting: GroupSorting, filtering: GroupFiltering): Either<Exception, List<Group>> {
        TODO("not yet implemented")
    }

    class EntryModeRow(val entryMode: GroupEntryMode)

    override fun loadEntryMode(id: String): Either<Exception, GroupEntryMode> {
        return try {
            val query = "select entryMode from `$bucketName` where id = \$id and _type=\"group\""
            val params = JsonObject.create().put("id", id)
            val serializer = JacksonJsonSerializer.create(jacksonObjectMapper())
            val options = QueryOptions.queryOptions().serializer(serializer).parameters(params)

            val result = cluster.query(query, options).rowsAs(EntryModeRow::class.java)
            if (result.isEmpty()) {
                Either.left(EntityNotFound(type, id))
            } else {
                Either.right(result.last().entryMode)
            }
        } catch (e: Exception) {
            Either.left(e)
        }
    }

    override fun replace(group: Group) {
        TODO("Not yet implemented")
    }

    override fun loadOnlyAvailable(pagination: Repository.Pagination, sorting: GroupSorting): Either<Exception, List<Group>> {
        val params = JsonObject.create()

        val ordering = when (sorting) {
            GroupSorting.CTIME_ASC -> "ctime asc"
            GroupSorting.CTIME_DESC -> "ctime desc"
        }

        var filterQueryParts = listOf(
                """entryMode IN ["${GroupEntryMode.PUBLIC}","${GroupEntryMode.CLOSED}"]"""
        )

        return super.load(filterQueryParts, ordering, params, pagination)
    }

    override fun addMember(groupId: String, member: GroupMember): Either<Exception, Unit> {
        return try {
            val pMember = "\$member"
            val query = """
                    UPDATE `$bucketName`
                    SET members = ARRAY_APPEND(members,$pMember)
                    WHERE _type="$type"
                          AND id=${'$'}id 
                          AND NOT ANY member IN members SATISFIES member.id = ${'$'}memberId END 
                    RETURNING id
        """.trimIndent()

            val mapper = initMapper()
            val jsonStrMember = mapper.writeValueAsString(member)
            val jsonMember = JsonObject.fromJson(jsonStrMember) // ugly hack :(

            val params = JsonObject.create();
            params.put("member", jsonMember)
            params.put("id", groupId)
            params.put("memberId", member.id)

            val serializer = JacksonJsonSerializer.create(mapper)
            val options = QueryOptions
                .queryOptions()
                .serializer(serializer)
                .parameters(params)

            log.trace {"query: [$query], params: [$params]"}

            cluster.query(query, options)
            Either.right(Unit)

        } catch (e: Exception) {
            Either.left(e)
        }
    }

    override fun removeMember(groupId: String, userId: String): Either<Exception, Unit> {
        return try {
            val pGroupId = "\$groupId"
            val pUserId = "\$userId"
            val query = """UPDATE `$bucketName`
                    SET members = ARRAY_REMOVE(members, (
                        SELECT RAW FIRST member FOR member IN members WHEN member.id = $pUserId END AS member
                        FROM `$bucketName` ie_member
                        WHERE id = $pGroupId
                            AND _type="$type")[0] )
                    WHERE _type="$type" 
                        AND id=$pGroupId 
                        AND ANY member IN members SATISFIES member.id = $pUserId END
                    RETURNING *""".trimIndent()

//            val mapper = initMapper()
//            val jsonStrMember = mapper.writeValueAsString(userId)
//            val jsonMember = JsonObject.fromJson(jsonStrMember) // ugly hack :(

            val params = JsonObject.create();
            params.put("groupId", groupId)
            params.put("userId", userId)

//            val serializer = JacksonJsonSerializer.create(mapper)
            val options = QueryOptions
                .queryOptions()
//                .serializer(serializer)
                .parameters(params)

            log.trace {"query: [$query], params: [$params]"}

            cluster.query(query, options)
            Either.right(Unit)

        } catch (e: Exception) {
            Either.left(e)
        }

    }
}