package idel.domain

import arrow.core.Either
import io.konform.validation.Validation
import io.konform.validation.jsonschema.maxLength
import io.konform.validation.jsonschema.minLength
import java.time.LocalDateTime
import java.util.UUID

interface ICommentEditableProperties {
    val content: String
}

class CommentEditableProperties(
    override var content: String
) : ICommentEditableProperties

class Comment(
    /**
     * Generated identifier
     */
    val id: UUID,

    /**
     * Creation time
     */
    val ctime: LocalDateTime,

    /**
     * Identifier of the idea to comment on.
     */
    val ideaId: IdeaId,

    /**
     * Identifier of the user, which created a comment.
     */
    val author: UserId,

    /**
     * Really, this is a comment.
     */
    override val content: String,

    /**
     *  Not null if comment was edited.
     */
    val lastEditedTime: LocalDateTime?,

    /**
     * User, which is last time edit the comment.
     */
    val lastEditedBy: UserId?,

    /**
     * Identifier of the comment if this comment reply to another.
     */
    val replyTo: UUID?
) : ICommentEditableProperties {
    init {
        require(content.isNotBlank()) {"content can't be blank"}
        require(
            (lastEditedBy == null && lastEditedTime == null) || (lastEditedBy != null && lastEditedTime != null)
        )
    }


    private fun clone(
        content: String = this.content,
        lastEditedBy: UserId? = this.lastEditedBy,
        lastEditedTime: LocalDateTime? = this.lastEditedTime
    ): Either<ValidationError, Comment> {
        val editableProperties = CommentEditableProperties(content)
        return CommentValidation.ifValid(editableProperties) {
            Comment(
                id = this.id,
                ctime = this.ctime,
                ideaId = this.ideaId,
                author = this.author,
                content = content,
                lastEditedBy = lastEditedBy,
                lastEditedTime = lastEditedTime,
                replyTo = this.replyTo
            )
        }
    }

    /**
     * Edit content.
     */
    fun edit(nextContent: String, editor: UserId): Either<ValidationError, Comment> {
        return clone(nextContent, editor, LocalDateTime.now())
    }
}

class CommentValidation {
    companion object : Validator<ICommentEditableProperties> {
        override val validation = Validation<ICommentEditableProperties> {
            ICommentEditableProperties::content required {
                minLength(1)
                maxLength(1000) // should be synced with DDL
            }
        }
    }
}

class CommentFactory {
    fun createComment(
        properties: ICommentEditableProperties,
        ideaId: IdeaId,
        author: UserId,
        replyTo: UUID?
    ): Either<ValidationError, Comment> {
        return CommentValidation.ifValid(properties) {
            Comment(
                id = UUID.randomUUID(),
                ctime = LocalDateTime.now(),
                ideaId = ideaId,
                author = author,
                content = properties.content,
                lastEditedTime = null,
                lastEditedBy = null,
                replyTo = replyTo
            )
        }
    }
}

enum class CommentsOrder {
    CTIME_ASC, CTIME_DESC
}

interface CommentRepository {
    fun add(comment: Comment): Either<DomainError, Comment>

    fun load(commentId : UUID) : Either<DomainError, Comment>

    fun countForIdeas(ideaIds: List<IdeaId>): Either<DomainError, Map<IdeaId, Int>>

    fun list(ideaId: IdeaId, orderBy: CommentsOrder, pagination: Repository.Pagination) : Either<DomainError, List<Comment>>

    fun delete(id : UUID) : Either<DomainError, Unit>

    fun update(comment: Comment) : Either<DomainError, Comment>
}