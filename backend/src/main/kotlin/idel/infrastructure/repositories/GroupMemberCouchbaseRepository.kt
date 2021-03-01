package idel.infrastructure.repositories

import arrow.core.Either
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Collection
import com.couchbase.client.java.json.JsonObject
import idel.domain.*
import mu.KotlinLogging

class GroupMemberCouchbaseRepository(cluster: Cluster, collection: Collection) :
        AbstractTypedCouchbaseRepository<GroupMember>(cluster, collection, type = "groupMember", typedClass = GroupMember::class.java),
        GroupMemberRepository {

    override val log = KotlinLogging.logger {}

    /**
     * Very effective performance based on key calculation and [Collection.exists].
     */
    override fun isMember(groupId: String, userId: String): Either<Exception, Boolean> {
        return safelyKeyOperation("[groupId = $groupId  userId =$userId]") {
            val groupMemberId = GroupMember.calculateId(groupId, userId)
            val result = collection.exists(groupMemberId)
//            переписать, т.к. exists не работает. Или обновить драйвер сначала!
//            collection.get(groupMemberId, getOptions())
            result.exists()
        }
    }

    /**
     * Very effective performance based on key calculation and [Collection.get].
     */
    override fun load(groupId: String, userId: String): Either<Exception, GroupMember> {
        return safelyKeyOperation("[groupId = $groupId, userId = $userId]") {
            val groupMemberId = GroupMember.calculateId(groupId, userId)
            val result = collection.get(groupMemberId, getOptions())
            result.contentAs(typedClass)
        }
    }

    override fun removeFromGroup(groupId: String, userId: String): Either<Exception, Unit> {
        return safelyKeyOperation("[groupId = $groupId, userId = $userId") {
            val groupMemberId = GroupMember.calculateId(groupId, userId)
            collection.remove(groupMemberId)
        }
    }
}