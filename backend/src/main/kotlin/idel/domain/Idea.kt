package idel.domain

import arrow.core.Either
import arrow.core.continuations.either
import arrow.core.flatten
import idel.api.DataOrError
import idel.domain.security.IdeaAccessLevel
import idel.domain.security.SecurityService
import io.konform.validation.Validation
import io.konform.validation.jsonschema.maxLength
import io.konform.validation.jsonschema.minLength
import mu.KLogger
import java.time.LocalDateTime
import java.util.*

typealias IdeaId = UUID

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
 *  Idea - main concept of application.
 *
 *  Don't use this constructor directly, instead call [IdeaFactory.createIdea]
 */
class Idea(
    /**
     * Generated identifier
     */
    val id: UUID,

    /**
     * Idea's group.
     */
    val groupId: GroupId,

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
    val assignee: UserId?,

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
) : IIdeaEditableProperties {

    init {
        require(summary.isNotBlank()) {"title can't be blank"}
    }


    /**
     * Add voter which add his vote for this idea.
     *
     * User, which offered an idea, can't vote for it. In this case the method returns an Idea without any changes)
     */
    fun addVote(userId: UserId): Either<EntityCantBeChanged, Idea> {
        if (voters.contains(userId)) {
            return Either.Right(this)
        }
        val newVoters = this.voters.plus(userId)
        return this.clone(voters = newVoters)
    }

    /**
     * Remove voter's vote from this idea.
     */
    fun removeVote(userId: UserId): Either<EntityCantBeChanged, Idea> {
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
        groupId: GroupId = this.groupId,
        description: String = this.description,
        descriptionPlainText: String = this.descriptionPlainText,
        link: String = this.link,
        assignee: UserId? = this.assignee,
        implemented: Boolean = this.implemented,
        offeredBy: UserId = this.author,
        voters: List<UserId> = this.voters,
        archived: Boolean = this.archived,
        deleted: Boolean = this.deleted
    ): Either<EntityCantBeChanged, Idea> {
        val shouldRestoreFromArchive = this.archived && !archived
        val shouldRestoreFromDeleted = this.deleted && !deleted

        return if (this.deleted && !shouldRestoreFromDeleted) {
            Either.Left(EntityLogicallyDeleted)
        } else if ((this.archived && !shouldRestoreFromArchive) && !deleted) {
            Either.Left(EntityArchived)
        } else {
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
                    deleted = deleted
                )
            )
        }
    }

    private fun isAdmin(changerLevels: Set<IdeaAccessLevel>) = changerLevels.contains(IdeaAccessLevel.GROUP_ADMIN)
    private fun isMember(changerLevels: Set<IdeaAccessLevel>) = changerLevels.contains(IdeaAccessLevel.GROUP_MEMBER)
    private fun isAuthor(changerLevels: Set<IdeaAccessLevel>) = changerLevels.contains(IdeaAccessLevel.AUTHOR)
    private fun isAssignee(changerLevels: Set<IdeaAccessLevel>) = changerLevels.contains(IdeaAccessLevel.ASSIGNEE)

    fun assign(newAssignee: UserId, changerLevels: Set<IdeaAccessLevel>): Either<DomainError, Idea> {

        val canAssign = when {
            hasAssignee() && isAdmin(changerLevels) -> true
            !hasAssignee() && isMember(changerLevels) -> true
            else -> false
        }

        return if (canAssign) {
            this.clone(assignee = newAssignee)
        } else {
            Either.Left(OperationNotPermitted())
        }
    }

    fun removeAssign(changerLevels: Set<IdeaAccessLevel>): Either<DomainError, Idea> {
        return if (isAssignee(changerLevels) || isAdmin(changerLevels)) {
            this.clone(assignee = null)
        } else {
            Either.Left(OperationNotPermitted())
        }
    }


    fun hasAssignee(): Boolean {
        return this.assignee != null
    }

    fun implement(changerLevels: Set<IdeaAccessLevel>): Either<DomainError, Idea> {
        return if (isAdmin(changerLevels) || isAssignee(changerLevels)) {
            this.clone(implemented = true)
        } else {
            Either.Left(OperationNotPermitted())
        }
    }

    fun notImplement(changerLevels: Set<IdeaAccessLevel>): Either<DomainError, Idea> {
        return if (isAdmin(changerLevels) || isAssignee(changerLevels)) {
            this.clone(implemented = false)
        } else {
            Either.Left(OperationNotPermitted())
        }
    }

    fun updateInformation(
        properties: IIdeaEditableProperties,
        changerLevels: Set<IdeaAccessLevel>
    ): Either<DomainError, Idea> {
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
    fun changeGroup(groupId: GroupId, changerLevels: Set<IdeaAccessLevel>): Either<DomainError, Idea> {
        return when {
            isAdmin(changerLevels) ||
                    ((hasAssignee() && isAssignee(changerLevels) || isAuthor(changerLevels))) ->
                clone(groupId = groupId)
            else -> Either.Left(OperationNotPermitted())
        }
    }

    /**
     * Archive idea
     */
    fun archive(changerLevels: Set<IdeaAccessLevel>): Either<DomainError, Idea> {
        return if (isAdmin(changerLevels) || isAssignee(changerLevels) || isAuthor(changerLevels)) {
            clone(archived = true)
        } else {
            Either.Left(OperationNotPermitted())
        }
    }

    /**
     * Restore idea from archive
     */
    fun unArchive(changerLevels: Set<IdeaAccessLevel>): Either<DomainError, Idea> {
        return if (isAdmin(changerLevels) || isAssignee(changerLevels) || isAuthor(changerLevels)) {
            clone(archived = false)
        } else {
            Either.Left(OperationNotPermitted())
        }
    }

    /**
     * Logically delete idea
     */
    fun delete(changerLevels: Set<IdeaAccessLevel>): Either<DomainError, Idea> {
        return when {
            votesCount() > 0 && isAdmin(changerLevels) -> {
                clone(deleted = true)
            }

            votesCount() == 0 && (isAdmin(changerLevels) || isAuthor(changerLevels)) -> {
                clone(deleted = true)
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
    fun createIdea(properties: IIdeaEditableProperties, groupId: GroupId, userId: UserId):
            Either<ValidationError, Idea> {
        return IdeaValidation.ifValid(properties) {
            Idea(
                id = UUID.randomUUID(),
                groupId = groupId,
                summary = properties.summary,
                description = properties.description,
                descriptionPlainText = properties.descriptionPlainText,
                link = properties.link,
                implemented = false,
                assignee = null,
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

fun requireNullOrNotBlank(opt: String?, field: String) {
    opt?.let {
        require(opt.isNotBlank()) {"$field is empty string, but should be Optional.empty()"}
    }
}

data class IdeaFiltering(
    val author: UserId?,
    val implemented: Boolean?,
    val assignee: UserId?,
    val text: String?,
    val votedBy: UserId?,
    /**
     * If true, a result list includes deleted ideas
     */
    val listDeleted: Boolean,

    /**
     * If true, a result list includes archived ideas
     */
    val listArchived: Boolean
) {
    init {
        requireNullOrNotBlank(text, "text")
    }

}


interface IdeaRepository {

    fun list(
        groupId: GroupId,
        ordering: IdeaOrdering,
        filtering: IdeaFiltering,
        pagination: Repository.Pagination
    ): Either<DomainError, List<Idea>>

    fun load(id: IdeaId): Either<DomainError, Idea>
    fun add(entity: Idea): Either<DomainError, Idea>
//

    fun update(idea: Idea): Either<DomainError, Idea>

    fun update(idea: Either<DomainError, Idea>): Either<DomainError, Idea>
}


/**
 * All functions which are take argument with type [IdeaAction] should call ideaAction in transaction.
 *
 * If a function pass action as argument to another function, wrapped transaction is not required.
 */
typealias IdeaAction<T> = (idea: Idea) -> Either<DomainError, T>

/**
 * All functions which are take argument with type [IdeaActionWithLevels] should call ideaAction in transaction.
 *
 * If a function pass action as argument to another function, wrapped transaction is not required.
 */

typealias IdeaActionWithLevels<T> = (idea: Idea, levels: Set<IdeaAccessLevel>) -> Either<DomainError, T>

class IdeaSecurity(
    private val securityService: SecurityService,
    private val ideaRepository: IdeaRepository,
) {


    fun <T> secure(
        ideaId: IdeaId,
        user: User,
        requiredLevels: Set<IdeaAccessLevel>,
        action: IdeaAction<T>
    ): Either<DomainError, T> {
        return fTransaction {
            either.eager {
                val (idea, levels) = calculateLevels(ideaId, user).bind()
                if (levels.intersect(requiredLevels).isNotEmpty()) {
                    action(idea)
                } else {
                    Either.Left(OperationNotPermitted())
                }
            }
        }.flatten()
    }

    fun <T> withLevels(ideaId: IdeaId, user: User, action: IdeaActionWithLevels<T>): Either<DomainError, T> {
        return fTransaction {
            either.eager<DomainError, Either<DomainError, T>> {
                val (idea, levels) = calculateLevels(ideaId, user).bind()
                action(idea, levels)
            }.flatten()
        }
    }


    fun calculateLevels(ideaId: IdeaId, user: User): Either<DomainError, Pair<Idea, Set<IdeaAccessLevel>>> {
        return fTransaction {
            either.eager {
                val idea = ideaRepository.load(ideaId).bind()
                val levels = securityService.ideaAccessLevels(idea, user).bind()
                Pair(idea, levels)
            }
        }
    }

    fun <T> asMember(ideaId: IdeaId, user: User, action: IdeaAction<T>): Either<DomainError, T> {
        return secure(ideaId, user, setOf(IdeaAccessLevel.GROUP_MEMBER), action)
    }

    fun <T> asEditor(ideaId: IdeaId, user: User, action: IdeaAction<T>): Either<DomainError, T> {
        return secure(ideaId, user, setOf(IdeaAccessLevel.ASSIGNEE, IdeaAccessLevel.AUTHOR), action)
    }

}