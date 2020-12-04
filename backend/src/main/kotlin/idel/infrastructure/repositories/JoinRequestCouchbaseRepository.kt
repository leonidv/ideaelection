package idel.infrastructure.repositories

import arrow.core.Either
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Collection
import com.couchbase.client.java.json.JsonObject
import idel.domain.Repository
import idel.domain.*
import mu.KotlinLogging


class JoinRequestCouchbaseRepository(
        cluster: Cluster,
        collection: Collection
) : AbstractTypedCouchbaseRepository<JoinRequest>(cluster, collection, type = "joinRequest", JoinRequest::class.java), JoinRequestRepository {

    override val log = KotlinLogging.logger {}

    override fun loadByUser(userId: UserId, ordering: GroupMembershipRequestOrdering, pagination: Repository.Pagination): Either<Exception, List<JoinRequest>> {
        val params = JsonObject.create()

        val filterQueryParts = listOf("userId = \$userId")
        params.put("userId", userId)

        return super.load(filterQueryParts, Repository.enumAsOrdering(ordering), params, pagination)
    }

    override fun loadByGroup(groupId: String, ordering: GroupMembershipRequestOrdering, pagination: Repository.Pagination): Either<Exception, List<JoinRequest>> {
        val params = JsonObject.create()

        val filterQueryParts = listOf("groupId = \$groupId")
        params.put("groupId", groupId)

        return super.load(filterQueryParts, Repository.enumAsOrdering(ordering), params, pagination)

    }
}