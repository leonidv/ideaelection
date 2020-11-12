package idel.domain

import arrow.core.*
import arrow.core.extensions.fx
import org.springframework.util.DigestUtils
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
        val status: AcceptStatus
) : Identifiable {
    override val id: String = compositeId(groupId, userId)
    val ctime: LocalDateTime = LocalDateTime.now()
}


/**
 * Group admin creates an invite when wants a user to join a group.
 */
class Invite(groupId: String, userId: UserId, status: AcceptStatus) : MembershipRequest(groupId, userId, status) {
    companion object {
        fun create(groupId: String, userId: UserId) = Invite(groupId = groupId, userId = userId, AcceptStatus.UNRESOLVED)
    }

    override fun toString(): String {
        return "Invite(groupId='$groupId', userId='$userId', accepted=$status)"
    }

}

/**
 * User creates a join request when want to join the group.
 */
class JoinRequest(groupId: String, userId: UserId, status: AcceptStatus) : MembershipRequest(groupId, userId, status) {
    companion object {
        fun createUnresloved(groupId: String, userId: UserId) =
                JoinRequest(
                        groupId = groupId,
                        userId = userId,
                        AcceptStatus.UNRESOLVED)

        fun createApproved(groupId: String, userId: UserId) =
                JoinRequest(
                        groupId = groupId,
                        userId = userId,
                        AcceptStatus.APPROVED
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

    fun replace(invite: Invite)
}


interface JoinRequestRepository {
    fun add(request: JoinRequest): Either<Exception, JoinRequest>

    fun load(id: String): Either<Exception, JoinRequest>

    fun replace(invite: JoinRequest)

    fun loadByUser(userId: UserId, ordering: GroupMembershipRequestOrdering, pagination: Repository.Pagination) : Either<Exception, List<JoinRequest>>

    fun loadByGroup(groupId : String, ordering: GroupMembershipRequestOrdering, pagination: Repository.Pagination) : Either<Exception, List<JoinRequest>>
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
        // private val membershipRepository: GroupMembershipRepository
) {

    data class Entities(val groupEntryMode: GroupEntryMode, val user: User)

    private fun loadEntities(groupId: String, userId: UserId): Either<Exception, Entities> {
        return Either.fx {
            val (entryMode) = groupRepository.loadEntryMode(groupId)
            val (user) = userRepository.load(userId)
            Entities(entryMode, user)
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
                        val newMember = GroupMember.of(user)
                        groupRepository.addMember(groupId, newMember).map {request}

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
}