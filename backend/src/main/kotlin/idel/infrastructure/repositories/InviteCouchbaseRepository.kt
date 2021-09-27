package idel.infrastructure.repositories

import arrow.core.Either
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Collection
import com.couchbase.client.java.json.JsonObject
import idel.domain.GroupMembershipRequestOrdering
import idel.domain.Invite
import idel.domain.InviteRepository
import idel.domain.Repository
import mu.KotlinLogging

class InviteCouchbaseRepository(cluster : Cluster, collection : Collection) :
        AbstractTypedCouchbaseRepository<Invite>(cluster, collection, type = "invite", Invite::class.java), InviteRepository {
    override val log = KotlinLogging.logger {}

    override fun loadByUser(
        userId: String,
        order: GroupMembershipRequestOrdering,
        pagination: Repository.Pagination
    ): Either<Exception, List<Invite>> {
        val params = JsonObject.create()

        val filterQueryParts = listOf("userId = \$userId")
        params.put("userId", userId)

        return super.load(
            filterQueryParts,
            Repository.enumAsOrdering(order),
            params,
            pagination,
            useFulltextSearch = false
        )
    }

    override fun loadByGroup(
        groupId: String,
        order: GroupMembershipRequestOrdering,
        pagination: Repository.Pagination
    ) : Either<Exception, List<Invite>> {
        val params = JsonObject.create()
        val filterQueryParts = listOf("groupId = \$groupId")
        params.put("groupId", groupId)

        return super.load(
            filterQueryParts,
            Repository.enumAsOrdering(order),
            params,
            pagination,
            useFulltextSearch = false
        )
    }
}