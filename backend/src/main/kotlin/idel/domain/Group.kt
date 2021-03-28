package idel.domain


import arrow.core.*
import arrow.core.extensions.either.monad.flatten
import arrow.core.extensions.fx

import io.konform.validation.*
import io.konform.validation.jsonschema.maxLength
import io.konform.validation.jsonschema.minLength
import java.time.LocalDateTime

/**
 * Editable properties of Group.
 *
 * See properties documentation in the [Group] class.
 */
interface IGroupEditableProperties {
    val name: String
    val description: String
    val entryMode: GroupEntryMode
    val logo: String
}

class GroupEditableProperties(
    override val name: String,
    override val description: String,
    override val entryMode: GroupEntryMode,
    override val logo: String
) : IGroupEditableProperties {
}

enum class GroupEntryMode {
    /**
     * Anybody can join a group without approving by group's administrator.
     */
    PUBLIC,

    /**
     * Anybody can try to join a group, but group's admin should accept join request.
     */
    CLOSED,

    /**
     * Only admin can invite users to group.
     */
    PRIVATE
}

enum class GroupState {
    /**
     * Group is available for working.
     */
    ACTIVE,

    /**
     * User deleted group and can't work with it.
     */
    DELETED
}
/**
 * Group is binding between users and ideas.
 *
 * Group allows to share ideas between specific users.
 */
class Group(
    /**
     * Generated idenitfier.
     */
    override val id: String,

    /**
     * Time of the creation
     */
    val ctime: LocalDateTime,

    /**
     * User which created the group
     */
    val creator: UserInfo,

    /**
     * State of group (active, archived, etc).
     */
    val state : GroupState,

    /**
     * Name of group.
     */
    override val name: String,

    /**
     * Short description of group.
     */
    override val description: String,

    /**
     * Logotype of group. Link to image.
     */
    override val logo: String,

    /**
     * Regulate how user's can join to group.
     */
    override val entryMode: GroupEntryMode,

    ) : IGroupEditableProperties, Identifiable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Group

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Group(id='" + id +
                "', ctime=" + ctime + ", creator=" + creator + ", name='" + name + "', description='" + description +
                "', logo='" + logo + "', entryMode=" + entryMode + ")"
    }

    private fun clone(
        state : GroupState = this.state,
        entryMode: GroupEntryMode = this.entryMode,
        description: String = this.description,
        logo : String = this.logo,
        name : String = this.name
    ) : Group {
        return Group(
            id = this.id,
            creator = this.creator,
            ctime = this.ctime,
            state =state,
            entryMode = entryMode,
            description = description,
            logo = logo,
            name = name
        )
    }

    fun update(properties: IGroupEditableProperties) : Either<ValidationException, Group> {
        return GroupValidation.ifValid(properties) {
            clone(
                entryMode = properties.entryMode,
                description = properties.description,
                logo = properties.logo,
                name = properties.name
            )
        }
    }

    /**
     * Logical delete. Change status to [[GroupState.DELETED]] which is mean that user can't work with group.
     */
    fun delete() : Group {
        return this.clone(state = GroupState.DELETED)
    }

    fun isDeleted() : Boolean {
        return state == GroupState.DELETED
    }
}


class GroupValidation {
    companion object : Validator<IGroupEditableProperties> {

        override val validation = Validation<IGroupEditableProperties> {
            IGroupEditableProperties::name {
                minLength(3)
                maxLength(255)
            }

            IGroupEditableProperties::description {
                minLength(1)
                maxLength(300)
            }

            IGroupEditableProperties::logo {
                maxLength(150_000) // approx 100kb in base64
                isImageBase64(allowEmptyValue = false)
            }
        }
    }

}


class GroupFactory {
    fun createGroup(
        creator: UserInfo,
        properties: IGroupEditableProperties
    ): Either<ValidationException, Group> {
        return GroupValidation.ifValid(properties) {
            Group(
                id = generateId(),
                ctime = LocalDateTime.now(),
                creator = creator,
                state = GroupState.ACTIVE,
                name = properties.name,
                description = properties.description,
                logo = properties.logo,
                entryMode = properties.entryMode
            )
        }
    }
}

enum class GroupOrdering {
    CTIME_ASC,
    CTIME_DESC,
    TITLE_ASC,
    TITLE_DESC
}

interface GroupRepository : BaseRepository<Group>, CouchbaseTransactionBaseRepository<Group> {

    /**
     * Load user's groups.
     *
     * In fact, the best place for this method is [GroupMemberRepository], but it's required too hard refactoring.
     */
    fun loadByUser(
        userId: String,
        pagination: Repository.Pagination,
        ordering: GroupOrdering
    ): Either<Exception, List<Group>>

    /**
     * Loads only available and visible groups.
     */
    fun loadOnlyAvailable(pagination: Repository.Pagination, ordering: GroupOrdering): Either<Exception, List<Group>>

}


/**
 * It's not good solution. This is methods should be in the [Group], but then repositories will be persisted into DB :)
 * Should be refactoring after migrating to PostgreSQL
 */
class GroupService(val groupMemberRepository: GroupMemberRepository) {

    private fun checkNoLastAdmin(groupId: String): Either<Exception, Boolean> {
        val pagination = Repository.Pagination(0, 2)
        val admins = groupMemberRepository.loadByGroup(
            groupId,
            pagination,
            roleFilter = Option.just(GroupMemberRole.GROUP_ADMIN)
        )

        return admins.map {it.size > 1}
    }

    fun changeRoleInGroup(groupId: String, userId: String, nextRole: GroupMemberRole): Either<Exception, GroupMember> {
        return Either.fx<Exception, Either<Exception, GroupMember>> {
            val (canChange) = when (nextRole) {
                GroupMemberRole.GROUP_ADMIN -> Either.right(true)
                GroupMemberRole.MEMBER -> checkNoLastAdmin(groupId)
            }

            if (canChange) {
                  val (member) = groupMemberRepository.load(groupId, userId )
                  groupMemberRepository.changeRole(member.changeRole(nextRole))
            } else {
                Either.left(InvalidOperation("Group should contains at least one admin, you try to remove last admin"))
            }

        }.flatten()
    }
}
