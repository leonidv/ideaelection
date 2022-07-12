package idel.domain

import arrow.core.Either
import arrow.core.continuations.either
import arrow.core.flatten
import idel.api.DataOrError
import idel.domain.security.GroupAccessLevel
import idel.domain.security.GroupMemberAccessLevel
import idel.domain.security.SecurityService
import mu.KLogger
import mu.KotlinLogging
import java.time.LocalDateTime
import java.util.*

enum class GroupMemberRole {
    GROUP_ADMIN, MEMBER
}

// TODO Really need inheritance from User??
class GroupMember(
    val userId: UserId,
    val groupId : GroupId,
    val ctime: LocalDateTime,
    val roleInGroup : GroupMemberRole,

    override val externalId: String,
    override val avatar: String,
    override val displayName: String,
    override val email: String,
    override val roles: Set<String>,
    override val subscriptionPlan: SubscriptionPlan

) : User {
    override val id: UserId = UUID.randomUUID()

    companion object {
        fun calculateId(groupId: GroupId, userId: UserId) = compositeId(key = "gm", groupId, userId)

        fun createAdmin(groupId: GroupId, user: User) : GroupMember {
            return of(groupId, user, GroupMemberRole.GROUP_ADMIN)
        }

        fun of(groupId: GroupId, user: User, role : GroupMemberRole): GroupMember {
            return GroupMember(
                    userId = user.id,
                    groupId = groupId,
                    externalId = user.externalId,
                    ctime = LocalDateTime.now(),
                    roleInGroup = role,
                    avatar = user.avatar,
                    displayName = user.displayName,
                    email = user.email,
                    roles = user.roles,
                    subscriptionPlan = user.subscriptionPlan
            )
        }
    }

    private fun clone(
        userId : UserId = this.userId,
        groupId: GroupId = this.groupId,
        ctime: LocalDateTime = this.ctime,
        roleInGroup: GroupMemberRole = this.roleInGroup,
        avatar: String = this.avatar,
        displayName: String = this.displayName,
        email: String = this.email,
        roles: Set<String> = this.roles,
        subscriptionPlan: SubscriptionPlan = this.subscriptionPlan,
        externalId: String = this.externalId
    ) : GroupMember {
        return GroupMember(
            userId = userId,
            groupId = groupId,
            ctime = ctime,
            roleInGroup = roleInGroup,
            avatar = avatar,
            displayName = displayName,
            email = email,
            roles = roles,
            subscriptionPlan = subscriptionPlan,
            externalId = externalId
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

interface GroupMemberRepository  {

    fun add(groupMember: GroupMember): Either<DomainError, GroupMember>

    /**
     * Check that groups has user as member.
     */
    fun isMember(groupId: GroupId,userId: UserId) : Either<DomainError, Boolean>

    /**
     * Load group member by group and user id.
     */
    fun load(groupId: GroupId, userId: UserId) : Either<DomainError, GroupMember>

    /**
     * Remove member group from a group.
     */
    fun removeFromGroup(groupId: GroupId, userId: UserId): Either<DomainError, Unit>

    fun update(groupMember: GroupMember) : Either<DomainError, GroupMember>;

    fun listByGroup(
        groupId: GroupId,
        pagination: Repository.Pagination,
        usernameFilter: String?,
        roleFilter: GroupMemberRole?
    ): Either<DomainError, List<GroupMember>>

}


typealias GroupMemberAction<T> = (groupMember: GroupMember) -> Either<DomainError, T>

class GroupMemberSecurity(private val securityService: SecurityService,
                          private val groupMemberRepository: GroupMemberRepository,
) {
    private val log = KotlinLogging.logger {}

    private fun <T> secure(
        groupId: GroupId,
        memberUserId: UserId,
        user: User,
        requiredLevels: Set<GroupMemberAccessLevel>,
        action: GroupMemberAction<T>
    ): Either<DomainError, T> {
        val result: Either<DomainError, Either<DomainError, T>> = fTransaction {
            either.eager {
                //val (group) = groupRepository.load(memberGroupId)
                val groupWithLevels = securityService.groupAccessLevel(groupId, user).bind()
                log.trace {"$user has $groupWithLevels for $groupId"}
                if (!groupWithLevels.levels.contains(GroupAccessLevel.MEMBER)) {
                    Either.Left(OperationNotPermitted())
                } else {
                    val groupMember = groupMemberRepository.load(groupId, memberUserId).bind()

                    val levels = securityService.groupMemberAccessLevels(groupMember, groupId, user).bind()
                    if (levels.intersect(requiredLevels).isNotEmpty()) {
                        action(groupMember)
                    } else {
                        Either.Left(OperationNotPermitted())
                    }
                }
            }
        }

        return result.flatten()
    }

    /**
     * Call an action if a [user] is an admin of the group or a groupmember belongs to him.
     */
    fun <T> asAdminOrHimSelf(memberGroupId: GroupId,
                             memberUserId: UserId,
                             user: User,
                             action: GroupMemberAction<T>): Either<DomainError, T> {
        val requiredLevels = setOf(GroupMemberAccessLevel.HIM_SELF, GroupMemberAccessLevel.GROUP_ADMIN)
        return secure(memberGroupId, memberUserId, user, requiredLevels, action)
    }
}