package idel.domain

import arrow.core.*
import java.time.LocalDateTime
import java.util.*

/**
 * Summary information about Idea
 */
interface IIdeaEditableProperties {
    val groupId: String
    val title: String
    val description: String
    val link: String
}

class IdeaEditableProperties(
        override val groupId: String,
        override var title: String,
        override var description: String,
        override var link: String
) : IIdeaEditableProperties

/**
 * Identifier of not assigned user.
 */
const val NOT_ASSIGNED: UserId = ""

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

        override val groupId: String,

        /**
     * Time of creation.
     */
    val ctime: LocalDateTime,

        /**
     * Title of idea
     */
    override val title: String,
        /**
     * Description of idea
     */
    override val description: String,
        /**
     * Link to external resource, like Confluence page or Google Doc
     */
    override val link: String,

        /**
     * User which is assigned to implement this Idea.
     */
    val assignee: UserId,

        /**
     * Idea is implemented. And it is very cool!
     */
    val implemented: Boolean,

        /**
     * Voter which have offered this idea.
     */
    val author: UserId,

        /**
     * Voters which give their voice for this idea.
     */
    val voters: Set<UserId>,

        ) : IIdeaEditableProperties, Identifiable {

    init {
        require(id.isNotBlank()) { "id can't be blank" }
        require(title.isNotBlank()) { "title can't be blank" }
        require(!voters.contains(author)) { "user can't vote his idea" }
        require(!author.isBlank()) { "offeredBy can't be blank" }
    }


    /**
     * Add voter which add his vote for this idea.
     *
     * User, which offered an idea, can't vote for it. In this case the method returns an Idea without any changes)
     */
    fun addVote(userId: UserId): Idea {
        if (this.author == userId) {
            return this
        }
        val newVoters = this.voters.plus(userId)
        return this.clone(voters = newVoters)
    }

    /**
     * Remove voter's vote from this idea.
     */
    fun removeVote(userId: UserId): Idea {
        val newVoters = this.voters.minus(userId)
        return this.clone(voters = newVoters)
    }

    /**
     * Total votes which were given for this idea.
     */
    fun votesCount(): Int {
        return this.voters.size
    }

    private fun clone(
            title: String = this.title,
            groupId: String = this.groupId,
            description: String = this.description,
            link: String = this.link,
            assigned: UserId = this.assignee,
            implemented: Boolean = this.implemented,
            offeredBy: UserId = this.author,
            voters: Set<UserId> = this.voters
    ): Idea = Idea(
            id = id,
            groupId = groupId,
            ctime = ctime,
            title = title,
            description = description,
            link = link,
            assignee = assigned,
            implemented = implemented,
            author = offeredBy,
            voters = voters
    )

    fun assign(userId: UserId): Idea {
        return if (this.assignee == userId) {
            this
        } else {
            clone(assigned = userId)
        }
    }

    fun removeAssign(): Idea {
        return this.clone(assigned = NOT_ASSIGNED)
    }

    fun update(
        groupId: String = this.groupId,
        title: String = this.title,
        description: String = this.description,
        link: String = this.link,
        implemented: Boolean = this.implemented
    ): Idea = this.clone(
            title = title,
            groupId = groupId,
            description = description,
            link = link,
            implemented = implemented
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Idea

        if (id != other.id) return false
        if (title != other.title) return false
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
        result = 31 * result + title.hashCode()
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

class IdeaFactory {
    fun createIdea(properties: IIdeaEditableProperties, userId: UserId): Idea {
        return Idea(
            id = generateId(),
            groupId = properties.groupId,
            title = properties.title,
            description = properties.description,
            link = properties.link,
            implemented = false,
            assignee = NOT_ASSIGNED,
            author = userId,
            voters = emptySet(),
            ctime = LocalDateTime.now()
        )
    }
}

/**
 * Possible Idea's sorting
 */
enum class IdeaSorting {
    CTIME_ASC,
    CTIME_DESC,
    VOTES_DESC
}

fun requireNoneNullValue(opt: Optional<String>, field: String) {
    if (opt.isPresent) {
        require(!opt.get().isNullOrBlank()) { "$field is empty string, but should be Optional.empty()" }
    }
}

data class IdeaFiltering(
    val offeredBy: Optional<String>,
    val implemented: Optional<Boolean>,
    val assignee: Optional<UserId>,
    val text: Optional<String>
) {
    init {
        requireNoneNullValue(offeredBy, "offeredBy")
        requireNoneNullValue(assignee, "assigned")
        requireNoneNullValue(text, "text")
    }

}

/**
 * The idea with Couchbase CAS for safely updating.
 * It's not domain level.
 */
@Deprecated("Will be removed after migration into Postgresql")
data class IdeaWithVersion(val idea: Idea, val version: Long)


interface IdeaRepository {
    /**
     * Load idea by id.
     */
    fun load(id : String) : Either<Exception, Idea>

    /**
     * Add new idea.
     */
    fun add(idea: Idea) : Either<Exception, Idea>

    /**
     * Update an idea's info.
     *
     * @return
     *  [Right] with update Idea and new version, if can optimistic locking allows to updates
     *  [Left] with an Idea from storage with current storage version. Left means, that operation is failed!
     *
     */
    fun updateInfo(id: String, info: IIdeaEditableProperties): Either<Exception, Idea>

    /**
     * Update idea.
     *
     * Assumption that Idea is already loaded. So, not special checking for existing.
     *
     * @return [Left] if CAS is expired. Left contains new Idea with version.
     *         [Right] if successful update an Idea. Right is hold nothing.
     * @throws [com.couchbase.client.core.error.DocumentNotFoundException] if can't load document from collection
     */
    fun replace(ideaWithVersion: IdeaWithVersion): Either<IdeaWithVersion, Unit>

    fun loadWithVersion(first: Int, last: Int, sorting: IdeaSorting, filtering: IdeaFiltering): List<Idea>

    fun loadWithVersion(id: String): Optional<IdeaWithVersion>
}