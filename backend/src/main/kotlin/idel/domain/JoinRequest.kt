package idel.domain

import arrow.core.Either
import java.time.LocalDateTime

/**
 * User creates a join request when want to join the group.
 */
class JoinRequest(val groupId: String,
                  val userId: UserId,
                  val message: String,
                  val status: AcceptStatus,
                  val ctime: LocalDateTime = LocalDateTime.now(),
                  val mtime: LocalDateTime = ctime
) : Identifiable {

    override val id = generateId(groupId = groupId, userId = userId)

    companion object {
        private fun generateId(groupId : String, userId : String) = compositeId(key = "jnreq", groupId, userId)

        fun id(user : User, group : Group) = Companion.generateId(groupId = group.id, userId = user.id)

        fun createUnresolved(groupId: String, userId: UserId, message: String) =
            JoinRequest(
                groupId = groupId,
                userId = userId,
                message = message,
                status = AcceptStatus.UNRESOLVED
            )

        fun createApproved(groupId: String, userId: UserId, message: String) =
            JoinRequest(
                groupId = groupId,
                userId = userId,
                message = message,
                status = AcceptStatus.APPROVED
            )
    }

    fun resolve(resolution: AcceptStatus): JoinRequest {
        require(resolution != AcceptStatus.UNRESOLVED) {"can't resolve to ${AcceptStatus.UNRESOLVED}"}

        return JoinRequest(
            groupId = this.groupId,
            userId = this.userId,
            message = this.message,
            status = resolution,
            ctime = this.ctime,
            mtime = LocalDateTime.now()
        )
    }

    override fun toString(): String {
        return "JoinRequest(groupId='$groupId', userId='$userId', accepted=$status)"
    }
}

interface JoinRequestRepository : BaseRepository<JoinRequest> {
    fun load(user : User, group: Group) : Either<Exception,JoinRequest>

    fun loadByUser(userId: UserId, ordering: GroupMembershipRequestOrdering, pagination: Repository.Pagination): Either<Exception, List<JoinRequest>>

    fun loadByGroup(groupId: String, ordering: GroupMembershipRequestOrdering, pagination: Repository.Pagination): Either<Exception, List<JoinRequest>>
}