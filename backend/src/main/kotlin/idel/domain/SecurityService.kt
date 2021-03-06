package idel.domain

import arrow.core.Either
import arrow.core.computations.either
import mu.KotlinLogging


enum class GroupAccessLevel {
    NOT_MEMBER,
    MEMBER,
    ADMIN
}

enum class IdeaAccessLevel {
    DENIED,
    GROUP_MEMBER,
    GROUP_ADMIN,
    AUTHOR,
    ASSIGNEE
}

enum class GroupMemberAccessLevel {
    DENIED,
    GROUP_ADMIN,
    GROUP_MEMBER,
    HIM_SELF
}

/**
 * Security levels is how a user is aligned to a entity. Some operations are permitted only for some levels.
 *
 * For example, only a group member can add a new Idea to the group. A member has an access level {member}.
 * On another hand, a group admin is also a group member, so the admin has access levels {admin, member}.
 * As a result, when we check user access, we should only check that a user is a member of the group and
 * we should not check that a user is an admin every time. But if action can do only admin, {admin} level is checked.
 */
class SecurityService(
    private val groupMemberRepository: GroupMemberRepository,
    private val groupRepository: GroupRepository
) {

    val log = KotlinLogging.logger {}

    companion object {

        /**
         * Group levels of a Group admin has
         */
        val GROUP_LEVELS_ADMIN = setOf(GroupAccessLevel.ADMIN, GroupAccessLevel.MEMBER)

        /**
         * Group levels of a Group Member has
         */
        val GROUP_LEVELS_MEMBER = setOf(GroupAccessLevel.MEMBER)

        /**
         * Group level of a not group member
         */
        val GROUP_LEVELS_NOT_MEMBER = setOf(GroupAccessLevel.NOT_MEMBER)

        /**
         *  Idea levels of a group admin. Admin can do anything in a group.
         */
        val IDEA_LEVELS_FOR_GROUP_ADMIN = IdeaAccessLevel.values().toSet().minus(IdeaAccessLevel.DENIED)

        /**
         * Base set of idea levels of a member group.
         */
        val IDEA_LEVELS_FOR_MEMBER = setOf(IdeaAccessLevel.GROUP_MEMBER)

        /**
         * Idea levels of a not member group.
         */
        val IDEA_LEVELS_FOR_NOT_GROUP_MEMBER = setOf(IdeaAccessLevel.DENIED)

        /**
         * Group member level of a group admin.
         */
        val GROUP_MEMBER_LEVELS_FOR_ADMIN = setOf(
            GroupMemberAccessLevel.GROUP_ADMIN,
            GroupMemberAccessLevel.GROUP_MEMBER,
            GroupMemberAccessLevel.HIM_SELF
        )

        /**
         * Group member level of a group member.
         */
        val GROUP_MEMBER_LEVELS_FOR_MEMBER = setOf(GroupMemberAccessLevel.GROUP_MEMBER)

        /**
         * Group member user is user.
         */
        val GROUP_MEMBER_HIM_SELF = setOf(GroupMemberAccessLevel.GROUP_MEMBER, GroupMemberAccessLevel.HIM_SELF)

        /**
         * User can't work with group member.
         */
        val GROUP_MEMBER_ACCESS_DENIED = setOf(GroupMemberAccessLevel.DENIED)
    }


    private fun containsEntity(collection: Collection<Identifiable>, entityId: String): Boolean {
        return collection.find {it.id == entityId} != null
    }

    private fun <T : Identifiable> containsEntity(collection: Collection<T>, entity: T): Boolean {
        return containsEntity(collection, entity.id)
    }

    private fun <T : Identifiable> Collection<T>.has(entity: T) = containsEntity(this, entity)

    fun isSuperUser(user: User): Boolean {
        return user.roles.contains(Roles.SUPER_USER)
    }

    /**
     * Calculate group access level.
     */
    fun groupAccessLevel(groupId: String, user: User): Either<Exception, Set<GroupAccessLevel>> {
        val eGroupExists = groupRepository.exists(groupId)

        // check that the group is not logically deleted. By design, for logical deleted group member is exists.
        return if (eGroupExists is Either.Right && eGroupExists.value) {
            if (user.roles.contains(Roles.SUPER_USER)) {
                return Either.Right(GROUP_LEVELS_ADMIN)
            }

            val eUser = groupMemberRepository.load(groupId, user.id)

            when (eUser) {
                is Either.Left ->
                    if (eUser.value is EntityNotFound) {
                        log.info {"SECURITY ${user.id} try to get access into group ${groupId} "}
                        Either.Right(GROUP_LEVELS_NOT_MEMBER)
                    } else {
                        eUser
                    }

                is Either.Right ->
                    when (eUser.value.roleInGroup) {
                        GroupMemberRole.GROUP_ADMIN -> Either.Right(GROUP_LEVELS_ADMIN)
                        GroupMemberRole.MEMBER -> Either.Right(GROUP_LEVELS_MEMBER)
                    }
            }
        } else {
            eGroupExists.map {GROUP_LEVELS_NOT_MEMBER}
        }
    }

    /**
     * Calculate group access levels. It's may be
     */
    fun ideaAccessLevels(idea: Idea, user: User): Either<Exception, Set<IdeaAccessLevel>> {
        return groupAccessLevel(idea.groupId, user).map {groupAccessLevels ->
            when {
                groupAccessLevels.contains(GroupAccessLevel.NOT_MEMBER) -> IDEA_LEVELS_FOR_NOT_GROUP_MEMBER

                groupAccessLevels.contains(GroupAccessLevel.ADMIN) -> IDEA_LEVELS_FOR_GROUP_ADMIN

                groupAccessLevels.contains(GroupAccessLevel.MEMBER) -> {
                    val levels = IDEA_LEVELS_FOR_MEMBER.toMutableSet()
                    if (idea.assignee == user.id) {
                        levels.add(IdeaAccessLevel.ASSIGNEE)
                    }

                    if (idea.author == user.id) {
                        levels.add(IdeaAccessLevel.AUTHOR)
                    }

                    levels
                }

                else -> setOf(IdeaAccessLevel.DENIED)
            }
        }

    }


    fun groupMemberAccessLevels(
        groupMember: GroupMember,
        groupId: String,
        user: User
    ): Either<Exception, Set<GroupMemberAccessLevel>> {
        return if (groupMember.userId == user.id) {
            Either.Right(GROUP_MEMBER_HIM_SELF)
        } else {
            either.eager<Exception, Set<GroupMemberAccessLevel>> {
                // it has a second call to a repository, but the operation is rarely so we don't optimize it
                val groupAccessLevel = groupAccessLevel(groupId, user).bind()
                when {
                    groupAccessLevel.contains(GroupAccessLevel.ADMIN) -> GROUP_MEMBER_LEVELS_FOR_ADMIN
                    groupAccessLevel.contains(GroupAccessLevel.MEMBER) -> GROUP_MEMBER_LEVELS_FOR_MEMBER
                    else -> GROUP_MEMBER_ACCESS_DENIED
                }
            }
        }
    }

}

