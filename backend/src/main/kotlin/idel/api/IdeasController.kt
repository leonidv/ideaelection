package idel.api

import arrow.core.Either
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
class IdeasController(val ideaRepository: IdeaRepository, apiSecurityFactory: ApiSecurityFactory) {

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

    @PostMapping
    fun create(
        @AuthenticationPrincipal user: User,
        @RequestBody properties: InitialProperties
    ): EntityOrError<Idea> {
        return secure.group.asMember(properties.groupId, user) {
            either.eager {
                val idea = factory.createIdea(properties, properties.groupId, user.id).bind()
                ideaRepository.add(idea)
                idea
            }
        }
    }


    @GetMapping("/{ideaId}")
    fun load(@AuthenticationPrincipal user: User, @PathVariable ideaId: String): EntityOrError<Idea> {
        return secure.idea.asMember(ideaId, user) {idea ->
            Either.Right(idea)
        }
    }

    @PatchMapping("/{ideaId}")
    fun updateInfo(
        @AuthenticationPrincipal user: User,
        @PathVariable ideaId: String,
        @RequestBody properties: IdeaEditableProperties
    ): EntityOrError<Idea> {
        return secure.idea.withLevels(ideaId, user) {_, levels ->
            ideaRepository.possibleMutate(ideaId) {idea ->
                idea.updateInformation(properties, levels)
            }
        }
    }

    @PostMapping("/{ideaId}/voters")
    @ResponseStatus(HttpStatus.CREATED)
    fun vote(
        @AuthenticationPrincipal user: User,
        @PathVariable ideaId: String
    ): EntityOrError<Idea> {
        return secure.idea.asMember(ideaId, user) {
            ideaRepository.mutate(ideaId) {idea ->
                idea.addVote(user.id)
            }
        }
    }

    @DeleteMapping("/{ideaId}/voters")
    fun devote(
        @AuthenticationPrincipal user: User,
        @PathVariable ideaId: String
    ): EntityOrError<Idea> {
        return secure.idea.asMember(ideaId, user) {
            ideaRepository.mutate(ideaId) {idea ->
                idea.removeVote(user.id)
            }
        }
    }

    data class Assignee(val userId: String)

    @PatchMapping("/{ideaId}/assignee")
    fun assign(
        @AuthenticationPrincipal user: User,
        @PathVariable ideaId: String,
        @RequestBody assignee: Assignee
    ): EntityOrError<Idea> {
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
            }.flatten()
        }
    }

    data class Implemented(val implemented: Boolean)

    @PatchMapping("/{ideaId}/implemented")
    fun implemented(
        @AuthenticationPrincipal user: User,
        @PathVariable ideaId: String,
        @RequestBody body: Implemented
    ): EntityOrError<Idea> {
        return secure.idea.withLevels(ideaId, user) {_, levels ->
            ideaRepository.possibleMutate(ideaId) {idea ->
                if (body.implemented) {
                    idea.implement(levels)
                } else {
                    idea.notImplement(levels)
                }
            }
        }
    }

    @GetMapping
    @ResponseBody
    fun load(
        @AuthenticationPrincipal user: User,
        @RequestParam(required = true) groupId: String,
        @RequestParam(required = false, defaultValue = "") ordering: IdeaOrdering,
        @RequestParam("offered-by") offeredBy: Optional<String>,
        @RequestParam("assignee") assignee: Optional<String>,
        @RequestParam("implemented") implemented: Optional<Boolean>,
        @RequestParam("text") text: Optional<String>,
        pagination: Repository.Pagination
    )
            : EntityOrError<List<Idea>> {

        return secure.group.asMember(groupId, user) {
            val filtering = IdeaFiltering(
                offeredBy = offeredBy.asOption(),
                implemented = implemented.asOption(),
                assignee = assignee.asOption(),
                text = text.asOption()
            )
            ideaRepository.load(groupId, ordering, filtering, pagination)
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