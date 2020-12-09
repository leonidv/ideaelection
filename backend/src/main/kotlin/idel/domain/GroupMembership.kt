package idel.domain

import arrow.core.*
import arrow.core.extensions.fx
import java.time.LocalDateTime
import kotlin.Exception


/**
 * Status of the membership request.
 */
enum class AcceptStatus {
    /**
     * Membership request is still waiting for resolution.
     */
    UNRESOLVED,

    /**
     * Request was approved.
     */
    APPROVED,

    /**
     * Request was rejected.
     */
    REJECTED
}

/**
 * Common request for membership user in group
 */
sealed class MembershipRequest(
        val groupId: String,
        val userId: UserId,
        val status: AcceptStatus,
        val ctime: LocalDateTime,
        val mtime: LocalDateTime
) : Identifiable {
    override val id: String = compositeId(groupId, userId)
}


/**
 * Group admin creates an invite when wants a user to join a group.
 */
class Invite(
        groupId: String,
        userId: UserId,
        status: AcceptStatus,
        ctime: LocalDateTime = LocalDateTime.now(),
        mtime: LocalDateTime = ctime
) :
        MembershipRequest(groupId, userId, status, ctime, mtime) {
    companion object {
        fun create(groupId: String, userId: UserId) =
                Invite(groupId = groupId,
                        userId = userId,
                        status = AcceptStatus.UNRESOLVED)
    }

    override fun toString(): String {
        return "Invite(groupId='$groupId', userId='$userId', accepted=$status)"
    }

}

/**
 * User creates a join request when want to join the group.
 */
class JoinRequest(groupId: String,
                  userId: UserId,
                  status: AcceptStatus,
                  ctime: LocalDateTime = LocalDateTime.now(),
                  mtime: LocalDateTime = ctime
) : MembershipRequest(groupId, userId, status, ctime, mtime) {
    companion object {
        fun createUnresloved(groupId: String, userId: UserId) =
                JoinRequest(
                        groupId = groupId,
                        userId = userId,
                        status = AcceptStatus.UNRESOLVED
                )

        fun createApproved(groupId: String, userId: UserId) =
                JoinRequest(
                        groupId = groupId,
                        userId = userId,
                        AcceptStatus.APPROVED
                )
    }

    fun resolve(resolution: AcceptStatus): JoinRequest {
        require(resolution != AcceptStatus.UNRESOLVED) {"can't resolve to ${AcceptStatus.UNRESOLVED}"}

        return JoinRequest(
                groupId = this.groupId,
                userId = this.userId,
                status = resolution,
                ctime = this.ctime,
                mtime = LocalDateTime.now()
        )
    }

    override fun toString(): String {
        return "JoinRequest(groupId='$groupId', userId='$userId', accepted=$status)"
    }
}

enum class GroupMembershipRequestOrdering {
    CTIME_ASC,
    CTIME_DESC
}

interface InviteRepository {
    fun add(invite: Invite): Either<Exception, Invite>

    fun load(id: String): Either<Exception, Invite>

    fun replace(invite: Invite) : Either<Exception, Invite>
}


interface JoinRequestRepository {
    fun add(request: JoinRequest): Either<Exception, JoinRequest>

    fun load(id: String): Either<Exception, JoinRequest>

    fun remove(id: String): Either<Exception, Unit>

    fun replace(invite: JoinRequest): Either<Exception, JoinRequest>

    fun loadByUser(userId: UserId, ordering: GroupMembershipRequestOrdering, pagination: Repository.Pagination): Either<Exception, List<JoinRequest>>

    fun loadByGroup(groupId: String, ordering: GroupMembershipRequestOrdering, pagination: Repository.Pagination): Either<Exception, List<JoinRequest>>
}

/**
 * Implement group membership complex scenarios.
 *
 * It's like application service.
 */
class GroupMembershipService(
        private val groupRepository: GroupRepository,
        private val userRepository: UserRepository,
        private val joinRequestRepository: JoinRequestRepository,
        private val inviteRepository: InviteRepository,
        private val groupMemberRepository: GroupMemberRepository

) {

    data class Entities(val groupEntryMode: GroupEntryMode, val user: User)

    private fun loadEntities(groupId: String, userId: UserId): Either<Exception, Entities> {
        return Either.fx {
            val (group) = groupRepository.load(groupId)
            val (user) = userRepository.load(userId)
            Entities(group.entryMode, user)
        }
    }

    /**
     * Create invite if group and user are exist.
     *
     * Check that group and user are exists
     */
    fun inviteToGroup(groupId: String, userId: UserId): Either<Exception, Invite> {
        return loadEntities(groupId = groupId, userId = userId).map {
            Invite.create(groupId, userId)
        }
    }

    /**
     * Process the request for join to the group. If request is approved by group create group membership.
     *
     * Check that group and user are exists.
     */
    fun requestMembership(groupId: String, userId: UserId): Either<Exception, JoinRequest> {
        return try {
            loadEntities(groupId = groupId, userId = userId).flatMap {(entryMode: GroupEntryMode, user: User) ->
                when (entryMode) {
                    GroupEntryMode.PUBLIC -> {
                        val request = JoinRequest.createApproved(groupId, userId)
                        val newMember = GroupMember.of(groupId, user)
                        //groupRepository.addMember(groupId, newMember).map {request}
                        groupMemberRepository.add(newMember).map {request}

                    }
                    GroupEntryMode.CLOSED,
                    GroupEntryMode.PRIVATE -> {
                        val request = JoinRequest.createUnresloved(groupId, userId)
                        joinRequestRepository.add(request)
                    }
                }
            }
        } catch (ex: Exception) {
            Either.left(ex)
        }
    }

    fun resolveRequest(requestId: String, status: AcceptStatus): Either<Exception, JoinRequest> {
        return joinRequestRepository.load(requestId).flatMap {joinRequest ->
            when (status) {
                AcceptStatus.UNRESOLVED -> Either.left(IllegalArgumentException("Can't resolve to UNRESOLVED"))
                AcceptStatus.APPROVED -> {
                    Either.fx {
                        val (user) = userRepository.load(joinRequest.userId)
                        groupMemberRepository.add(GroupMember.of(joinRequest.groupId, user)).bind()
                        joinRequest.resolve(AcceptStatus.APPROVED) // for future notification and event-based perstence
                        joinRequestRepository.remove(joinRequest.id).bind()
                        joinRequest
                    }
                }
                AcceptStatus.REJECTED -> {
                    val rejectedRequest = joinRequest.resolve(AcceptStatus.REJECTED)
                    joinRequestRepository.replace(rejectedRequest)
                }
            }
        }

    }
}