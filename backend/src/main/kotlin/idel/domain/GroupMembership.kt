package idel.domain

import arrow.core.Either
import arrow.core.continuations.either
import arrow.core.flatMap
import arrow.core.flatten
import arrow.core.separateEither
import mu.KotlinLogging
import java.util.*


/**
 * Status of the membership request.
 */
enum class AcceptStatus(val readOnly: Boolean) {
    /**
     * Membership request is still waiting for resolution.
     */
    UNRESOLVED(readOnly = false),

    /**
     * Request was approved.
     */
    APPROVED(readOnly = true),

    /**
     * Request is rejected.
     */
    DECLINED(readOnly = true),

    /**
     * Request is revoked by creator
     */
    REVOKED(readOnly = true)
}


enum class GroupMembershipRequestOrdering {
    CTIME_ASC,
    CTIME_DESC,
    MTIME_ASC,
    MTIME_DESC
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

    private fun loadEntitiesByJoiningKey(groupJoiningKey: String, userId: UserId): Either<DomainError, Entities> {
        return either.eager {
            val group = groupRepository.loadByJoiningKey(groupJoiningKey).bind()
            val user = userRepository.load(userId).bind()
            Entities(group, user)
        }
    }

    data class InviteToGroupResult private constructor(
        val invites: Iterable<Invite>,
        val errors: Iterable<InviteCreationError>
    ) {
        companion object {
            fun create(result: Iterable<Either<InviteCreationError, Invite>>): InviteToGroupResult {
                val (errors, invites) = result.separateEither()
                return InviteToGroupResult(invites = invites, errors = errors)
            }
        }

        infix fun and(another: InviteToGroupResult) =
            InviteToGroupResult(
                invites = this.invites + another.invites,
                errors = this.errors + another.errors
            )
    }

    data class InviteCreationError private constructor(
        val id: UserId?,
        val email: String?,
        val error: String
    ) {
        companion object {
            fun forUser(id: UserId, error: String) = InviteCreationError(id = id, email = null, error = error)
            fun forPerson(email: String, error: String) = InviteCreationError(id = null, email, error)
        }
    }

    /**
     * It's presupposed that a group and user exists.
     */
    fun inviteUsersToGroup(
        author: User,
        group: Group,
        message: String,
        registeredUserIds: Set<UserId>,
        newUserEmails: Set<String>
    ): InviteToGroupResult {
        val usersResult = createInviteForUsers(author, group, message, registeredUserIds)
        val personsResult = createForPersons(author, group, message, newUserEmails)
        return usersResult and personsResult
    }

    /**
     * Create a new invite for user. If user already created a join request to same group, the join request is resolved
     * and invite is auto approved.
     */
    private fun createInviteForUsers(
        author: User,
        group: Group,
        message: String,
        usersIds: Set<UserId>
    ): InviteToGroupResult {
        val inviteCreationResult = usersIds.map {userId ->
            fTransaction {
                either.eager {
                    val user = userRepository.load(userId).bind()

                    val possibleJoinRequest = joinRequestRepository.loadUnresolved(user, group)

                    if (possibleJoinRequest.isNotEntityOrNotFound()) {
                        possibleJoinRequest.bind() // ugly exit with error
                    }

                    var invite = Invite.createForRegisteredUser(group.id, user, message, author.id)
                    invite = when (possibleJoinRequest) {
                        // join request is not found, create new Invite
                        is Either.Left -> {
                            require(possibleJoinRequest.value is EntityNotFound) // double check of developer
                            inviteRepository.add(invite).bind()
                        }

                        // join request is found, resolve it and return approved invite
                        is Either.Right -> {
                            resolveRequest(possibleJoinRequest.value, AcceptStatus.APPROVED).bind()
                            invite.accept().bind()
                        }
                    }
                    invite
                }
            }.mapLeft {domainError ->
                InviteCreationError.forUser(userId, domainError.toString())
            }
        }

        return InviteToGroupResult.create(inviteCreationResult)
    }

    private fun createForPersons(
        author: User,
        group: Group,
        message: String,
        newUserEmails: Set<String>
    ): InviteToGroupResult {
        val results = newUserEmails.map {email ->
            fTransaction {
                val invite = Invite.createForPerson(group.id, email, message, author.id)
                inviteRepository.add(invite)
            }.mapLeft {domainError ->
                InviteCreationError.forPerson(email, domainError.toString())
            }
        }

        return InviteToGroupResult.create(results)
    }

    fun requestMembership(joiningKey: String, userId: UserId, message: String): Either<DomainError, JoinRequest> {
        return fTransaction {
            loadEntitiesByJoiningKey(
                groupJoiningKey = joiningKey,
                userId = userId
            ).flatMap {(group: Group, user: User) ->
                either.eager {
                    val possibleInvite = inviteRepository.loadUnresolved(user, group)

                    if (possibleInvite.isNotEntityOrNotFound()) {
                        possibleInvite.bind()
                    }

                    when (possibleInvite) {
                        is Either.Right -> approveInviteByJoinRequest(group, user, possibleInvite.value, message).bind()
                        is Either.Left -> createJoinRequest(group, user, message).bind()
                    }
                }
            }
        }
    }

    private fun approveInviteByJoinRequest(group: Group, user: User, invite: Invite, message: String): Either<DomainError, JoinRequest> {
        return either.eager {
            resolveInviteByUser(invite, user, AcceptStatus.APPROVED).bind()
            JoinRequest.createApproved(group.id, user.id, message)
        }
    }


    /**
     * Base flow of creation join request.
     * Creates a join request for non-public groups or creates a membership, if group is public.
     */
    private fun createJoinRequest(
        group: Group,
        user: User,
        message: String
    ): Either<DomainError, JoinRequest> {
        val groupId = group.id
        val userId = user.id

        val domainRestrictions = group.domainRestrictions
        val domainAllowed = domainRestrictions.isEmpty() || domainRestrictions.contains(user.domain)

        return if (domainAllowed) {
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
            Either.Left(EntityNotFound("group", "joiningKey = ${group.joiningKey}"))
        }
    }


    fun resolveRequest(requestId: UUID, status: AcceptStatus): Either<DomainError, JoinRequest> {
        return fTransaction {
            either.eager {
                val joinRequest = joinRequestRepository.load(requestId).bind()
                resolveRequest(joinRequest, status).bind()
            }
        }
    }

    private fun resolveRequest(joinRequest: JoinRequest, status: AcceptStatus): Either<DomainError, JoinRequest> {
        return fTransaction {
            either.eager {
                val nextJoinRequest = when (status) {
                    AcceptStatus.APPROVED -> {
                        val nextJoinRequest = joinRequest.resolve(AcceptStatus.APPROVED).bind()
                        val user = userRepository.load(joinRequest.userId).bind()
                        val groupMember = GroupMember.of(joinRequest.groupId, user, GroupMemberRole.MEMBER)
                        groupMemberRepository.add(groupMember).bind()
                        nextJoinRequest
                    }
                    else -> {
                        joinRequest.resolve(status).bind()
                    }
                }
                joinRequestRepository.update(nextJoinRequest).bind()
            }
        }
    }

    /**
     * User can accept or decline invite. This function check resolution status for these values.
     */
    private fun resolveInviteByUser(invite: Invite, user: User, status: AcceptStatus): Either<DomainError, Invite> {
        require(user.id == invite.userId) {
            "How did you check that the invite belongs to the user??? " +
                    "invite.userId = ${invite.userId}, user.id = ${user.id}"
        }

        return when (status) {
            AcceptStatus.UNRESOLVED -> Either.Left(InvalidOperation("Can't resolve to UNRESOLVED"))
            AcceptStatus.REVOKED -> Either.Left(InvalidOperation("User can't revoke invite. Only group admin can revoke invite to group"))
            AcceptStatus.APPROVED -> {
                either.eager {
                    val groupMember = GroupMember.of(invite.groupId, user, GroupMemberRole.MEMBER)
                    groupMemberRepository.add(groupMember).bind()
                    val nextInvite = invite.accept().bind()
                    inviteRepository.update(nextInvite).bind()
                }
            }
            AcceptStatus.DECLINED -> {
                either.eager {
                    val nextInvite = invite.decline().bind()
                    inviteRepository.update(nextInvite).bind()
                }
            }
        }
    }

    /**
     * Admin can only revoke invite. This function check resolution status for correct value.
     */
    private fun resolveInviteByAdmin(invite: Invite, status: AcceptStatus): Either<DomainError, Invite> {
        return when (status) {
            AcceptStatus.APPROVED,
            AcceptStatus.DECLINED,
            AcceptStatus.UNRESOLVED ->
                Either.Left(InvalidOperation("Admin can only change status of invite to ${AcceptStatus.REVOKED}"))

            AcceptStatus.REVOKED -> {
                either.eager {
                    val nextInvite = invite.revoke().bind()
                    inviteRepository.update(nextInvite).bind()
                    nextInvite
                }
            }
        }
    }

    fun resolveInvite(
        resolver: User,
        invite: Invite,
        status: AcceptStatus,
        accessLevel: InviteAccessLevel
    ): Either<DomainError, Invite> {
        return when (accessLevel) {
            InviteAccessLevel.GROUP_ADMIN -> resolveInviteByAdmin(invite, status)
            InviteAccessLevel.OWNER -> resolveInviteByUser(invite, resolver, status)
        }
    }


    fun convertPersonInviteToUserInvite(user: User): Either<DomainError, Int> {
        var counter = 0;
        var nextOffset = 0;
        var count = 99;
        val result = fTransaction {
            either.eager {
                do {
                    val invites = inviteRepository.loadByEmail(
                        user.email,
                        Repository.Pagination(skip = nextOffset, count = count)
                    ).bind()

                    val (convertedInvites, exceptions) = invites.map {invite ->
                        val nextInvite = invite.convertToUserInvite(user).bind()
                        inviteRepository.update(nextInvite)
                    }.separateEither()

                    counter += convertedInvites.size
                    nextOffset += count
                } while (invites.isNotEmpty())
                counter
            }
        }
        return result
    }
}