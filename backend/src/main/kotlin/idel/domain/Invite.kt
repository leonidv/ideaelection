package idel.domain

import arrow.core.Either
import java.time.LocalDateTime

/**
 * Group admin creates an invite when wants a user to join a group.
 */
class Invite(
    val groupId: String,
    val userId: UserId,
    val status: AcceptStatus,
    val ctime: LocalDateTime = LocalDateTime.now(),
    val mtime: LocalDateTime = ctime
) : Identifiable {
    override val id = compositeId(groupId, userId)

    companion object {
        fun create(groupId: String, userId: UserId) =
            Invite(
                groupId = groupId,
                userId = userId,
                status = AcceptStatus.UNRESOLVED
            )
    }

    override fun toString(): String {
        return "Invite(groupId='$groupId', userId='$userId', accepted=$status)"
    }

}

interface InviteRepository {
    fun add(invite: Invite): Either<Exception, Invite>

    fun load(id: String): Either<Exception, Invite>

    fun replace(invite: Invite) : Either<Exception, Invite>
}