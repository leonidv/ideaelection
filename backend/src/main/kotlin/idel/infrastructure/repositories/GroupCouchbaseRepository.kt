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
        val NOT_DELETED_CONDITION: String = """grp.state != "${GroupState.DELETED}" """;

        fun generateBaseQueryPart(
            bucketName: String,
            type: String,
            additionalFields: String = "",
            ansiJoin: String = ""
        ): String {
            val templatedQuery = """
                    | SELECT OBJECT_CONCAT(grp, {
                    |    "membersCount" : (select raw count(*) from n as x where x._type="groupMember")[0],
                    |    "ideasCount" : (select raw count(*) from n as x where x._type="idea")[0]
                    | }) AS ie
                    | FROM `$bucketName` as grp --#ansiJoin
                    | INNER NEST `$bucketName` as n
                    | ON n.groupId == grp.id
                    | where grp._type = "$type" """.trimMargin()
            return templatedQuery
                .replaceFirst("--#ansiJoin", ansiJoin)
        }
    }

    private val baseQueryPart = generateBaseQueryPart(bucketName, type)


    override val log = KotlinLogging.logger {}

    override fun collection(): Collection = collection

    override fun load(id: String): Either<Exception, Group> {
        val baseQueryPart = "$baseQueryPart AND $NOT_DELETED_CONDITION AND grp.id=\$groupId "
        val params = JsonObject.create();
        params.put("groupId", id)

        val result = rawLoad(
            basePart = baseQueryPart,
            params = params,
            filterQueryParts = emptyList(),
            ordering = "",
            pagination = Repository.ONE_ELEMENT,
        )

        return extractFirstGroup(result, id)
    }


    override fun exists(id: String): Either<Exception, Boolean> {
        return load(id).map {true}
    }

    override fun loadByUser(
        userId: String,
        pagination: Repository.Pagination,
        ordering: GroupOrdering
    ): Either<Exception, List<Group>> {
        val selectPart = generateBaseQueryPart(
            bucketName = bucketName,
            type = type,
            ansiJoin = """${'\n'} join $bucketName as grpMember on (grp.id = grpMember.groupId) """
        )

        val filterParts = listOf(
            """grpMember._type = "groupMember" """,
            "grpMember.userId = \$userId",
            NOT_DELETED_CONDITION)
        val params = JsonObject.create();
        params.put("userId", userId)

        val orderingPart = "grp.${Repository.enumAsOrdering(ordering)}"

        return rawLoad(
            basePart = selectPart,
            filterQueryParts = filterParts,
            ordering = orderingPart,
            params, pagination
        )
    }


    override fun loadOnlyAvailable(
        pagination: Repository.Pagination,
        ordering: GroupOrdering
    ): Either<Exception, List<Group>> {
        val params = JsonObject.create()
        val orderingValue = "grp.${Repository.enumAsOrdering(ordering)}"

        val filterQueryParts = listOf(
            """grp.entryMode IN ["${GroupEntryMode.PUBLIC}","${GroupEntryMode.CLOSED}"]""",
            NOT_DELETED_CONDITION,
        )

        return rawLoad(
            basePart = baseQueryPart,
            filterQueryParts = filterQueryParts,
            ordering = orderingValue,
            params = params,
            pagination = pagination,
            useFulltextSearch = false
        )
    }

    override fun loadByJoiningKey(joiningKey: String): Either<Exception, Group> {
        val filters = listOf("grp.joiningKey = \$joiningKey", NOT_DELETED_CONDITION)
        val params = JsonObject.create().put("joiningKey", joiningKey)

        val queryResult = super.rawLoad(
            basePart = baseQueryPart,
            filterQueryParts = filters,
            ordering = "",
            params = params,
            pagination = Repository.ONE_ELEMENT,
            useFulltextSearch = false
        )

        return extractFirstGroup(queryResult, "joiningKey=$joiningKey")

    }

    private fun extractFirstGroup(
        queryResult: Either<Exception, List<Group>>,
        notFoundMessage: String
    ) = when (queryResult) {
        is Either.Left -> queryResult
        is Either.Right -> {
            if (queryResult.b.isEmpty()) {
                Either.left(EntityNotFound(type, notFoundMessage))
            } else {
                Either.right(queryResult.b[0])
            }
        }
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
