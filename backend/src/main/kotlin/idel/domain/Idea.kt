package idel.domain

import arrow.core.Either
import arrow.core.Option
import arrow.core.Some
import io.konform.validation.Validation
import io.konform.validation.jsonschema.maxLength
import io.konform.validation.jsonschema.minLength
import java.time.LocalDateTime
import java.util.*

/**
 * Summary information about Idea
 */
interface IIdeaEditableProperties {
    val summary: String
    val description: String
    val descriptionPlainText: String
    val link: String
}

class IdeaEditableProperties(
    override var summary: String,
    override var description: String,
    override val descriptionPlainText: String,
    override var link: String
) : IIdeaEditableProperties

/**
 * Identifier of not assigned user.
 */
const val NOT_ASSIGNED: String = ""

/**
 *  Idea - main concept of application.
 *
 *  Don't use this constructor directly, instead call [IdeaFactory.createIdea]
 */
class Idea(
    /**
     * Generated identifier
     */
    override val id: String,

    /**
     * Idea's group.
     */
    val groupId: String,

    /**
     * Time of creation.
     */
    val ctime: LocalDateTime,

    /**
     * Title of idea
     */
    override val summary: String,
    /**
     * Description of a idea in markup language.
     */
    override val description: String,

    /**
     * Description of a idea in the plain text format. For indexing and searching.
     */
    override val descriptionPlainText: String,
    /**
     * Link to external resource, like Confluence page or Google Doc
     */
    override val link: String,

    /**
     * User which is assigned to implement this Idea.
     */
    val assignee: UserId,

    /**
     * Idea is done. And it is very cool!
     */
    val implemented: Boolean,

    /**
     * User which have offered this idea.
     */
    val author: UserId,

    /**
     * Voters which give their voice for this idea.
     */
    val voters: List<UserId>,

    /**
     * Idea is in archive and don't visible by default in the lists.
     */
    val archived: Boolean,

    /**
     * Idea is logically deleted, don't visible for users.
     */
    val deleted: Boolean
) : IIdeaEditableProperties, Identifiable {

    init {
        require(id.isNotBlank()) {"id can't be blank"}
        require(summary.isNotBlank()) {"title can't be blank"}
        require(!author.isBlank()) {"author can't be blank"}
    }


    /**
     * Add voter which add his vote for this idea.
     *
     * User, which offered an idea, can't vote for it. In this case the method returns an Idea without any changes)
     */
    fun addVote(userId: UserId): Either<EntityLogicallyDeleted,Idea> {
        if (voters.contains(userId)) {
            return Either.Right(this)
        }
        val newVoters = this.voters.plus(userId)
        return this.clone(voters = newVoters)
    }

    /**
     * Remove voter's vote from this idea.
     */
    fun removeVote(userId: UserId): Either<EntityLogicallyDeleted, Idea> {
        val newVoters = this.voters.minus(userId)
        return this.clone(voters = newVoters)
    }

    /**
     * Total votes which were given for this idea.
     */
    fun votesCount(): Int {
        return this.voters.size
    }

    /**
     * Checks that idea is not logically deleted.
     */
    private fun clone(
        summary: String = this.summary,
        groupId: String = this.groupId,
        description: String = this.description,
        descriptionPlainText: String = this.descriptionPlainText,
        link: String = this.link,
        assignee: String = this.assignee,
        implemented: Boolean = this.implemented,
        offeredBy: String = this.author,
        voters: List<String> = this.voters,
        archived: Boolean = this.archived,
        deleted: Boolean = this.deleted,
        restoreFromDeleted : Boolean = false
    ): Either<EntityLogicallyDeleted, Idea> {
        return if (!this.deleted || restoreFromDeleted) {
            Either.Right(
                Idea(
                    id = id,
                    groupId = groupId,
                    ctime = ctime,
                    summary = summary,
                    description = description,
                    descriptionPlainText = descriptionPlainText,
                    link = link,
                    assignee = assignee,
                    implemented = implemented,
                    author = offeredBy,
                    voters = voters,
                    archived = archived,
                    deleted = deleted || restoreFromDeleted
                )
            )
        } else {
            Either.Left(EntityLogicallyDeleted())
        }
    }

    private fun isAdmin(changerLevels: Set<IdeaAccessLevel>) = changerLevels.contains(IdeaAccessLevel.GROUP_ADMIN)
    private fun isMember(changerLevels: Set<IdeaAccessLevel>) = changerLevels.contains(IdeaAccessLevel.GROUP_MEMBER)
    private fun isAuthor(changerLevels: Set<IdeaAccessLevel>) = changerLevels.contains(IdeaAccessLevel.AUTHOR)
    private fun isAssignee(changerLevels: Set<IdeaAccessLevel>) = changerLevels.contains(IdeaAccessLevel.ASSIGNEE)

    fun assign(newAssignee: String, changerLevels: Set<IdeaAccessLevel>): Either<Exception, Idea> {

        val canAssign = when {
            hasAssignee() && isAdmin(changerLevels) -> true
            !hasAssignee() && isMember(changerLevels) -> true
            else -> false
        }

        return if (newAssignee != NOT_ASSIGNED && canAssign) {
            this.clone(assignee = newAssignee)
        } else {
            Either.Left(OperationNotPermitted())
        }
    }

    fun removeAssign(changerLevels: Set<IdeaAccessLevel>): Either<Exception, Idea> {
        return if (isAssignee(changerLevels) || isAdmin(changerLevels)) {
            this.clone(assignee = NOT_ASSIGNED)
        } else {
            Either.Left(OperationNotPermitted())
        }
    }


    fun hasAssignee(): Boolean {
        return this.assignee != NOT_ASSIGNED
    }

    fun implement(changerLevels: Set<IdeaAccessLevel>): Either<Exception, Idea> {
        return if (isAdmin(changerLevels) || isAssignee(changerLevels)) {
            this.clone(implemented = true)
        } else {
            Either.Left(OperationNotPermitted())
        }
    }

    fun notImplement(changerLevels: Set<IdeaAccessLevel>): Either<Exception, Idea> {
        return if (isAdmin(changerLevels) || isAssignee(changerLevels)) {
            this.clone(implemented = false)
        } else {
            Either.Left(OperationNotPermitted())
        }
    }

    fun updateInformation(
        properties: IIdeaEditableProperties,
        changerLevels: Set<IdeaAccessLevel>
    ): Either<Exception, Idea> {
        val canUpdate = when {
            isAdmin(changerLevels) -> true

            !this.implemented -> {
                if (this.hasAssignee()) {
                    isAssignee(changerLevels)
                } else {
                    changerLevels.contains(IdeaAccessLevel.AUTHOR)
                }
            }
            else -> false

        }

        return if (canUpdate) {
            IdeaValidation.ifValidEither(properties) {
                this.clone(
                    summary = properties.summary,
                    description = properties.description,
                    descriptionPlainText = properties.descriptionPlainText,
                    link = properties.link
                )
            }
        } else {
            Either.Left(OperationNotPermitted())
        }
    }


    /**
     * Moves idea to another group.
     */
    fun changeGroup(groupId: String): Either<Exception, Idea> {
        return clone(groupId = groupId)
    }

    /**
     * Archive idea
     */
    fun archive(changerLevels: Set<IdeaAccessLevel>): Either<OperationNotPermitted, Idea> {
        return if (isAdmin(changerLevels) || isAssignee(changerLevels) || isAuthor(changerLevels)) {
            clone(archived = true).mapLeft {OperationNotPermitted()}
        } else {
            Either.Left(OperationNotPermitted())
        }
    }

    /**
     * Restore idea from archive
     */
    fun unArchive(changerLevels: Set<IdeaAccessLevel>): Either<OperationNotPermitted, Idea> {
        return if (isAdmin(changerLevels) || isAssignee(changerLevels) || isAuthor(changerLevels)) {
            clone(archived = false).mapLeft {OperationNotPermitted()}
        } else {
            Either.Left(OperationNotPermitted())
        }
    }

    /**
     * Logically delete idea
     */
    fun delete(changerLevels: Set<IdeaAccessLevel>): Either<OperationNotPermitted, Idea> {
        return when {
            votesCount() > 0 && isAdmin(changerLevels) -> {
                clone(deleted = true).mapLeft {OperationNotPermitted()}
            }

            votesCount() == 0 && (isAdmin(changerLevels) || isAuthor(changerLevels)) -> {
                clone(deleted = true).mapLeft {OperationNotPermitted()}
            }

            else -> {
                Either.Left(OperationNotPermitted())
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Idea

        if (id != other.id) return false
        if (summary != other.summary) return false
        if (description != other.description) return false
        if (link != other.link) return false
        if (assignee != other.assignee) return false
        if (implemented != other.implemented) return false
        if (author != other.author) return false
        if (voters != other.voters) return false
        if (ctime != other.ctime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + summary.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + link.hashCode()
        result = 31 * result + assignee.hashCode()
        result = 31 * result + implemented.hashCode()
        result = 31 * result + author.hashCode()
        result = 31 * result + voters.hashCode()
        result = 31 * result + ctime.hashCode()
        return result
    }


}

class IdeaValidation {
    companion object : Validator<IIdeaEditableProperties> {
        override val validation = Validation<IIdeaEditableProperties> {
            IIdeaEditableProperties::summary required {
                minLength(3)
                maxLength(255)
            }

            IIdeaEditableProperties::description required {
                minLength(3)
                maxLength(10000)
            }

            IIdeaEditableProperties::descriptionPlainText required {
                minLength(3)
                maxLength(2000)
            }

            IIdeaEditableProperties::link {
                isUrl(allowEmptyValue = true)
            }
        }
    }
}

class IdeaFactory {
    fun createIdea(properties: IIdeaEditableProperties, groupId: String, userId: String):
            Either<ValidationException, Idea> {
        return IdeaValidation.ifValid(properties) {
            Idea(
                id = generateId(),
                groupId = groupId,
                summary = properties.summary,
                description = properties.description,
                descriptionPlainText = properties.descriptionPlainText,
                link = properties.link,
                implemented = false,
                assignee = NOT_ASSIGNED,
                author = userId,
                voters = emptyList(),
                ctime = LocalDateTime.now(),
                archived = false,
                deleted = false
            )
        }
    }
}


/**
 * Possible Idea's sorting
 */
enum class IdeaOrdering {
    CTIME_ASC,
    CTIME_DESC,
    VOTES_DESC
}

fun requireNoneOrNotEmptyValue(opt: Option<String>, field: String) {
    if (opt is Some) {
        require(opt.value.isNotBlank()) {"$field is empty string, but should be Optional.empty()"}
    }
}

data class IdeaFiltering(
    val offeredBy: Option<String>,
    val implemented: Option<Boolean>,
    val assignee: Option<String>,
    val text: Option<String>,
    val deleted: Boolean,
    val archived: Boolean
) {
    init {
        requireNoneOrNotEmptyValue(offeredBy, "offeredBy")
        requireNoneOrNotEmptyValue(assignee, "assigned")
        requireNoneOrNotEmptyValue(text, "text")
    }

}

/**
 * The idea with Couchbase CAS for safely updating.
 * It's not domain level.
 */
@Deprecated("Will be removed after migration into Postgresql")
data class IdeaWithVersion(val idea: Idea, val version: Long)


interface IdeaRepository : BaseRepository<Idea> {

    fun load(groupId: String,
             ordering: IdeaOrdering,
             filtering: IdeaFiltering,
             pagination: Repository.Pagination):
            Either<Exception, List<Idea>>

    fun loadWithVersion(id: String): Optional<IdeaWithVersion>
}