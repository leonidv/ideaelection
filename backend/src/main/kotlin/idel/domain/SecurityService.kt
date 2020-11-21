package idel.domain

import arrow.core.Either
import arrow.core.extensions.fx


class OperationNotPermitted : Exception()

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

class SecurityService(
        private val groupMemberRepository: GroupMemberRepository
) {

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

    }

    private fun containsEntity(collection: Collection<out Identifiable>, entityId: String): Boolean {
        return collection.find {it.id == entityId} != null
    }

    private fun <T : Identifiable> containsEntity(collection: Collection<out T>, entity: T): Boolean {
        return containsEntity(collection, entity.id)
    }

    private fun <T : Identifiable> Collection<out T>.has(entity: T) = containsEntity(this, entity)

    fun isSuperUser(user: User): Boolean {
        return user.roles.contains(Roles.SUPER_USER)
    }

    /**
     * Calculate group access level.
     */
    fun groupAccessLevel(group: Group, user: User): Either<Exception, Set<GroupAccessLevel>> {
        return Either.fx {
            val isAdmin = group.administrators.has(user) || isSuperUser(user)
            if (isAdmin) {
                // group admin can do anything that can do member
                GROUP_LEVELS_ADMIN
            } else {
                val (isMember) = groupMemberRepository.isMember(group.id, user.id)
                if (isMember) {
                    GROUP_LEVELS_MEMBER
                } else {
                    GROUP_LEVELS_NOT_MEMBER
                }
            }
        }
    }

    /**
     * Calculate group access levels. It's may be
     */
    fun ideaAccessLevels(group: Group, idea: Idea, user: User): Either<Exception, Set<IdeaAccessLevel>> {
        if (group.id != idea.groupId) {
            return Either.left(IllegalArgumentException("Group doesn't contain idea, groupId = [${group.id}], idea.groupId = [${idea.groupId}]"))
        } else {
            return groupAccessLevel(group, user).map {groupAccessLevels ->
                when {
                    groupAccessLevels.contains(GroupAccessLevel.NOT_MEMBER) -> IDEA_LEVELS_FOR_NOT_GROUP_MEMBER

                    // group admin can do anything in group
                    groupAccessLevels.contains(GroupAccessLevel.ADMIN) -> IDEA_LEVELS_FOR_GROUP_ADMIN


                    groupAccessLevels.contains(GroupAccessLevel.MEMBER) -> {
                        val levels = IDEA_LEVELS_FOR_MEMBER.toMutableSet()
                        if (idea.assignee == user.id) {
                            levels.add(IdeaAccessLevel.ASSIGNEE)
                        }

                        if (idea.offeredBy == user.id) {
                            levels.add(IdeaAccessLevel.AUTHOR)
                        }

                        levels
                    }

                    else -> setOf(IdeaAccessLevel.DENIED)
                }
            }
        }
    }
}

