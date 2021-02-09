package idel.api

import arrow.core.Either
import arrow.core.extensions.either.monad.flatten
import arrow.core.extensions.fx
import idel.domain.*
import idel.infrastructure.security.IdelOAuth2User
import mu.KotlinLogging
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.lang.IllegalArgumentException
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
    fun create(@AuthenticationPrincipal user: IdelOAuth2User, @RequestBody properties: InitialProperties): EntityOrError<Idea> {
        return secure.group.asMember(properties.groupId, user) {
            Either.fx {
                val (idea) = factory.createIdea(properties, properties.groupId, user.id)
                ideaRepository.add(idea)
                idea
            }
        }
    }


    @GetMapping("/{ideaId}")
    fun load(@AuthenticationPrincipal user: IdelOAuth2User, @PathVariable ideaId: String): EntityOrError<Idea> {
        return secure.idea.asMember(ideaId, user) {_, idea ->
            Either.right(idea)
        }
    }

    @PatchMapping("/{ideaId}")
    fun updateInfo(
            @AuthenticationPrincipal user: IdelOAuth2User,
            @PathVariable ideaId: String,
            @RequestBody properties: IdeaEditableProperties): EntityOrError<Idea> {
        return secure.idea.withLevels(ideaId, user) {_, _, levels ->
            ideaRepository.possibleMutate(ideaId) {idea ->
                idea.updateInformation(properties, levels)
            }
        }
    }


    @PostMapping("/{ideaId}/voters")
    @ResponseStatus(HttpStatus.CREATED)
    fun vote(@AuthenticationPrincipal user: IdelOAuth2User,
             @PathVariable ideaId: String): EntityOrError<Idea> {
        return secure.idea.asMember(ideaId, user) {_, _ ->
            ideaRepository.mutate(ideaId) {idea ->
                idea.addVote(user.id)
            }
        }
    }

    @DeleteMapping("/{ideaId}/voters")
    fun devote(@AuthenticationPrincipal user: IdelOAuth2User,
               @PathVariable ideaId: String): EntityOrError<Idea> {
        return secure.idea.asMember(ideaId, user) {_, _ ->
            ideaRepository.mutate(ideaId) {idea ->
                idea.removeVote(user.id)
            }
        }
    }

    data class Assignee(val userId: String)

    @PatchMapping("/{ideaId}/assignee")
    fun assign(@AuthenticationPrincipal user: IdelOAuth2User,
               @PathVariable ideaId: String,
               @RequestBody assignee: Assignee): EntityOrError<Idea> {
        return secure.idea.withLevels(ideaId, user) {group, _, levels ->
            Either.fx<Exception, Either<Exception, Idea>> {
                val removeAssignee = (assignee.userId == NOT_ASSIGNED)
                if (removeAssignee) {
                    ideaRepository.possibleMutate(ideaId) {idea ->
                        idea.removeAssign(levels)
                    }
                } else {
                    val (assigneeIsMember) = secure.group.isMember(group, assignee.userId)
                    if (assigneeIsMember) {
                        ideaRepository.possibleMutate(ideaId) {idea ->
                            idea.assign(assignee.userId, levels)
                        }
                    } else {
                        Either.left(OperationNotPermitted())
                    }
                }
            }.flatten()
        }
    }

    data class Implemented(val implemented: Boolean)

    @PatchMapping("/{ideaId}/implemented")
    fun implemented(
            @AuthenticationPrincipal user: IdelOAuth2User,
            @PathVariable ideaId: String,
            @RequestBody body: Implemented
    ): EntityOrError<Idea> {
        return secure.idea.withLevels(ideaId, user) {_, _, levels ->
            ideaRepository.possibleMutate(ideaId) {idea ->
                if (body.implemented) {
                    idea.implement(levels)
                } else {
                    idea.notImplement(levels)
                }
            }
        }
    }

    @GetMapping(
            path = [""],
            produces = ["application/json"]
    )
    @ResponseBody
    fun load(
            @RequestParam(required = false, defaultValue = "0") first: Int,
            @RequestParam(required = false, defaultValue = "10") last: Int,
            @RequestParam(required = false, defaultValue = "") sorting: IdeaSorting,
            @RequestParam("offeredBy") offeredBy: Optional<String>,
            @RequestParam("assignee") assignee: Optional<String>,
            @RequestParam("implemented") implemented: Optional<Boolean>,
            @RequestParam("text") text: Optional<String>
    )
            : EntityOrError<List<Idea>> {
        val size = last - first

        if (size <= 0) {
            return DataOrError.incorrectArgument("last: $last, first: $first", "first should be less then first")
        }

        if (size > 100) {
            val error = ErrorDescription.tooManyItems(size, 100)
            return DataOrError.errorResponse(error)
        }


        val filtering = IdeaFiltering(
                offeredBy = offeredBy,
                implemented = implemented,
                assignee = assignee,
                text = text
        )

        val data = ideaRepository.loadWithVersion(first, last, sorting, filtering)
        return DataOrError.data(data)
    }
}

/**
 * Convert string to IdeaSorting. If string is empty, return [IdeaSorting.CTIME_DESC] as default value.
 */
class StringToIdeaSortingConverter : Converter<String, IdeaSorting> {
    val DEFAULT = IdeaSorting.CTIME_DESC

    override fun convert(source: String): IdeaSorting {
        return if (source.isNullOrBlank()) {
            DEFAULT
        } else {
            try {
                IdeaSorting.valueOf(source.toUpperCase())
            } catch (ex: IllegalArgumentException) {
                DEFAULT
            }
        }
    }
}