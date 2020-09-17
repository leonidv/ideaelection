package idel.domain


import arrow.core.*

import io.konform.validation.*
import io.konform.validation.Invalid
import io.konform.validation.Valid
import io.konform.validation.jsonschema.maxLength
import io.konform.validation.jsonschema.minItems
import io.konform.validation.jsonschema.minLength
import java.time.LocalDateTime
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * Editable properties of Group.
 *
 * See properties documentation in the [Group] class.
 */
interface IGroupEditableProperties {
    companion object {

    }

    val title: String
    val description: String
    val usersRestrictions: List<String>
}

class GroupEditableProperties(
        override val title: String,
        override val description: String,
        override val usersRestrictions: List<String>) : IGroupEditableProperties {
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
        override val usersRestrictions: List<String>

) : IGroupEditableProperties {

    private val usersRestrictionsRegexp: List<Regex>

    init {
        val validationErrors = GroupValidation.validate(this)
        validationErrors.count()
        if (!validationErrors.isEmpty()) {
            throw ValidationException("Initial value of Group is incorrect", validationErrors)
        }

        usersRestrictionsRegexp = usersRestrictions.map {it.toRegex(RegexOption.IGNORE_CASE)}
    }

    /**
     * User satisfies the group's restrictions.
     */
    fun allowToJoin(user: User): Boolean {
        return usersRestrictionsRegexp.firstOrNone {it.matches(user.email)}.isDefined()
    }

}

class GroupValidation {
    companion object {
        fun ValidationBuilder<String>.isRegexp(): Constraint<String> {
            return addConstraint(
                    errorMessage = "Should be valid regexp"
            ) {value ->
                try {
                    Pattern.compile(value)
                    true
                } catch (e: PatternSyntaxException) {
                    false
                }
            }
        }

        val propertiesValidation = Validation<IGroupEditableProperties> {
            IGroupEditableProperties::title {
                minLength(3)
                maxLength(255)
            }

            IGroupEditableProperties::description {
                minLength(1)
                maxLength(300)
            }

            IGroupEditableProperties::usersRestrictions {
                minItems(1)
            }

            IGroupEditableProperties::usersRestrictions onEach {
                isRegexp()
            }
        }

        val groupValidation = Validation<Group> {
            Group::creator {
                minLength(3)
                maxLength(255)
            }

        }

        fun validate(group: Group): Collection<ValidationError> {
            val validationResult = listOf(
                    propertiesValidation(group), groupValidation(group)
            )
                .filterIsInstance<Invalid<*>>()
                .map {it.errors}
                .flatten()

            return validationResult
        }
    }
}

class GroupFactory {
    fun createGroup(creator: UserId, editableProperties: IGroupEditableProperties): Either<Invalid<IGroupEditableProperties>, Group> {
        val validationResult = GroupValidation.propertiesValidation(editableProperties)

        return when (validationResult) {
            is Invalid -> Either.left(validationResult)
            is Valid -> {
                val group = Group(
                        id = generateId(),
                        ctime = LocalDateTime.now(),
                        creator = creator,
                        title = editableProperties.title,
                        description = editableProperties.description,
                        usersRestrictions = editableProperties.usersRestrictions
                )
                Either.right(group)
            }
        }

    }
}

interface GroupRepository {
    fun add(group: Group)

    fun load(id: String): Option<Group>

    fun replace(group: Group)
}