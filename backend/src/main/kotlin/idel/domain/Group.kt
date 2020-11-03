package idel.domain


import arrow.core.*
import idel.api.Repository

import io.konform.validation.*
import io.konform.validation.Invalid
import io.konform.validation.Valid
import io.konform.validation.jsonschema.maxLength
import io.konform.validation.jsonschema.minItems
import io.konform.validation.jsonschema.minLength
import java.time.LocalDateTime

/**
 * Editable properties of Group.
 *
 * See properties documentation in the [Group] class.
 */
interface IGroupEditableProperties {
    val title: String
    val description: String
    val entryMode: GroupEntryMode
}

class GroupEditableProperties(
        override val title: String,
        override val description: String,
        override val entryMode: GroupEntryMode,
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
         * Name of group.
         */
        override val title: String,

        /**
         * Short description of group.
         */
        override val description: String,

        /**
         * Regulate how user's can join to group.
         */
        override val entryMode: GroupEntryMode,

        /**
         * Administrators of groups.
         */
        val administrators: List<UserInfo>,

        /**
         * Group's members
         */
        val members: List<GroupMember>

) : IGroupEditableProperties, Identifiable {


    fun clone(members: List<GroupMember> = this.members): Group {
        return Group(
                id = this.id,
                ctime = this.ctime,
                creator = this.creator,
                title = this.title,
                description = this.description,
                entryMode = this.entryMode,
                administrators = this.administrators,
                members = members
        )
    }


    fun addMember(user: GroupMember): Group {
        val newMembers = members + user
        return clone(members = newMembers)
    }

    fun removeMember(userId: UserId): Group {
        val newMembers = members.filterNot {it.id == userId}.toList()
        return clone(members = newMembers)
    }
}

data class GroupMember(
        override val id: String,
        val ctime: LocalDateTime,
        override val email: String,
        override val displayName: String,
        override val avatar: String,
) : IUserInfo {
    companion object {
        fun of(userInfo: IUserInfo): GroupMember {
            return GroupMember(
                    id = userInfo.id,
                    ctime = LocalDateTime.now(),
                    email = userInfo.email,
                    displayName = userInfo.displayName,
                    avatar = userInfo.avatar
            )
        }
    }
}

class GroupValidation {
    companion object {

        val propertiesValidation = Validation<IGroupEditableProperties> {
            IGroupEditableProperties::title {
                minLength(3)
                maxLength(255)
            }

            IGroupEditableProperties::description {
                minLength(1)
                maxLength(300)
            }
        }
    }
}


class GroupFactory {
    fun createGroup(
            creator: UserInfo,
            properties: IGroupEditableProperties,
            administrators: List<UserInfo>,
            members: List<UserInfo>
    ): Either<Invalid<IGroupEditableProperties>, Group> {
        val validationResult = GroupValidation.propertiesValidation(properties)

        return when (validationResult) {
            is Invalid -> Either.left(validationResult)
            is Valid -> {
                val group = Group(
                        id = generateId(),
                        ctime = LocalDateTime.now(),
                        creator = creator,
                        title = properties.title,
                        description = properties.description,
                        entryMode = properties.entryMode,
                        administrators = administrators + creator,
                        members = members.map {GroupMember.of(it)}
                )
                Either.right(group)
            }
        }
    }
}

enum class GroupSorting {
    CTIME_ASC,
    CTIME_DESC
}

data class GroupFiltering(

        /**
         * Filter groups with member
         */
        val member: Option<UserId>
)

interface GroupRepository {
    fun add(group: Group): Either<Exception, Group>

    fun load(id: String): Either<Exception, Option<Group>>

    /**
     *  Case-specific function. Used for adding members to group. It allows dont' load full group with all members from
     *  database.
     */
    fun loadEntryMode(id: String): Either<Exception, GroupEntryMode>

    fun replace(group: Group)

    fun load(first: Int, last: Int, sorting: GroupSorting, filtering: GroupFiltering): Either<Exception, List<Group>>

    /**
     * Loads only available and visible groups.
     */
    fun loadOnlyAvailable(pagination: Repository.Pagination, sorting: GroupSorting): Either<Exception, List<Group>>

    /**
     * Add member to group. Granular operation for performance.
     */
    fun addMember(groupId: String, member: GroupMember): Either<Exception, Unit>;

}