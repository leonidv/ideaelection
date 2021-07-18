package idel.domain

import arrow.core.Either
import arrow.core.None
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

        fun createAdmin(groupId: String, user: User) : GroupMember {
            return of(groupId, user, GroupMemberRole.GROUP_ADMIN)
        }

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

    private fun clone(
        userId : String = this.userId,
        groupId: String = this.groupId,
        ctime: LocalDateTime = this.ctime,
        roleInGroup: GroupMemberRole = this.roleInGroup,
        avatar: String = this.avatar,
        displayName: String = this.displayName,
        email: String = this.email,
        roles: Set<String> = this.roles
    ) : GroupMember {
        return GroupMember(
            userId = userId,
            groupId = groupId,
            ctime = ctime,
            roleInGroup = roleInGroup,
            avatar = avatar,
            displayName = displayName,
            email = email,
            roles = roles
        )
    }

    fun changeRole(nextRole : GroupMemberRole) : GroupMember {
        return if (this.roleInGroup == nextRole) {
            this
        } else {
            this.clone(roleInGroup = nextRole)
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

    fun changeRole(groupMember: GroupMember) : Either<Exception, GroupMember>;

    fun loadByGroup(
        groupId: String,
        pagination: Repository.Pagination,
        usernameFilter: Option<String> = None,
        roleFilter: Option<GroupMemberRole> = None
    ): Either<Exception, List<GroupMember>>

}
