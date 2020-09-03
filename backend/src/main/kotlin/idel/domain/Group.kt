package idel.domain

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import java.time.LocalDateTime
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * Editable properties of Group.
 *
 * See properties documentation in the [Group] class.
 */
interface GroupEditableProperties {
    val title: String
    val description: String
    val usersRestrictions: Set<String>
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
         * Regexp with users restriction. May include patterns like domains and other.
         */
        override val usersRestrictions: Set<String>

) : GroupEditableProperties {

    private fun isCorrectRestriction(restriction: String): Option<PatternSyntaxException> {
        return try {
            Pattern.compile(restriction)
            None
        } catch (e: PatternSyntaxException) {
            Some(e)
        }
    }


}

class GroupFactory {
    fun from(creator: UserId, editableProperties: GroupEditableProperties): Group {
        return Group(
                id = generateId(),
                ctime = LocalDateTime.now(),
                creator = creator,
                title = editableProperties.title,
                description = editableProperties.description,
                usersRestrictions = editableProperties.usersRestrictions
        )
    }
}

interface GroupRepository {
    fun add(group: Group)

    fun load(id : String) : Option<Group>

    fun update(id : String, group: Group)
}