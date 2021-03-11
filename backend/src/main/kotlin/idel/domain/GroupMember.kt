package idel.domain

import arrow.core.Either
import arrow.core.Option
import java.time.LocalDateTime

enum class GroupMemberRole {
    GROUP_ADMIN, MEMBER
}

class GroupMember(
    val userId: String,
    val groupId : String,
    val ctime: LocalDateTime,
    val roleInGroup : GroupMemberRole,

    override val avatar: String,
    override val displayName: String,
    override val email: String,
    override val roles: Set<String>

) : Identifiable, User {
    override val id = calculateId(groupId, userId)

    companion object {
        fun calculateId(groupId: String, userId: String) = compositeId(key = "gm", groupId, userId)

        fun of(groupId: String, user: User, role : GroupMemberRole): GroupMember {
            return GroupMember(
                    userId = user.id,
                    groupId = groupId,
                    ctime = LocalDateTime.now(),
                    roleInGroup = role,
                    avatar = user.avatar,
                    displayName = user.displayName,
                    email = user.email,
                    roles = user.roles
            )
        }
    }
}

interface GroupMemberRepository : BaseRepository<GroupMember>, CouchbaseTransactionBaseRepository<GroupMember> {
    /**
     * Check that groups has user as member.
     */
    fun isMember(groupId: String,userId: String) : Either<Exception, Boolean>

    /**
     * Load group member by group and user id.
     */
    fun load(groupId: String, userId: String) : Either<Exception, GroupMember>

    /**
     * Remove member group from a group.
     */
    fun removeFromGroup(groupId: String, userId: String): Either<Exception, Unit>

    fun loadByGroup(
        groupId: String,
        pagination: Repository.Pagination,
        usernameFilter: Option<String>
    ): Either<Exception, List<GroupMember>>

}
