package idel.infrastructure.repositories


import arrow.core.Either
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Collection
import com.couchbase.client.java.codec.JacksonJsonSerializer
import com.couchbase.client.java.json.JsonObject
import com.couchbase.client.java.query.QueryOptions
import com.fasterxml.jackson.databind.node.ObjectNode
import idel.domain.Repository
import idel.domain.*
import mu.KotlinLogging

class GroupCouchbaseRepository(
    cluster: Cluster,
    collection: Collection
) : AbstractTypedCouchbaseRepository<Group>(cluster, collection, type = "group", Group::class.java), GroupRepository {

    companion object {
        val NOT_DELETED_CONDITION: String = """state != "${GroupState.DELETED}" """;
    }

    override val log = KotlinLogging.logger {}

    override fun collection(): Collection = collection

    override fun load(id: String): Either<Exception, Group> {
        val result = super.load(id)

        return if (result is Either.Right && result.b.isDeleted()) {
            Either.left(EntityNotFound(type, id))
        } else {
            result
        }
    }

    override fun exists(id: String): Either<Exception, Boolean> {
        return load(id).map {!it.isDeleted()}
    }

    override fun loadByUser(
        userId: String,
        pagination: Repository.Pagination,
        ordering: GroupOrdering
    ): Either<Exception, List<Group>> {
        val selectPart = """
                SELECT ie  from `$bucketName` gm JOIN `$bucketName` ie  ON KEYS gm.groupId
                WHERE gm._type = "groupMember" and ie._type ="$type" and ie.$NOT_DELETED_CONDITION   
            """.trimIndent()

        val filterParts = listOf("gm.userId = \$userId")
        val params = JsonObject.create();
        params.put("userId", userId)

        val orderingPart = "ie.${Repository.enumAsOrdering(ordering)}"

        return rawLoad(
            basePart = selectPart,
            filterQueryParts = filterParts,
            orderingPart = orderingPart,
            params, pagination
        )
    }


    override fun loadOnlyAvailable(
        pagination: Repository.Pagination,
        ordering: GroupOrdering
    ): Either<Exception, List<Group>> {
        val params = JsonObject.create()

        val orderingValue = Repository.enumAsOrdering(ordering)

        var filterQueryParts = listOf(
            """entryMode IN ["${GroupEntryMode.PUBLIC}","${GroupEntryMode.CLOSED}"]""",
            NOT_DELETED_CONDITION,
        )

        return super.load(filterQueryParts, orderingValue, params, pagination, useFulltextSearch = false)
    }

    fun addMember(groupId: String, member: GroupMember): Either<Exception, String> {
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
            Either.right("OK")

        } catch (e: Exception) {
            Either.left(e)
        }
    }

    fun removeMember(groupId: String, userId: String): Either<Exception, String> {
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


            val params = JsonObject.create();
            params.put("groupId", groupId)
            params.put("userId", userId)


            val options = QueryOptions
                .queryOptions()
                .parameters(params)

            log.trace {"query: [$query], params: [$params]"}

            cluster.query(query, options)
            Either.right("ok")

        } catch (e: Exception) {
            Either.left(e)
        }

    }

    override fun entityToJsonObject(entity: Group): ObjectNode = jsonSerializer.serializeToObjectNode(entity)
}
