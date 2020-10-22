package idel.domain


import arrow.core.*

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
    val administrators: Set<UserId>
}

class GroupEditableProperties(
        override val title: String,
        override val description: String,
        override val entryMode: GroupEntryMode,
        override val administrators: Set<UserId>,
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
        val id: String,

        /**
         * Time of the creation
         */
        val ctime: LocalDateTime,

        /**
         * User which created the group
         */
        val creator: UserId,

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
        override val administrators: Set<UserId>,


        ) : IGroupEditableProperties {


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

            IGroupEditableProperties::administrators {
                minItems(1)
            }

        }

        val groupValidation = Validation<Group> {
            Group::creator {
                minLength(3)
                maxLength(255)
            }

        }
    }
}

class GroupFactory {
    fun createGroup(creatorId: UserId, properties: IGroupEditableProperties): Either<Invalid<IGroupEditableProperties>, Group> {
        val validationResult = GroupValidation.propertiesValidation(properties)

        return when (validationResult) {
            is Invalid -> Either.left(validationResult)
            is Valid -> {
                val group = Group(
                        id = generateId(),
                        ctime = LocalDateTime.now(),
                        creator = creatorId,
                        title = properties.title,
                        description = properties.description,
                        entryMode = properties.entryMode,
                        administrators = properties.administrators + creatorId
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
         *  Filter only groups which are available for joining.
         */
        val onlyAvailable: Boolean
)

interface GroupRepository {
    fun add(group: Group)

    fun load(id: String):  Either<Exception,Option<Group>>

    fun replace(group: Group)

    /**
     * [GroupFiltering.onlyAvailable] processed only if is true. Load NO available groups is senselessly and not secure.
     */
    fun load(first: Int, last: Int, sorting: GroupSorting, filtering: GroupFiltering): List<Group>
}