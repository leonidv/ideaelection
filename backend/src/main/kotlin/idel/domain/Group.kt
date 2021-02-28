package idel.domain


import arrow.core.*

import io.konform.validation.*
import io.konform.validation.Invalid
import io.konform.validation.Valid
import io.konform.validation.jsonschema.maxLength
import io.konform.validation.jsonschema.minLength
import io.konform.validation.jsonschema.pattern
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

        /**
         * Administrators of groups.
         */
        val administrators: List<UserInfo>,

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
                "', logo='" + logo + "', entryMode=" + entryMode + ", administrators=" + administrators + ")"
    }


}


class GroupValidation {
    companion object {

        // https://rgxdb.com/r/1NUN74O6
        private val base64Regex = """data\:image/(png|jpg);base64,(?:[A-Za-z0-9+\/]{4})*""" +
                """(?:[A-Za-z0-9+\/]{2}==|[A-Za-z0-9+\/]{3}=|[A-Za-z0-9+\/]{4})""".toRegex()


        val propertiesValidation = Validation<IGroupEditableProperties> {
            IGroupEditableProperties::name {
                minLength(3)
                maxLength(255)
            }

            IGroupEditableProperties::description {
                minLength(1)
                maxLength(300)
            }

            IGroupEditableProperties::logo {
                pattern(base64Regex)
            }
        }
    }
}


class GroupFactory {
    fun createGroup(
            creator: UserInfo,
            properties: IGroupEditableProperties,
            administrators: List<UserInfo>
    ): Either<Invalid<IGroupEditableProperties>, Group> {
        val validationResult: ValidationResult<IGroupEditableProperties> = GroupValidation.propertiesValidation(properties)

        return when (validationResult) {
            is Invalid -> Either.left(validationResult)
            is Valid -> {
                val group = Group(
                        id = generateId(),
                        ctime = LocalDateTime.now(),
                        creator = creator,
                        name = properties.name,
                        description = properties.description,
                        logo = properties.logo,
                        entryMode = properties.entryMode,
                        administrators = administrators + creator
                )
                Either.right(group)
            }
        }
    }
}

enum class GroupOrdering {
    CTIME_ASC,
    CTIME_DESC,
    TITLE_ASC,
    TITLE_DESC
}

data class GroupFiltering(

        /**
         * Filter groups with member
         */
        val member: Option<UserId>
)

interface GroupRepository {
    fun add(entity: Group): Either<Exception, Group>

    fun load(id: String): Either<Exception, Group>

    fun replace(entity: Group): Either<Exception, Group>

    /**
     * Load user's groups.
     *
     * In fact, the best place for this method is [GroupMemberRepository], but it's required too hard refactoring.
     */
    fun loadByUser(userId: String, pagination: Repository.Pagination, ordering: GroupOrdering): Either<Exception, List<Group>>

    /**
     * Loads only available and visible groups.
     */
    fun loadOnlyAvailable(pagination: Repository.Pagination, ordering: GroupOrdering): Either<Exception, List<Group>>

}