package idel.api

import arrow.core.Either
import arrow.core.None
import arrow.core.Some
import arrow.core.computations.either
import arrow.core.flatten
import idel.domain.*
import mu.KotlinLogging
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*


@RestController
@RequestMapping("/ideas")
class IdeasController(
    val ideaRepository: IdeaRepository,
    val userRepository: UserRepository,
    apiSecurityFactory: ApiSecurityFactory
) {

    private val log = KotlinLogging.logger {}

    private val factory = IdeaFactory()

    private val secure: ApiSecurity = apiSecurityFactory.create(log)


    class InitialProperties(
        val groupId: String,
        override val summary: String,
        override val description: String,
        override val descriptionPlainText: String,
        override val link: String,
    ) : IIdeaEditableProperties

    data class IdeasPayload(
        val ideas: List<Idea>,
        val users: Set<User>
    )

    data class IdeaPayload(
        val idea: Idea,
        val users: Set<User>?
    ) {
        companion object {
            fun onlyIdea(idea: Idea) = IdeaPayload(idea, null)
        }
    }


    @PostMapping
    fun create(
        @AuthenticationPrincipal user: User,
        @RequestBody properties: InitialProperties
    ): EntityOrError<IdeaPayload> {
        return secure.group.asMember(properties.groupId, user) {
            either.eager {
                val idea = factory.createIdea(properties, properties.groupId, user.id).bind()
                ideaRepository.add(idea)
                IdeaPayload.onlyIdea(idea)
            }
        }
    }


    @GetMapping("/{ideaId}")
    fun load(@AuthenticationPrincipal user: User, @PathVariable ideaId: String): EntityOrError<IdeaPayload> {
        return secure.idea.asMember(ideaId, user) {idea ->
            either.eager {
                val users = userRepository.enrichIdeas(listOf(idea), 10).bind()
                IdeaPayload(idea, users)
            }
        }
    }

    @PatchMapping("/{ideaId}")
    fun updateInfo(
        @AuthenticationPrincipal user: User,
        @PathVariable ideaId: String,
        @RequestBody properties: IdeaEditableProperties
    ): EntityOrError<IdeaPayload> {
        return secure.idea.withLevels(ideaId, user) {_, levels ->
            ideaRepository.possibleMutate(ideaId) {idea ->
                idea.updateInformation(properties, levels)
            }.map(IdeaPayload::onlyIdea)
        }
    }

    @PostMapping("/{ideaId}/voters")
    @ResponseStatus(HttpStatus.CREATED)
    fun vote(
        @AuthenticationPrincipal user: User,
        @PathVariable ideaId: String
    ): EntityOrError<IdeaPayload> {
        return secure.idea.asMember(ideaId, user) {
            ideaRepository.possibleMutate(ideaId) {idea ->
                idea.addVote(user.id)
            }.map(IdeaPayload::onlyIdea)
        }
    }

    @DeleteMapping("/{ideaId}/voters")
    fun devote(
        @AuthenticationPrincipal user: User,
        @PathVariable ideaId: String
    ): EntityOrError<IdeaPayload> {
        return secure.idea.asMember(ideaId, user) {
            ideaRepository.possibleMutate(ideaId) {idea ->
                idea.removeVote(user.id)
            }.map(IdeaPayload::onlyIdea)
        }
    }

    data class AssigneeBody(val userId: String)

    @PatchMapping("/{ideaId}/assignee")
    fun assign(
        @AuthenticationPrincipal user: User,
        @PathVariable ideaId: String,
        @RequestBody assignee: AssigneeBody
    ): EntityOrError<IdeaPayload> {
        return secure.idea.withLevels(ideaId, user) {idea, levels ->
            either.eager<Exception, Either<Exception, Idea>> {
                val removeAssignee = (assignee.userId == NOT_ASSIGNED)
                if (removeAssignee) {
                    ideaRepository.possibleMutate(ideaId) {idea ->
                        idea.removeAssign(levels)
                    }
                } else {
                    val assigneeIsMember = secure.group.isMember(idea.groupId, assignee.userId).bind()
                    if (assigneeIsMember) {
                        ideaRepository.possibleMutate(ideaId) {idea ->
                            idea.assign(assignee.userId, levels)
                        }
                    } else {
                        Either.Left(OperationNotPermitted())
                    }
                }
            }.flatten().map(IdeaPayload::onlyIdea)
        }
    }

    data class Implemented(val implemented: Boolean)

    @PatchMapping("/{ideaId}/implemented")
    fun implemented(
        @AuthenticationPrincipal user: User,
        @PathVariable ideaId: String,
        @RequestBody body: Implemented
    ): EntityOrError<IdeaPayload> {
        return secure.idea.withLevels(ideaId, user) {_, levels ->
            ideaRepository.possibleMutate(ideaId) {idea ->
                if (body.implemented) {
                    idea.implement(levels)
                } else {
                    idea.notImplement(levels)
                }
            }.map(IdeaPayload::onlyIdea)
        }
    }

    @DeleteMapping("/{ideaId}")
    fun delete(
        @AuthenticationPrincipal user: User,
        @PathVariable ideaId: String
    ): EntityOrError<IdeaPayload> {
        return secure.idea.withLevels(ideaId, user) {_, levels ->
            ideaRepository.possibleMutate(ideaId) {idea ->
                idea.delete(levels)
            }.map(IdeaPayload::onlyIdea)
        }
    }

    data class Archived(val archived: Boolean)

    @PatchMapping("/{ideaId}/archived")
    fun changeArchived(
        @AuthenticationPrincipal user: User,
        @PathVariable ideaId: String,
        @RequestBody value: Archived
    ): EntityOrError<IdeaPayload> {
        return secure.idea.withLevels(ideaId, user) {_, levels ->
            ideaRepository.possibleMutate(ideaId) {idea ->
                if (value.archived) {
                    idea.archive(levels)
                } else {
                    idea.unArchive(levels)
                }
            }.map(IdeaPayload::onlyIdea)
        }
    }

    data class ChangeGroup(val groupId: String)

    @PatchMapping("/{ideaId}/group")
    fun changeGroup(
        @AuthenticationPrincipal user: User,
        @PathVariable ideaId: String,
        @RequestBody value: ChangeGroup
    ): EntityOrError<IdeaPayload> {
        return secure.group.asMember(groupId = value.groupId, user) {
            either.eager<Exception, Idea> {
                val userIdeaLevels = secure.idea.calculateLevels(ideaId, user).bind()
                ideaRepository.possibleMutate(ideaId) {idea ->
                    idea.changeGroup(value.groupId, userIdeaLevels)
                }.bind()
            }.map(IdeaPayload::onlyIdea)
        }
    }

    @GetMapping
    @ResponseBody
    fun load(
        @AuthenticationPrincipal user: User,
        @RequestParam(required = true) groupId: String,
        @RequestParam(required = false, defaultValue = "") ordering: IdeaOrdering,
        @RequestParam("author") author: Optional<String>,
        @RequestParam("assignee") assignee: Optional<String>,
        @RequestParam("implemented") implemented: Optional<Boolean>,
        @RequestParam("text") text: Optional<String>,
        @RequestParam("archived", defaultValue = "false") archived: Boolean,
        @RequestParam("voted-by-me") votedByMy: Optional<Boolean>,
        pagination: Repository.Pagination
    )
            : EntityOrError<IdeasPayload> {

        return secure.group.asMember(groupId, user) {
            val votedBy = votedByMy.asOption().flatMap {
                if (it) {
                    Some(user.id)
                } else {
                    None
                }
            }

            val filtering = IdeaFiltering(
                author = author.asOption(),
                implemented = implemented.asOption(),
                assignee = assignee.asOption(),
                text = text.asOption(),
                archived = archived,
                deleted = false,
                votedBy = votedBy
            )
            either.eager {
                val ideas = ideaRepository.load(groupId, ordering, filtering, pagination).bind()
                val users = userRepository.enrichIdeas(ideas, maxVoters = 10).bind()
                IdeasPayload(ideas, users)
            }
        }
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