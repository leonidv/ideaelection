package idel.domain

import arrow.core.Either
import arrow.core.computations.either
import arrow.core.flatMap
import arrow.core.separateEither
import mu.KotlinLogging


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
        return either.eager {
            val group = groupRepository.load(groupId).bind()
            val user = userRepository.load(userId).bind()
            Entities(group, user)
        }
    }

    private fun loadEntitiesByJoiningKey(groupJoiningKey: String, userId: String): Either<Exception, Entities> {
        return either.eager {
            val group = groupRepository.loadByJoiningKey(groupJoiningKey).bind()
            val user = userRepository.load(userId).bind()
            Entities(group, user)
        }
    }

    data class InviteToGroupResult(
        val createdInvites: Set<Invite>,
        val errorsFor: Set<String>
    )

    /**
     * Create invite if group and user are exist.
     *
     * Check that group and user are exists
     */
    fun inviteUsersToGroup(
        author: User,
        groupId: String,
        message: String,
        registeredUserIds: Set<String>,
        newUserEmails: Set<String>
    ): Either<Exception, InviteToGroupResult> {
        fun errorToId(ex: Exception, id: String): String {
            return if (ex is EntityAlreadyExists) {
                "!!$id!!invite-already-exists"
            } else {
                id
            }
        }

        return either.eager {
            if (!groupRepository.exists(groupId).bind()) {
                throw EntityNotFound("group", groupId)
            }
            val existsResult = registeredUserIds.map {userId ->
                val eUserExists = userRepository.exists(userId)
                when (eUserExists) {
                    is Either.Right -> {
                        if (eUserExists.value) {
                            val invite = Invite.createForRegisteredUser(
                                groupId = groupId,
                                userId = userId,
                                message = message,
                                author = author.id
                            )
                            inviteRepository.add(invite).mapLeft {errorToId(it, userId)}
                        } else {
                            Either.Left(userId)
                        }
                    }

                    is Either.Left -> Either.Left(userId)
                }

            }.separateEither()

            val newUsersResult = newUserEmails.map {personEmail ->
                val invite = Invite.createForPerson(
                    groupId = groupId,
                    email = personEmail,
                    message = message,
                    author = author.id
                )
                inviteRepository.add(invite).mapLeft {errorToId(it, personEmail)}
            }.separateEither()

            val invites = (existsResult.second + newUsersResult.second).toSet()
            val errors = (existsResult.first + newUsersResult.first).filterNot {it.isEmpty()}.toSet()

            InviteToGroupResult(createdInvites = invites, errorsFor = errors)

        }
    }

    fun requestMembership(joiningKey: String, userId: UserId, message: String): Either<Exception, JoinRequest> {
        return try {
            loadEntitiesByJoiningKey(
                groupJoiningKey = joiningKey,
                userId = userId
            ).flatMap {(group: Group, user: User) ->
                val groupId = group.id
                val domainRestrictions = group.domainRestrictions
                val domainAllowed = domainRestrictions.isEmpty() || domainRestrictions.contains(user.domain)
                if (domainAllowed) {
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
                } else {
                    Either.Left(EntityNotFound("group", "joiningKey = $joiningKey"))
                }
            }
        } catch (ex: Exception) {
            Either.Left(ex)
        }
    }

    fun resolveRequest(requestId: String, status: AcceptStatus): Either<Exception, JoinRequest> {
        return joinRequestRepository.load(requestId).flatMap {joinRequest ->
            when (status) {
                AcceptStatus.UNRESOLVED -> Either.Left(IllegalArgumentException("Can't resolve to UNRESOLVED"))
                AcceptStatus.APPROVED -> {
                    either.eager {
                        val user = userRepository.load(joinRequest.userId).bind()
                        val groupMember = GroupMember.of(joinRequest.groupId, user, GroupMemberRole.MEMBER)
                        groupMemberRepository.add(groupMember).bind()
                        val nextJoinRequest = joinRequest.resolve(AcceptStatus.APPROVED)
                        joinRequestRepository.delete(joinRequest.id)
                        nextJoinRequest
                    }
                }
                AcceptStatus.DECLINED -> {
                    joinRequestRepository.mutate(requestId) {joinRequest.resolve(AcceptStatus.DECLINED)}
                }
            }
        }

    }

    fun resolveInvite(inviteId: String, status: AcceptStatus): Either<Exception, Invite> {
        return inviteRepository.load(inviteId).flatMap {invite ->
            if (invite.isForPerson) {
                Either.Left(IllegalArgumentException("Only invite for user can be resolved"))
            } else {
                when (status) {
                    AcceptStatus.UNRESOLVED -> Either.Left(IllegalArgumentException("Can't resolve to UNRESOLVED"))
                    AcceptStatus.APPROVED -> {
                        either.eager {
                            val user = userRepository.load(invite.userId!!).bind()
                            val groupMember = GroupMember.of(invite.groupId, user, GroupMemberRole.MEMBER)
                            groupMemberRepository.add(groupMember).bind()
                            inviteRepository.delete(invite.id).map {invite.resolve(AcceptStatus.APPROVED)}.bind()
                        }
                    }
                    AcceptStatus.DECLINED -> {
                        either.eager {
                            inviteRepository.delete(inviteId).map {invite.resolve(AcceptStatus.DECLINED)}.bind()
                        }
                    }
                }
            }
        }
    }
}