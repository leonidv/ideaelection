package idel.domain

import arrow.core.*
import arrow.core.extensions.fx
import mu.KotlinLogging
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
    fun requestMembership(groupId: String, userId: UserId, message : String): Either<Exception, JoinRequest> {
        return try {
            loadEntities(groupId = groupId, userId = userId).flatMap {(entryMode: GroupEntryMode, user: User) ->
                when (entryMode) {
                    GroupEntryMode.PUBLIC -> {
                        val request = JoinRequest.createApproved(groupId, userId, message)
                        val newMember = GroupMember.of(groupId, user)
                        //groupRepository.addMember(groupId, newMember).map {request}
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
                        val groupMember = GroupMember.of(joinRequest.groupId, user)
                        groupMemberRepository.add(groupMember).bind()
                        val nextJoinRequest = joinRequest.resolve(AcceptStatus.APPROVED)
                        joinRequestRepository.replace(nextJoinRequest)
                        nextJoinRequest
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