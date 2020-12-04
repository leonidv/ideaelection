package idel.api

import arrow.core.Either
import idel.domain.*
import idel.infrastructure.security.IdelOAuth2User
import mu.KotlinLogging
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.lang.IllegalArgumentException
import java.util.*

typealias IdeaResponse = ResponseEntity<DataOrError<Idea>>
typealias IdeaListResponse = ResponseEntity<DataOrError<List<Idea>>>

@RestController
@RequestMapping("/ideas")
class IdeasController(val ideaRepository: IdeaRepository, apiSecurityFactory: ApiSecurityFactory) {

    private val log = KotlinLogging.logger {}

    private val factory = IdeaFactory()

    private val secure = apiSecurityFactory.create(log)


    private fun currentUser(): User {
        val auth = SecurityContextHolder.getContext().authentication
        return auth.principal as IdelOAuth2User
    }

    @PostMapping
    fun create(@AuthenticationPrincipal user: IdelOAuth2User, @RequestBody properties: IdeaEditableProperties): EntityOrError<Idea> {
          return secure.group.asMember(properties.groupId, user) {
              val idea = factory.createIdea(properties, user.id)
              ideaRepository.add(idea)
          }
    }


    @GetMapping("/{id}")
    fun load(@AuthenticationPrincipal user: IdelOAuth2User, @PathVariable id: String): EntityOrError<Idea> {
//        return secure.asMember()
//
//        val maybeIdea: Optional<IdeaWithVersion> = ideaRepository.loadWithVersion(id)
//        return if (maybeIdea.isPresent) {
//            DataOrError.data(maybeIdea.get().idea)
//        } else {
//            DataOrError.notFound(id)
//        }
        return DataOrError.notImplemented()
    }

    @PutMapping(
        path = ["/{id}"],
        produces = ["application/json"]
    )
    fun updateInfo(@PathVariable id: String, @RequestBody ideaInfo: IdeaEditableProperties): IdeaResponse {
        val maybeIdeaWithCas = ideaRepository.loadWithVersion(id)
        if (maybeIdeaWithCas.isEmpty) {
            return DataOrError.notFound(id)
        }

        val currentVoter = currentUser()
        val originIdea = maybeIdeaWithCas.get().idea

        if (originIdea.author != currentVoter.id) {
            return DataOrError.forbidden("Only user which offered Idea can change it")
        }

        var updateResult = ideaRepository.updateInfo(id, ideaInfo)

        return when (updateResult) {
            is Either.Left -> DataOrError.internal("Can't update idea. CAS mismatching. You can retry")
            is Either.Right -> DataOrError.data(updateResult.b)
        }
    }

    /**
     * Wrapper for updating Idea with optimistic locking.
     */
    private fun updateIdea(
        ideaId: String,
        mutation: (idea: Idea) -> Idea
    ): IdeaResponse {
        var canReplace: Boolean
        var attempt = 1
        lateinit var newIdea: Idea
        do {
            var maybeIdea = ideaRepository.loadWithVersion(ideaId)
            if (maybeIdea.isEmpty) {
                return DataOrError.notFound(ideaId)
            }

            var (originIdea, version) = maybeIdea.get()

            newIdea = mutation(originIdea)
            val replaceResult = ideaRepository.replace(IdeaWithVersion(newIdea, version))
            canReplace = replaceResult.isRight()
            attempt++
        } while (!canReplace && attempt <= 3)

        return if (canReplace) {
            DataOrError.data(newIdea)
        } else {
            DataOrError.internal("Can't update idea. CAS mismatching. You can retry")
        }
    }

    @PostMapping(
        path = ["/{id}/voters"]
    )
    @ResponseStatus(HttpStatus.CREATED)
    fun vote(@PathVariable id: String): IdeaResponse {
        val voterId = currentUser().id
        return updateIdea(id) { it.addVote(voterId) }
    }

    @DeleteMapping(
        path = ["/{id}/voters"]
    )
    fun devote(@PathVariable id: String): IdeaResponse {
        val voterId = currentUser().id
        return updateIdea(id) { it.removeVote(voterId) }
    }

    @PostMapping(
        path = ["/{id}/assignee/{userId}"]
    )
    fun assign(@PathVariable id: String, @PathVariable userId: UserId): IdeaResponse {
        val callerId = currentUser().id
        if (userId != callerId) {
            return DataOrError.forbidden("User can take idea for implementation only by self")
        }

        return updateIdea(id) { it.assign(userId) }
    }


    @DeleteMapping(
        path = ["/{id}/assignee"]
    )
    fun removeAssign(@PathVariable id: String): IdeaResponse {
        return changeOnlyIfAssignedToUser(id, currentUser().id) {
            this.updateIdea(id) { it.removeAssign() }
        }
    }

    @PostMapping(
        path = ["/{id}/implemented"]
    )
    fun markAsImplemented(@PathVariable id: String): IdeaResponse {
        return changeOnlyIfAssignedToUser(id, currentUser().id) {
            this.updateIdea(id) { it.update(implemented = true) }
        }
    }

    @DeleteMapping(
        path = ["/{id}/implemented"]
    )
    fun markAsUnimplemented(@PathVariable id: String): IdeaResponse {
        return changeOnlyIfAssignedToUser(id, currentUser().id) {
            this.updateIdea(id) { it.update(implemented = false) }
        }
    }

    /**
     * Make mutation of Idea only if it assigned to callerId
     *
     * @return [Optional.empty] if user can change Idea or Some with Error response.

     */
    private fun changeOnlyIfAssignedToUser(id: String, callerId: UserId, mutation: () -> IdeaResponse): IdeaResponse {
        val maybeIdea = this.ideaRepository.loadWithVersion(id)
        if (maybeIdea.isEmpty) {
            return DataOrError.notFound(id)
        }

        val currentAssignee = maybeIdea.get().idea.assignee
        return if (currentAssignee != callerId) {
            DataOrError.forbidden("Only assignee can make remove his from idea implementation")
        } else {
            mutation()
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
        @RequestParam("text") text : Optional<String>
    )
            : IdeaListResponse {
        val size = last - first;

        if (size <= 0) {
            return DataOrError.incorrectArgument("last: $last, first: $first","first should be less then first")
        }

        if (size > 100) {
            val error = ErrorDescription.tooManyItems(size, 100);
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