package idel.infrastructure.repositories


import arrow.core.Either
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Collection
import com.couchbase.client.java.json.JsonObject
import com.fasterxml.jackson.databind.node.ObjectNode
import idel.domain.*
import mu.KotlinLogging

class GroupCouchbaseRepository(
    cluster: Cluster,
    collection: Collection
) : AbstractTypedCouchbaseRepository<Group>(
    cluster,
    collection,
    type = TYPE,
    Group::class.java,
    ignoredFields = setOf("membersCount", "ideasCount")
), GroupRepository {

    companion object {
        const val TYPE = "group"

        private val NOT_DELETED_CONDITION: String = """grp.state != "${GroupState.DELETED}" """;

        private const val GROUP_MEMBER_TYPE = GroupMemberCouchbaseRepository.TYPE

        private const val IDEA_TYPE = IdeaCouchbaseRepository.TYPE
    }

    private val baseQueryPart = """
                    | SELECT OBJECT_CONCAT(grp, {
                    |    "membersCount" : (select raw count(*) from n as x where x._type="$GROUP_MEMBER_TYPE")[0],
                    |    "ideasCount" : (select raw count(*) from n as x where x._type="$IDEA_TYPE")[0]
                    | }) AS ie
                    | FROM `$bucketName` as grp
                    | INNER NEST `$bucketName` as n ON KEY n.groupId FOR grp
                    | where grp._type = "$type" """.trimMargin()

    /**
     * Subquery which loads groupId from groupsMember which are belongs to a user.
     * You MUST add a param ```userId```.
     */
    private val subQueryUserGroupMember = "(SELECT RAW gm.groupId from `$bucketName` gm " +
            """where gm._type="$GROUP_MEMBER_TYPE" and gm.userId=${'$'}userId)"""

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
        partOfName: String?,
        pagination: Repository.Pagination,
        ordering: GroupOrdering
    ): Either<Exception, List<Group>> {
        val filters = mutableListOf(
            "grp.id in $subQueryUserGroupMember",
            NOT_DELETED_CONDITION
        )
        val params = JsonObject.create();
        params.put("userId", userId)


        if (!partOfName.isNullOrBlank()) {
            filters += "CONTAINS(LOWER(grp.name), LOWER(\$partOfName))"
            params.put("partOfName", partOfName)
        }

        val orderingPart = "grp.${Repository.enumAsOrdering(ordering)}"

        return rawLoad(
            basePart = baseQueryPart,
            filterQueryParts = filters,
            ordering = orderingPart,
            params = params,
            pagination = pagination,
            useFulltextSearch = false
        )
    }


    override fun loadOnlyAvailable(
        userId: String,
        userDomain: String,
        partOfName: String?,
        pagination: Repository.Pagination,
        ordering: GroupOrdering
    ): Either<Exception, List<Group>> {
        val orderingValue = "grp.${Repository.enumAsOrdering(ordering)}"

        val entryModeCondition = """grp.entryMode IN ["${GroupEntryMode.PUBLIC}","${GroupEntryMode.CLOSED}"]"""

        val domainRestrictionCondition = "( (ARRAY_LENGTH(grp.domainRestrictions) = 0) " +
                "OR ANY dr IN grp.domainRestrictions SATISFIES lower(dr) = lower(\$userDomain) END )"

        val userIsNotInGroupCondition = "grp.id not in $subQueryUserGroupMember"

        val filters = mutableListOf(
            entryModeCondition,
            domainRestrictionCondition,
            userIsNotInGroupCondition,
            NOT_DELETED_CONDITION
        )
        val params = JsonObject.create()
        params.put("userDomain", userDomain)
        params.put("userId", userId)

        if (!partOfName.isNullOrBlank()) {
            filters += "CONTAINS(LOWER(grp.name), LOWER(\$partOfName))"
            params.put("partOfName", partOfName)
        }

        return rawLoad(
            basePart = baseQueryPart,
            filterQueryParts = filters,
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
            if (queryResult.value.isEmpty()) {
                Either.Left(EntityNotFound(type, notFoundMessage))
            } else {
                Either.Right(queryResult.value[0])
            }
        }
    }

    override fun entityToJsonObject(entity: Group): ObjectNode = jsonSerializer.serializeToObjectNode(entity)
}
