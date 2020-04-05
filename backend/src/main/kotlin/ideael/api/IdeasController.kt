package ideael.api

import arrow.core.Either
import ideael.domain.*
import ideael.infrastructure.configs.VoterUser
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.lang.IllegalArgumentException
import java.util.*

typealias IdeaResponse = ResponseEntity<ResponseOrError<Idea>>
//typealias IdeaWithVersionResponse = ResponseEntity<ResponseOrError<IdeaWithVersion>>
typealias IdeaListResponse = ResponseEntity<ResponseOrError<List<Idea>>>

@RestController
@RequestMapping("/ideas")
class IdeasController(val ideaRepository: IdeaRepository) {

    private val factory = IdeaFactory()

    class IdeaInfo(
        override var title: String,
        override var description: String,
        override var link: String
    ) : ideael.domain.IdeaInfo


    private fun currentUser(): Voter {
        val auth = SecurityContextHolder.getContext().authentication
        return (auth.principal as VoterUser).getVoter()
    }

    @PostMapping(produces = ["application/json"])
    @ResponseBody
    fun create(@RequestBody info: IdeaInfo): IdeaResponse {
        val voter = currentUser()
        val newIdea = factory.createIdea(info, voter.id())
        return ResponseOrError.data(ideaRepository.add(newIdea), HttpStatus.CREATED)
    }

    @GetMapping(
        path = ["/{id}"],
        produces = ["application/json"]
    )
    @ResponseBody
    fun load(@PathVariable id: String): IdeaResponse {
        val maybeIdea: Optional<IdeaWithVersion> = ideaRepository.load(id)
        return if (maybeIdea.isPresent) {
            ResponseOrError.data(maybeIdea.get().idea)
        } else {
            ResponseOrError.notFound(id)
        }
    }

    @PutMapping(
        path = ["/{id}"],
        produces = ["application/json"]
    )
    fun updateInfo(@PathVariable id: String, @RequestBody ideaInfo: IdeaInfo): IdeaResponse {
        val maybeIdeaWithCas = ideaRepository.load(id)
        if (maybeIdeaWithCas.isEmpty) {
            return ResponseOrError.notFound(id)
        }

        val currentVoter = currentUser()
        val originIdea = maybeIdeaWithCas.get().idea

        if (originIdea.offeredBy != currentVoter.id()) {
            return ResponseOrError.forbidden("Only user which offered Idea can change it")
        }

        var updateResult = ideaRepository.updateInfo(id, ideaInfo)

        return when (updateResult) {
            is Either.Left -> ResponseOrError.internal("Can't update idea. CAS mismatching. You can retry")
            is Either.Right -> ResponseOrError.data(updateResult.b)
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
            var maybeIdea = ideaRepository.load(ideaId)
            if (maybeIdea.isEmpty) {
                return ResponseOrError.notFound(ideaId)
            }

            var (originIdea, version) = maybeIdea.get()

            newIdea = mutation(originIdea)
            val replaceResult = ideaRepository.replace(IdeaWithVersion(newIdea, version))
            canReplace = replaceResult.isRight()
            attempt++
        } while (!canReplace && attempt <= 3)

        return if (canReplace) {
            ResponseOrError.data(newIdea)
        } else {
            ResponseOrError.internal("Can't update idea. CAS mismatching. You can retry")
        }
    }

    @PostMapping(
        path = ["/{id}/voters"]
    )
    @ResponseStatus(HttpStatus.CREATED)
    fun vote(@PathVariable id: String): IdeaResponse {
        val voterId = currentUser().id()
        return updateIdea(id) { it.addVote(voterId) }
    }

    @DeleteMapping(
        path = ["/{id}/voters"]
    )
    fun devote(@PathVariable id: String): IdeaResponse {
        val voterId = currentUser().id()
        return updateIdea(id) { it.removeVote(voterId) }
    }

    @PostMapping(
        path = ["/{id}/assignee/{userId}"]
    )
    fun assign(@PathVariable id: String, @PathVariable userId: UserId): IdeaResponse {
        val callerId = currentUser().id()
        if (userId != callerId) {
            return ResponseOrError.forbidden("User can take idea for implementation only by self")
        }

        return updateIdea(id) { it.assign(userId) }
    }


    @DeleteMapping(
        path = ["/{id}/assignee"]
    )
    fun removeAssign(@PathVariable id: String): IdeaResponse {
        return changeOnlyIfAssignedToUser(id, currentUser().id()) {
            this.updateIdea(id) { it.removeAssign() }
        }
    }

    @PostMapping(
        path = ["/{id}/implemented"]
    )
    fun markAsImplemented(@PathVariable id: String): IdeaResponse {
        return changeOnlyIfAssignedToUser(id, currentUser().id()) {
            this.updateIdea(id) { it.copy(implemented = true) }
        }
    }

    @DeleteMapping(
        path = ["/{id}/implemented"]
    )
    fun markAsUnimplemented(@PathVariable id: String): IdeaResponse {
        return changeOnlyIfAssignedToUser(id, currentUser().id()) {
            this.updateIdea(id) { it.copy(implemented = false) }
        }
    }

    /**
     * Make mutation of Idea only if it assigned to callerId
     *
     * @return [Optional.empty] if user can change Idea or Some with Error response.

     */
    private fun changeOnlyIfAssignedToUser(id: String, callerId: UserId, mutation: () -> IdeaResponse): IdeaResponse {
        val maybeIdea = this.ideaRepository.load(id)
        if (maybeIdea.isEmpty) {
            return ResponseOrError.notFound(id)
        }

        val currentAssignee = maybeIdea.get().idea.assignee
        return if (currentAssignee != callerId) {
            ResponseOrError.forbidden("Only assignee can make remove his from idea implementation")
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
            val error = ErrorDescription.incorrectArgument(
                "last: $last, first: $first",
                "Request non-positive count of document"
            )
            return ResponseOrError.badRequest(error)
        }

        if (size > 100) {
            val error = ErrorDescription.tooManyItems(size, 100);
            return ResponseOrError.badRequest(error)
        }


        val filtering = IdeaFiltering(
            offeredBy = offeredBy,
            implemented = implemented,
            assignee = assignee,
            text = text
        )

        val data = ideaRepository.load(first, last, sorting, filtering)
        return ResponseOrError.data(data)
    }
}

/**
 * Convert string to IdeaSorting. If string is empty, return [IdeaSorting.CTIME_DESC] as default value.
 */
class StringToIdeaSortingConverter : Converter<String, IdeaSorting> {
    override fun convert(source: String): IdeaSorting {
        return if (source.isNullOrBlank()) {
            IdeaSorting.CTIME_DESC
        } else {
            try {
                IdeaSorting.valueOf(source.toUpperCase())
            } catch (ex: IllegalArgumentException) {
                IdeaSorting.CTIME_DESC
            }
        }
    }

}