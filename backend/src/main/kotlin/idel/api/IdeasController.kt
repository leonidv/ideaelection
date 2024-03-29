package idel.api

import arrow.core.Either
import arrow.core.continuations.either
import arrow.core.flatMap
import arrow.core.flatten
import idel.domain.*
import idel.infrastructure.repositories.PersistsUser
import mu.KotlinLogging
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*
import kotlin.math.min


@RestController
@RequestMapping("/ideas")
class IdeasController(
    private val ideaRepository: IdeaRepository,
    private val userRepository: UserRepository,
    private val commentRepository: CommentRepository,
    private val secure: ApiSecurity
) : DataOrErrorHelper {

    override val log = KotlinLogging.logger {}

    private val ideaFactory = IdeaFactory()

    private val commentFactory = CommentFactory()

    private val MAX_VOTERS = 10

    class InitialProperties(
        val groupId: GroupId,
        override val summary: String,
        override val description: String,
        override val descriptionPlainText: String,
        override val link: String,
    ) : IIdeaEditableProperties

    data class IdeasPayload(
        val ideas: List<Idea>,
        val users: Iterable<User>,
        val commentsCounts: Map<IdeaId, Int>
    )

    data class IdeaPayload(
        val idea: Idea,
        val users: Iterable<User>?,
        val comments: List<Comment>
    ) {
        companion object {
            fun onlyIdea(idea: Idea) = IdeaPayload(idea, null, emptyList())
        }
    }

    fun Idea.usersIds(maxVoters: Int): Set<UserId> {
        val votersCount = min(this.voters.size, maxVoters)
        // filter always return List, so use list first, then convert to set :(
        val ids = listOf(this.assignee, this.author) + this.voters.subList(0, votersCount)
        return ids.filterNotNull().toSet()
    }

    fun enrichIdeas(ideas: List<Idea>, maxVoters: Int): Either<DomainError, IdeasPayload> {
        val ideasIds = ideas.map {it.id}
        val usersIds = ideas.flatMap {it.usersIds(maxVoters)}.toSet()

        return either.eager {
            val users = userRepository.listById(usersIds).bind()
            val commentsCounts = commentRepository.countForIdeas(ideasIds).bind()
            IdeasPayload(ideas, users, commentsCounts)
        }
    }

    fun enrichIdea(idea: Idea, maxVoters: Int): Either<DomainError, IdeaPayload> {
        val commentsPagination = Repository.Pagination(skip = 0, count = 10)
        return either.eager {
            val voters = userRepository.listById(idea.usersIds(maxVoters)).bind().toSet()
            val comments = commentRepository.list(
                ideaId = idea.id,
                orderBy = CommentsOrder.CTIME_ASC,
                pagination = commentsPagination
            ).bind()
            val commentators = enrichComments(comments).bind().users.toSet()
            val users : Set<User> = voters + commentators
            IdeaPayload(idea, users, comments)
        }
    }

    data class CommentsPayload(
        val comments: List<Comment>,
        val users: List<User>
    )

    data class CommentPayload(
        val comment: Comment
    )

    fun enrichComments(comments: List<Comment>): Either<DomainError, CommentsPayload> {
        val usersId = comments.flatMap {listOf(it.author, it.lastEditedBy)}.filterNotNull()
        return userRepository
            .listById(usersId.toSet())
            .map {CommentsPayload(comments, it.toList())}
    }

    @PostMapping
    fun create(
        @AuthenticationPrincipal user: User,
        @RequestBody properties: InitialProperties
    ): ResponseDataOrError<IdeaPayload> {
        return secure.group.asMember(properties.groupId, user) {
            either.eager {
                val idea = ideaFactory.createIdea(properties, properties.groupId, user.id).bind()
                ideaRepository.add(idea).bind()
                IdeaPayload(idea, setOf(PersistsUser.of(user)), emptyList())
            }
        }.asHttpResponse()
    }


    @GetMapping("/{ideaId}")
    fun load(@AuthenticationPrincipal user: User, @PathVariable ideaId: IdeaId): ResponseDataOrError<IdeaPayload> {
        return secure.idea.asGroupMember(ideaId, user) {idea ->
            enrichIdea(idea, MAX_VOTERS)
        }.asHttpResponse()
    }

    @PatchMapping("/{ideaId}")
    fun updateInfo(
        @AuthenticationPrincipal user: User,
        @PathVariable ideaId: IdeaId,
        @RequestBody properties: IdeaEditableProperties
    ): ResponseDataOrError<IdeaPayload> {
        return secure.idea.withLevels(ideaId, user) {idea, levels ->
            val nextIdea = idea.updateInformation(properties, levels)
            ideaRepository.update(nextIdea)
                .map(IdeaPayload::onlyIdea)
        }.asHttpResponse()
    }


    @PostMapping("/{ideaId}/voters")
    @ResponseStatus(HttpStatus.CREATED)
    fun vote(
        @AuthenticationPrincipal user: User,
        @PathVariable ideaId: IdeaId
    ): ResponseDataOrError<IdeaPayload> {
        return secure.idea.asGroupMember(ideaId, user) {idea ->
            ideaRepository.update(idea.addVote(user.id))
                .map(IdeaPayload::onlyIdea)
        }.asHttpResponse()

    }

    @DeleteMapping("/{ideaId}/voters")
    fun devote(
        @AuthenticationPrincipal user: User,
        @PathVariable ideaId: IdeaId
    ): ResponseDataOrError<IdeaPayload> {
        return secure.idea.asGroupMember(ideaId, user) {idea ->
            ideaRepository.update(idea.removeVote(user.id))
                .map(IdeaPayload::onlyIdea)
        }.asHttpResponse()
    }


    data class AssigneeBody(val userId: String?)

    @PatchMapping("/{ideaId}/assignee")
    fun assign(
        @AuthenticationPrincipal user: User,
        @PathVariable ideaId: IdeaId,
        @RequestBody assignee: AssigneeBody
    ): ResponseDataOrError<IdeaPayload> {
        return secure.idea.withLevels(ideaId, user) {idea, levels ->
            either.eager<DomainError, Either<DomainError, Idea>> {
                val removeAssignee = (assignee.userId.isNullOrBlank())
                val nextIdea = if (removeAssignee) {
                    idea.removeAssign(levels)
                } else {
                    require(assignee.userId != null) // really double check, should be checked in val removeAssignee =
                    val nextAssigneeId = UUID.fromString(assignee.userId);
                    val assigneeIsMember =
                        secure.group.isMember(idea.groupId, UUID.fromString(assignee.userId)).bind()
                    if (assigneeIsMember) {
                        idea.assign(nextAssigneeId, levels)
                    } else {
                        Either.Left(OperationNotPermitted())
                    }
                }
                ideaRepository.update(nextIdea)
            }.flatten().map(IdeaPayload::onlyIdea)

        }.asHttpResponse()
    }

    data class Implemented(val implemented: Boolean)

    @PatchMapping("/{ideaId}/implemented")
    fun implemented(
        @AuthenticationPrincipal user: User,
        @PathVariable ideaId: IdeaId,
        @RequestBody body: Implemented
    ): ResponseDataOrError<IdeaPayload> {
        return secure.idea.withLevels(ideaId, user) {idea, levels ->
            val nextIdea = if (body.implemented) {
                idea.implement(levels)
            } else {
                idea.notImplement(levels)
            }
            ideaRepository.update(nextIdea)
                .map(IdeaPayload::onlyIdea)
        }.asHttpResponse()
    }

    @DeleteMapping("/{ideaId}")
    fun delete(
        @AuthenticationPrincipal user: User,
        @PathVariable ideaId: IdeaId
    ): ResponseDataOrError<IdeaPayload> {
        return secure.idea.withLevels(ideaId, user) {idea, levels ->
            val nextIdea = idea.delete(levels)
            ideaRepository.update(nextIdea)
                .map(IdeaPayload::onlyIdea)
        }.asHttpResponse()
    }

    data class Archived(val archived: Boolean)

    @PatchMapping("/{ideaId}/archived")
    fun changeArchived(
        @AuthenticationPrincipal user: User,
        @PathVariable ideaId: IdeaId,
        @RequestBody value: Archived
    ): ResponseDataOrError<IdeaPayload> {
        return secure.idea.withLevels(ideaId, user) {idea, levels ->
            val nextIdea = if (value.archived) {
                idea.archive(levels)
            } else {
                idea.unArchive(levels)
            }
            ideaRepository.update(nextIdea).map(IdeaPayload::onlyIdea)
        }.asHttpResponse()
    }

    data class ChangeGroup(val groupId: GroupId)

    @PatchMapping("/{ideaId}/group")
    fun changeGroup(
        @AuthenticationPrincipal user: User,
        @PathVariable ideaId: IdeaId,
        @RequestBody value: ChangeGroup
    ): ResponseDataOrError<IdeaPayload> {
        return secure.group.asMember(groupId = value.groupId, user) {
            either.eager<DomainError, Idea> {
                val (idea, levels) = secure.idea.calculateLevels(ideaId, user).bind()
                val nextIdea = idea.changeGroup(value.groupId, levels).bind()
                ideaRepository.update(nextIdea).bind()
            }.map(IdeaPayload::onlyIdea)
        }.asHttpResponse()
    }

    @GetMapping
    @ResponseBody
    fun load(
        @AuthenticationPrincipal user: User,
        @RequestParam(required = true) groupId: GroupId,
        @RequestParam(required = false, defaultValue = "") ordering: IdeaOrdering,
        @RequestParam("author") author: UserId?,
        @RequestParam("assignee") assignee: UserId?,
        @RequestParam("implemented") implemented: Boolean?,
        @RequestParam("text") text: String?,
        @RequestParam("archived", defaultValue = "false") archived: Boolean,
        @RequestParam("voted-by-me") votedByMy: Boolean?,
        pagination: Repository.Pagination
    ): ResponseDataOrError<IdeasPayload> {
        return secure.group.asMember(groupId, user) {
            val filtering = IdeaFiltering(
                author = author,
                implemented = implemented,
                assignee = assignee,
                text = text,
                listArchived = archived,
                listDeleted = false,
                votedBy = votedByMy?.let {user.id}
            )
            either.eager {
                val ideas = ideaRepository.list(groupId, ordering, filtering, pagination).bind()
                enrichIdeas(ideas, MAX_VOTERS).bind()
            }
        }.asHttpResponse()
    }

    class AddCommentProperties(
        override val content: String,
        val replyTo: UUID?
    ) : ICommentEditableProperties

    @PostMapping("/{ideaId}/comments")
    fun addComment(
        @AuthenticationPrincipal user: User,
        @PathVariable("ideaId") ideaId: IdeaId,
        @RequestBody properties: AddCommentProperties
    ):
            ResponseDataOrError<Comment> {
        return secure.idea.asGroupMember(ideaId, user) {
            either.eager {
                val comment = commentFactory.createComment(
                    properties = properties,
                    ideaId = ideaId,
                    author = user.id,
                    replyTo = properties.replyTo
                ).bind()
                commentRepository.add(comment).bind()
                comment
            }
        }.asHttpResponse()
    }

    @GetMapping("/{ideaId}/comments")
    fun listComments(
        @AuthenticationPrincipal user: User,
        @PathVariable("ideaId") ideaId: IdeaId,
        pagination: Repository.Pagination
    ): ResponseDataOrError<CommentsPayload> {
        return secure.idea.asGroupMember(ideaId, user) {
            commentRepository
                .list(ideaId, CommentsOrder.CTIME_ASC, pagination)
                .flatMap {enrichComments(it)}
        }.asHttpResponse()
    }

    @PutMapping("/{ideaId}/comments/{commentId}")
    fun editComment(
        @AuthenticationPrincipal user: User,
        @PathVariable("ideaId") ideaId: IdeaId,
        @PathVariable("commentId") commentId : UUID,
        @RequestBody properties : CommentEditableProperties
    ) : ResponseDataOrError<CommentPayload> {
        return secure.idea.asGroupMember(ideaId, user) {
            either.eager {
                val comment = commentRepository.load(commentId).bind() // should take from security action
                val nextComment = comment.edit(properties.content, user.id).bind()
                commentRepository.update(nextComment).bind()
                CommentPayload(nextComment)
            }
        }.asHttpResponse()
    }

    @DeleteMapping("/{ideaId}/comments/{commentId}")
    fun deleteComment(
        @AuthenticationPrincipal user: User,
        @PathVariable("ideaId") ideaId: IdeaId,
        @PathVariable("commentId") commentId: UUID
    ) : ResponseDataOrError<String> {
        // TODO("Security admin or author")
        return fTransaction {
            commentRepository.delete(commentId).map {"Deleted"}
        }.asHttpResponse()
    }
}


/**
 * Convert string to IdeaSorting. If string is empty, return [IdeaOrdering.CTIME_DESC] as default value.
 */
class StringToIdeaSortingConverter : Converter<String, IdeaOrdering> {
    companion object {
        val DEFAULT = IdeaOrdering.CTIME_DESC
    }

    override fun convert(source: String): IdeaOrdering {
        @Suppress("UselessCallOnNotNull")
        return if (source.isNullOrBlank()) {
            DEFAULT
        } else {
            try {
                IdeaOrdering.valueOf(source.uppercase(Locale.getDefault()))
            } catch (ex: IllegalArgumentException) {
                DEFAULT
            }
        }
    }
}