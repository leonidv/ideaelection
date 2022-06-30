package idel.domain

import arrow.core.Either
import arrow.core.continuations.either
import arrow.core.flatten
import java.time.LocalDateTime
import java.util.*

enum class InviteEmailStatus {
    /**
     *   Email is waiting to be sent
     */
    WAIT_TO_SENT,

    /*
     Email has been sent
     */
    SENT
}

/**
 * Group admin creates an invite when wants a user to join a group.
 */
class Invite(
    val id: UUID,
    /**
     * Group's identifier
     */
    val groupId: GroupId,
    /**
     * User's identifier. May be null if a person are yet not registered in the application.
     */
    val userId: UserId?,

    /**
     * User who created the invite.
     */
    val author: UserId,

    val userEmail: String,

    /**
     * Invitation email was sent to a person.
     */
    val emailStatus: InviteEmailStatus,

    /**
     * Resolution status of invite.
     */
    val status: AcceptStatus,

    /**
     * Invite message.
     */
    val message: String,

    /**
     * Invite creation time.
     */
    val ctime: LocalDateTime = LocalDateTime.now(),

    /**
     * Invite modification time.
     */
    val mtime: LocalDateTime = ctime
) {

    val isForPerson = userId == null

    val isForUser = userId != null

    companion object {
        private fun generateId(groupId: String, userId: UserId?, userEmail: String?) =
            compositeId(key = "invt", groupId, userId ?: userEmail!!)

        fun id(user: User, group: Group): String = compositeId(key = "invt", group.id, user.id)


        fun createForRegisteredUser(groupId: GroupId, user: User, message: String, author: UserId) =
            Invite(
                id = UUID.randomUUID(),
                groupId = groupId,
                userId = user.id,
                author = author,
                userEmail = user.email,
                emailStatus = InviteEmailStatus.WAIT_TO_SENT,
                status = AcceptStatus.UNRESOLVED,
                message = message
            )

        fun createForPerson(groupId: GroupId, email: String, message: String, authorId: UserId) =
            Invite(
                id = UUID.randomUUID(),
                groupId = groupId,
                userId = null,
                author = authorId,
                userEmail = email.lowercase(),
                emailStatus = InviteEmailStatus.WAIT_TO_SENT,
                status = AcceptStatus.UNRESOLVED,
                message = message
            )
    }

    private fun clone(
        userId: UserId? = this.userId,
        status: AcceptStatus = this.status,
        emailStatus: InviteEmailStatus = this.emailStatus,
    ): Either<EntityReadOnly, Invite> {
        return when {
            this.status.readOnly -> Either.Left(EntityReadOnly)
            else -> {
                Either.Right(
                    Invite(
                        id = this.id,
                        ctime = this.ctime,
                        mtime = LocalDateTime.now(),
                        userId = userId,
                        groupId = this.groupId,
                        status = status,
                        author = this.author,
                        userEmail = this.userEmail,
                        emailStatus = emailStatus,
                        message = this.message
                    )
                )
            }
        }
    }

    fun revoke() = resolve(AcceptStatus.REVOKED)

    fun accept() = resolve(AcceptStatus.APPROVED)

    fun decline() = resolve(AcceptStatus.DECLINED)

    private fun resolve(resolution: AcceptStatus): Either<DomainError, Invite> {
        return when (resolution) {
            AcceptStatus.UNRESOLVED -> Either.Left(InvalidOperation("Can't resolve to $resolution"))
            AcceptStatus.APPROVED,
            AcceptStatus.REVOKED,
            AcceptStatus.DECLINED -> this.clone(status = resolution)
        }
    }

    fun convertToUserInvite(user: User): Either<DomainError, Invite> {
        return when {
            userId != null -> Either.Left(InvalidArgument("Invite already belongs to user"))
            // adds email validation
            !userEmail.equals(user.email, ignoreCase = true) ->
                Either.Left(InvalidArgument("invites email [${this.userEmail}] is not equals to user email [${user.email}]"))
            else -> {
                this.clone(userId = user.id)
            }
        }
    }


    override fun toString(): String {
        return "Invite(groupId='$groupId', userId='$userId', accepted=$status)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Invite

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

}

interface InviteRepository {
    /**
     * Add entity to collection
     */
    fun add(invite: Invite): Either<DomainError, Invite>

    /**
     * Load entity by id.
     */
    fun load(id: UUID): Either<DomainError, Invite>

    /**
     * Replace entity by id with mutation.
     */
    fun update(invite: Invite): Either<DomainError, Invite>

    fun loadUnresolved(user: User, group: Group): Either<DomainError, Invite>

    fun loadByUser(
        userId: UserId,
        statuses: Set<AcceptStatus>,
        order: GroupMembershipRequestOrdering,
        pagination: Repository.Pagination
    ): Either<DomainError, List<Invite>>

    fun loadByEmail(
        email: String,
        pagination: Repository.Pagination
    ): Either<DomainError, List<Invite>>

    fun loadByGroup(
        groupId: GroupId,
        statuses: Set<AcceptStatus>,
        order: GroupMembershipRequestOrdering,
        pagination: Repository.Pagination
    ): Either<DomainError, List<Invite>>

}

typealias InviteSecuredAction<T> = (invite: Invite, level: InviteAccessLevel) -> Either<DomainError, T>

enum class InviteAccessLevel {
    OWNER,
    GROUP_ADMIN
}

class InviteSecurity(
    private val inviteRepository: InviteRepository,
    private val groupSecurity: GroupSecurity
) {
    fun <T> asOwnerOrGroupAdmin(user: User, inviteId: UUID, action: InviteSecuredAction<T>): Either<DomainError, T> {
        return fTransaction {
            either.eager<DomainError, Either<DomainError, T>> {
                val invite = inviteRepository.load(inviteId).bind()
                if (user.id == invite.userId) {
                    action(invite, InviteAccessLevel.OWNER)
                } else {
                    val isGroupAdmin = groupSecurity.isAdmin(invite.groupId, user).bind()
                    if (isGroupAdmin) {
                        action(invite, InviteAccessLevel.GROUP_ADMIN)
                    } else {
                        Either.Left(OperationNotPermitted())
                    }
                }
            }
        }.flatten()
    }
}