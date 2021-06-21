package idel.domain

import arrow.core.*
import arrow.core.extensions.fx
import mu.KotlinLogging
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
    DECLINED
}


enum class GroupMembershipRequestOrdering {
    CTIME_ASC,
    CTIME_DESC
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
    private val log = KotlinLogging.logger {}

    data class Entities(val group: Group, val user: User)

    private fun loadEntities(groupId: String, userId: String): Either<Exception, Entities> {
        return Either.fx {
            val (group) = groupRepository.load(groupId)
            val (user) = userRepository.load(userId)
            Entities(group, user)
        }
    }

    private fun loadEntitiesByJoiningKey(groupJoiningKey: String, userId: String): Either<Exception, Entities> {
        return Either.fx {
            val (group) = groupRepository.loadByJoiningKey(groupJoiningKey)
            val (user) = userRepository.load(userId)
            Entities(group, user)
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
    fun requestMembership(joiningKey: String, userId: UserId, message: String): Either<Exception, JoinRequest> {
        return try {
            loadEntitiesByJoiningKey(
                groupJoiningKey = joiningKey,
                userId = userId
            ).flatMap {(group: Group, user: User) ->
                val groupId = group.id
                when (group.entryMode) {
                    GroupEntryMode.PUBLIC -> {
                        val request = JoinRequest.createApproved(groupId, userId, message)
                        val newMember = GroupMember.of(groupId, user, GroupMemberRole.MEMBER)
                        groupMemberRepository.add(newMember).map {request}

                    }
                    GroupEntryMode.CLOSED,
                    GroupEntryMode.PRIVATE -> {
                        val request = JoinRequest.createUnresolved(groupId, userId, message)
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
                        val groupMember = GroupMember.of(joinRequest.groupId, user, GroupMemberRole.MEMBER)
                        groupMemberRepository.add(groupMember).bind()
                        val nextJoinRequest = joinRequest.resolve(AcceptStatus.APPROVED)
                        joinRequestRepository.replace(nextJoinRequest)
                        nextJoinRequest
                    }
                }
                AcceptStatus.DECLINED -> {
                    val rejectedRequest = joinRequest.resolve(AcceptStatus.DECLINED)
                    joinRequestRepository.replace(rejectedRequest)
                }
            }
        }

    }
}