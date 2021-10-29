package idel.domain

import arrow.core.Either
import java.time.LocalDateTime

/**
 * Group admin creates an invite when wants a user to join a group.
 */
class Invite(
    /**
     * Group's identifier
     */
    val groupId: String,
    /**
     * User's identifier. May be null if a person are yet not registered in the application.
     */
    val userId: UserId?,

    /**
     * User who created the invite.
     */
    val author: UserId,

    /**
     * User's email. Filled only for a person which yet is not application's user.
     */
    val userEmail: String?,

    /**
     * Invitation email was sent to a person.
     */
    val emailWasSent: Boolean?,

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
) : Identifiable {
    override val id = generateId(groupId, userId, userEmail)

    val isForPerson = userId == null

    val isForUser = userId != null

    init {
        require(
            (userId == null) && ((userEmail != null) && (emailWasSent != null)) ||
                    (userId != null) && ((userEmail == null) && (emailWasSent == null))
        )
        {"person invite should be filled with userEmail and emailWasSent OR userId should be filled"}

    }


    companion object {
        private fun generateId(groupId: String, userId: UserId?, userEmail: String?) =
            compositeId(key = "invt", groupId, userId ?: userEmail!!)

        fun id(user: User, group: Group): String = compositeId(key = "invt", group.id, user.id)


        fun createForRegisteredUser(groupId: String, userId: UserId, message: String, author: UserId) =
            Invite(
                groupId = groupId,
                userId = userId,
                author = author,
                userEmail = null,
                emailWasSent = null,
                status = AcceptStatus.UNRESOLVED,
                message = message
            )

        fun createForPerson(groupId: String, email: String, message: String, author: UserId) =
            Invite(
                groupId = groupId,
                userId = null,
                author = author,
                userEmail = email.lowercase(),
                emailWasSent = false,
                status = AcceptStatus.UNRESOLVED,
                message = message
            )
    }

    private fun clone(
        groupId: String = this.groupId,
        userId: UserId? = this.userId,
        userEmail: String? = this.userEmail,
        emailWasSent: Boolean? = this.emailWasSent,
        status: AcceptStatus = this.status,
        ctime: LocalDateTime = this.ctime
    ): Invite {
        return Invite(
            groupId = groupId,
            userId = userId,
            author = this.author,
            userEmail = userEmail,
            emailWasSent = emailWasSent,
            status = status,
            message = message,
            ctime = this.ctime,
            mtime = LocalDateTime.now()
        )
    }

    fun resolve(resolution: AcceptStatus): Invite {
        require(resolution != AcceptStatus.UNRESOLVED) {"Can't resolve to ${AcceptStatus.UNRESOLVED}"}
        return clone(status = resolution)
    }

    fun convertToUserInvite(user: User): Invite {
        require(userId == null) {"invite already belongs to user, userId = ${userId}"}
        require(this.userEmail == user.email.lowercase()) {
            "invites not belongs to the user. Emails are different:" +
                    " invite.userEmail = [${this.userEmail}], user.email = [${user.email.lowercase()}]"
        }

        return this.clone(userId = user.id, userEmail = null, emailWasSent = null)
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

interface InviteRepository : BaseRepository<Invite> {

    fun load(user : User, group: Group) : Either<Exception, Invite>

    fun loadByUser(
        userId: String,
        order: GroupMembershipRequestOrdering,
        pagination: Repository.Pagination
    ): Either<Exception, List<Invite>>

    fun loadByEmail(
        email: String,
        pagination: Repository.Pagination
    ): Either<Exception, List<Invite>>

    fun loadByGroup(
        groupId: String,
        order: GroupMembershipRequestOrdering,
        pagination: Repository.Pagination
    ): Either<Exception, List<Invite>>

}