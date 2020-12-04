package idel.domain

import arrow.core.Either
import java.time.LocalDateTime

class GroupMember(
        val userId: String,
        val groupId : String,
        val ctime: LocalDateTime,
        val email: String,
        val displayName: String,
        val avatar: String,
) : Identifiable {
    companion object {
        fun calculateId(groupId: String, userId: String) = compositeId(groupId, userId)

        fun of(groupId: String, userInfo: IUserInfo): GroupMember {
            return GroupMember(
                    userId = userInfo.id,
                    groupId = groupId,
                    ctime = LocalDateTime.now(),
                    email = userInfo.email,
                    displayName = userInfo.displayName,
                    avatar = userInfo.avatar
            )
        }
    }

    override val id = calculateId(groupId, userId)
}

interface GroupMemberRepository {
    /**
     * Check that groups has user as member.
     */
    fun isMember(groupId: String,userId: String) : Either<Exception, Boolean>

    /**
     * Load group member by id.
     */
    fun load(id : String) : Either<Exception, GroupMember>

    /**
     * Load group member by group and user id.
     */
    fun load(groupId: String, userId: String) : Either<Exception, GroupMember>
    /**
     * Add group member.
     */
    fun add(entity : GroupMember): Either<Exception, GroupMember>

    /**
     * Remove member group group.
     */
    fun removeFromGroup(groupId: String, userId: String): Either<Exception, Unit>
}
