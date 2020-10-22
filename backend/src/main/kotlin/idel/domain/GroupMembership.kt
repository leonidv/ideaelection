package idel.domain

import arrow.core.*
import arrow.core.extensions.fx
import arrow.core.extensions.option.monadFilter.monadFilter
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

    fun load(id: String): Either<Exception, Option<Invite>>

    fun replace(invite: Invite)
}

interface JoinRequestRepository {
    fun add(request: JoinRequest)

    fun load(id: String): Either<Exception, Option<JoinRequest>>

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

    fun create(request: MembershipRequest) =
        GroupMembership(
                id = generateId(),
                ctime = LocalDateTime.now(),
                userId = request.userId,
                groupId = request.groupId
        )

}

interface GroupMembershipRepository {
    fun add(membership: GroupMembership)

    fun load(membershipId: String): Either<Exception, Option<GroupMembership>>
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

    private val membershipFactory = GroupMembershipFactory()

    data class UserAndGroup(val group: Group, val user: User)

    private fun loadEntities(groupId: String, userId: UserId): Either<Exception, UserAndGroup> {
        val x: Either<Exception, Pair<Option<Group>, Option<User>>> = Either.fx {
            val (group) = groupRepository.load(groupId)
            val (user) = userRepository.load(userId)
            Pair(group, user)
        }

        val y: Either<Exception, UserAndGroup> = x.flatMap {(oGroup, oUser) ->
            if (oGroup is Some && oUser is Some) {
                Either.right(UserAndGroup(oGroup.t, oUser.t))
            } else if (oGroup is None) {
                Either.left(IllegalArgumentException("group is not exists, groupId = $groupId"))
            } else if (oUser is None) {
                Either.left(IllegalArgumentException("user is not exists, userId = $userId"))
            } else {
                Either.left(IllegalStateException("Option is not Some and not None :)"))
            }
        }

        return y
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
            loadEntities(groupId = groupId, userId = userId).map {(group: Group, user: User) ->
                when (group.entryMode) {
                    GroupEntryMode.PUBLIC -> {
                        val request = JoinRequest.createApproved(groupId, userId)
                        val membership = membershipFactory.create(request)
                        membershipRepository.add(membership)
                        request
                    }
                    GroupEntryMode.CLOSED,
                    GroupEntryMode.PRIVATE -> JoinRequest.createUnresloved(groupId, userId)
                }
            }
        } catch (ex: Exception) {
            Either.left(ex)
        }
    }
}