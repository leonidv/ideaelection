package idel.domain

import arrow.core.Either
import arrow.core.Option
import arrow.core.Some
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
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
) {
    val id: String = generateId()
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

interface InviteRepository {
    fun add(invite: Invite)

    fun load(id: String): Option<Invite>

    fun replace(invite: Invite)
}

interface JoinRequestRepository {
    fun add(request: JoinRequest)

    fun load(id: String): Option<Invite>

    fun replace(invite: JoinRequest)
}

/**
 * Relation between group and user.
 *
 */
class GroupMembership(
        val id: String,
        val ctime: LocalDateTime,
        val userId: UserId,
        val groupId: String,
)


class GroupMembershipFactory {

    fun createMembership(request: MembershipRequest): Either<Exception, GroupMembership> {
        return when (request.status) {
            AcceptStatus.APPROVED -> Either.right(GroupMembership(
                    id = generateId(),
                    ctime = LocalDateTime.now(),
                    userId = request.userId,
                    groupId = request.groupId
            ))

            AcceptStatus.REJECTED -> {
                Either.left(IllegalStateException("request was rejected, $request"))
            }

            AcceptStatus.UNRESOLVED -> {
                Either.left(IllegalStateException( "request is still waiting for resolution, $request"))
            }
        }
    }
}

interface GroupMembershipRepository {
    fun add(membership: GroupMembership)

    fun load(membershipId : String) : Option<GroupMembership>
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
        private val membershipRepository: GroupMembershipRepository
) {

    private val groupMembershipFactory = GroupMembershipFactory()

    data class UserAndGroup(val user: User, val group: Group)

    private fun loadEntities(groupId: String, userId: UserId): Either<Exception, UserAndGroup> {
        val optGroup = groupRepository.load(groupId)
        if (optGroup !is Some) {
            return Either.left(IllegalArgumentException("group is not exists, groupId = $groupId"))
        }

        val optUser = userRepository.load(userId)
        if (optUser !is Some) {
            return Either.left(IllegalArgumentException("user is not exists, userId = $userId"))
        }

        return Either.right(UserAndGroup(optUser.t, optGroup.t))

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
            loadEntities(groupId = groupId, userId = userId).map {(user: User, group: Group) ->
                if (group.allowToJoin(user)) {
                    val request = JoinRequest.createApproved(groupId = groupId, userId = userId)
                    val membership = groupMembershipFactory.createMembership(request)

                    when(membership) {
                        is Either.Left -> {
                            // error in the code, so throw exception
                            val cause = membership.a
                            throw IllegalStateException("can't create membership for approved join request $request",cause)
                        }

                        is Either.Right -> membershipRepository.add(membership.b)
                    }

                    request
                } else {
                    val request = JoinRequest.createUnresloved(groupId = groupId, userId = userId)
                    joinRequestRepository.add(request)
                    request
                }
            }
        } catch (ex : Exception) {
            Either.left(ex)
        }
    }
}