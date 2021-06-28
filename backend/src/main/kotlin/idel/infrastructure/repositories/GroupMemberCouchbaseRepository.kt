package idel.infrastructure.repositories

import arrow.core.Either
import arrow.core.Option
import arrow.core.Some
import arrow.core.extensions.either.monad.flatten
import arrow.core.extensions.fx
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Collection
import com.couchbase.client.java.json.JsonObject
import com.couchbase.transactions.AttemptContext
import com.fasterxml.jackson.databind.node.ObjectNode
import idel.domain.*
import mu.KotlinLogging
import java.time.LocalDateTime

private data class GroupMemberLink(
    override val id: String,
    val ctime: LocalDateTime,
    val groupId: String,
    val userId: String,
    val roleInGroup: GroupMemberRole
) : Identifiable {
    companion object {

        fun of(groupMember: GroupMember): GroupMemberLink {
            require(
                groupMember.id == GroupMember.calculateId(
                    groupId = groupMember.groupId,
                    userId = groupMember.userId
                )
            )

            return GroupMemberLink(
                groupId = groupMember.groupId,
                userId = groupMember.userId,
                id = groupMember.id,
                roleInGroup = groupMember.roleInGroup,
                ctime = LocalDateTime.now()
            )
        }
    }
}

class GroupMemberCouchbaseRepository(cluster: Cluster, collection: Collection) :
    AbstractTypedCouchbaseRepository<GroupMember>(
        cluster,
        collection,
        type = "groupMember",
        typedClass = GroupMember::class.java
    ),
    GroupMemberRepository, CouchbaseTransactionBaseRepository<GroupMember> {

    private val userType = UserCouchbaseRepository.TYPE;

    private val groupMemberLinkRepository =
        object : AbstractTypedCouchbaseRepository<GroupMemberLink>(
            cluster,
            collection,
            type,
            typedClass = GroupMemberLink::class.java
        ), CouchbaseTransactionBaseRepository<GroupMemberLink> {
            override val log = KotlinLogging.logger {}

            override fun collection() = this.collection

            override fun entityToJsonObject(entity: GroupMemberLink) = this.jsonSerializer.serializeToObjectNode(entity)
        }

    override val log = KotlinLogging.logger {}

    override fun collection(): Collection = collection

    override fun add(groupMember: GroupMember): Either<Exception, GroupMember> {
        val groupMemberLink = GroupMemberLink.of(groupMember)
        return groupMemberLinkRepository.add(groupMemberLink).map {groupMember}
    }

    /**
     * Very effective performance based on key calculation and [Collection.exists].
     */
    override fun isMember(groupId: String, userId: String): Either<Exception, Boolean> {
        return safelyKeyOperation("[groupId = $groupId  userId =$userId]") {
            val groupMemberId = GroupMember.calculateId(groupId, userId)
            val result = collection.exists(groupMemberId)
            result.exists()
        }
    }


    override fun load(groupId: String, userId: String): Either<Exception, GroupMember> {
        return safely {
            val groupMemberId = GroupMember.calculateId(groupId, userId)
            val queryString = """SELECT  OBJECT_CONCAT(u, gm) as ie 
                FROM `$bucketName` gm JOIN `$bucketName` u ON KEYS gm.userId
                WHERE gm._type = "$type" AND gm.id = ${'$'}groupMemberId 
                AND u._type = "$userType"
            """.trimMargin()

            val params = JsonObject.create()
            params.put("groupMemberId", groupMemberId)

            val queryOptions = queryOptions(params)

            traceRawQuery(queryString, params)

            val groupMembers = cluster.query(queryString, queryOptions).rowsAs(typedClass)

            if (groupMembers.isEmpty()) {
                Either.left(EntityNotFound(entityType = "GroupMember", id = groupMemberId))
            } else {
                Either.right(groupMembers.first()!!)
            }
        }
    }

    override fun removeFromGroup(groupId: String, userId: String): Either<Exception, Unit> {
        return safelyKeyOperation("[groupId = $groupId, userId = $userId") {
            val groupMemberId = GroupMember.calculateId(groupId, userId)
            collection.remove(groupMemberId)
        }
    }


    override fun loadByGroup(
        groupId: String,
        pagination: Repository.Pagination,
        usernameFilter: Option<String>,
        roleFilter: Option<GroupMemberRole>
    ): Either<Exception, List<GroupMember>> {
        val selectPart = """ SELECT  OBJECT_CONCAT(u, gm) as ie 
             FROM `$bucketName` gm JOIN `$bucketName` u ON KEYS gm.userId
             WHERE gm._type = "$type" and u._type = "$userType"
        """.trimIndent()

        val filterParts = mutableListOf("gm.groupId =  \$groupId")
        val params = JsonObject.create()
        params.put("groupId", groupId)

        if (usernameFilter is Some) {
            filterParts.add("""CONTAINS( UPPER(u.displayName), UPPER(${'$'}username) )""")
            params.put("username", usernameFilter.t)
        }

        if (roleFilter is Some) {
            filterParts.add("gm.roleInGroup = \$roleInGroup")
            params.put("roleInGroup", roleFilter.t.name)
        }

        val orderingPart = "gm.ctime DESC"

        return rawLoad(
            basePart = selectPart,
            filterQueryParts = filterParts,
            ordering = orderingPart,
            params = params,
            pagination = pagination
        )
    }

    override fun entityToJsonObject(entity: GroupMember): ObjectNode = jsonSerializer.serializeToObjectNode(entity)

    override fun add(groupMember: GroupMember, ctx: AttemptContext): Either<Exception, Unit> {
        return groupMemberLinkRepository.add(GroupMemberLink.of(groupMember), ctx)
    }

    override fun changeRole(groupMember: GroupMember): Either<Exception, GroupMember> {
        val memberId = groupMember.id
        return Either.fx<Exception, Either<Exception, GroupMember>> {
            groupMemberLinkRepository.possibleMutate(memberId) {link ->
                Either.right(link.copy(roleInGroup = groupMember.roleInGroup))
            }.bind()

            load(groupMember.groupId, groupMember.userId)
        }.flatten()
    }
}