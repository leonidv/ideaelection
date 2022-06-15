package idel.domain

import arrow.core.Either
import java.time.LocalDateTime
import java.util.*

/**
 * User creates a join request when want to join the group.
 */
class JoinRequest(
    val id: UUID,
    val groupId: GroupId,
    val userId: UserId,
    val message: String,
    val status: AcceptStatus,
    val ctime: LocalDateTime = LocalDateTime.now(),
    val mtime: LocalDateTime = ctime
) {

    companion object {
        fun createUnresolved(groupId: GroupId, userId: UserId, message: String) =
            JoinRequest(
                id = UUID.randomUUID(),
                groupId = groupId,
                userId = userId,
                message = message,
                status = AcceptStatus.UNRESOLVED
            )

        fun createApproved(groupId: GroupId, userId: UserId, message: String) =
            JoinRequest(
                id = UUID.randomUUID(),
                groupId = groupId,
                userId = userId,
                message = message,
                status = AcceptStatus.APPROVED
            )
    }

    private fun clone(status: AcceptStatus) : Either<EntityReadOnly,JoinRequest> {
        return if (this.status.readOnly) {
            Either.Left(EntityReadOnly)
        } else {
            Either.Right(
                JoinRequest(
                    id = this.id,
                    groupId = this.groupId,
                    userId = this.userId,
                    message = this.message,
                    status = status,
                    ctime = this.ctime,
                    mtime = LocalDateTime.now()
                )
            )
        }
    }

    fun resolve(resolution: AcceptStatus): Either<InvalidOperation, JoinRequest> {
        return when (resolution) {
                AcceptStatus.UNRESOLVED -> Either.Left(InvalidOperation("Can't resolve to $resolution"))
                AcceptStatus.REVOKED,
                AcceptStatus.APPROVED,
                AcceptStatus.DECLINED -> this.clone(status = resolution)
            }
        }


    override fun toString(): String {
        return "JoinRequest(groupId='$groupId', userId='$userId', accepted=$status)"
    }
}

interface JoinRequestRepository {
    fun loadUnresolved(user: User, group: Group): Either<DomainError, JoinRequest>

    fun loadByUser(
        userId: UserId,
        statuses: Set<AcceptStatus>,
        ordering: GroupMembershipRequestOrdering,
        pagination: Repository.Pagination
    ): Either<DomainError, List<JoinRequest>>

    fun loadByGroup(
        groupId: GroupId,
        statuses: Set<AcceptStatus>,
        ordering: GroupMembershipRequestOrdering,
        pagination: Repository.Pagination
    ): Either<DomainError, List<JoinRequest>>

    fun load(id: UUID): Either<DomainError, JoinRequest>
    fun add(entity: JoinRequest): Either<DomainError, JoinRequest>

//    fun mutate(
//        id: UUID,
//        mutation: (entity: JoinRequest) -> JoinRequest
//    ): Either<DomainError, JoinRequest>

    fun possibleMutate(
        id: UUID,
        mutation: (entity: JoinRequest) -> Either<DomainError, JoinRequest>
    ): Either<DomainError, JoinRequest>
}