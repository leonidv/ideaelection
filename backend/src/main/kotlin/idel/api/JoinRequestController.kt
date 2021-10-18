package idel.api

import idel.domain.*
import mu.KotlinLogging
import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/joinrequests")
class JoinRequestController(
    val groupMembershipService: GroupMembershipService,
    val joinRequestRepository: JoinRequestRepository
) {
    private val log = KotlinLogging.logger {}

    data class JoinRequestParams(
        val joiningKey: String,
        val message: String
    );

    data class JoinRequestPayload(
        val joinRequest: JoinRequest,
        val group: Set<Group>?,
        val users: Set<User>?
    ) {
        companion object {
            fun onlyJoinRequest(joinRequest: JoinRequest) = JoinRequestPayload(joinRequest, null, null)
        }
    }

    data class JoinRequestsPayload(
        val joinRequests: List<JoinRequest>,
        val groups: Set<Group>,
        val users: Set<User>
    )

    @PostMapping
    fun create(
        @AuthenticationPrincipal user: User,
        @RequestBody joinRequestParams: JoinRequestParams
    ): EntityOrError<JoinRequestPayload> {
        val eJoinRequest = groupMembershipService.requestMembership(
            joiningKey = joinRequestParams.joiningKey,
            userId = user.id,
            message = joinRequestParams.message
        ).map(JoinRequestPayload::onlyJoinRequest)
        return DataOrError.fromEither(eJoinRequest, log)
    }


    @GetMapping(params = ["userId"])
    fun loadByUser(
        @AuthenticationPrincipal user: User,
        @RequestParam(required = false, defaultValue = "") order: GroupMembershipRequestOrdering,
        @RequestParam userId: String,
        pagination: Repository.Pagination
    ): EntityOrError<List<JoinRequest>> {
        if (user.id != userId) {
            return DataOrError.forbidden("you can't get join requests another users")
        }

        val result = joinRequestRepository.loadByUser(userId, order, pagination)

        return DataOrError.fromEither(result, log)
    }

    @GetMapping(params = ["groupId"])
    fun loadByGroup(
        @AuthenticationPrincipal user: User,
        @RequestParam(required = false, defaultValue = "") order: GroupMembershipRequestOrdering,
        @RequestParam groupId: String,
        pagination: Repository.Pagination
    ): EntityOrError<List<JoinRequest>> {
        val result = joinRequestRepository.loadByGroup(groupId, order, pagination)

        return DataOrError.fromEither(result, log)
    }

    data class Resolution(val status: AcceptStatus)

    @PatchMapping("{joinRequestId}/status")
    fun resolve(
        @AuthenticationPrincipal user: User,
        @PathVariable joinRequestId: String,
        @RequestBody resolution: Resolution
    ): EntityOrError<JoinRequestPayload> {
        val result = groupMembershipService.resolveRequest(joinRequestId, resolution.status)
            .map(JoinRequestPayload::onlyJoinRequest)
        return DataOrError.fromEither(result, log)
    }

    @DeleteMapping("{joinRequestId}")
    fun delete(
        @AuthenticationPrincipal user: User,
        @PathVariable joinRequestId: String
    ): EntityOrError<String> {
        val result = joinRequestRepository.delete(joinRequestId).map {"OK"}
        return DataOrError.fromEither(result, log)
    }
}

class StringToGroupMembershipRequestOrderingConverter : Converter<String, GroupMembershipRequestOrdering> {
    companion object {
        val DEFAULT = GroupMembershipRequestOrdering.CTIME_DESC
    }

    override fun convert(source: String): GroupMembershipRequestOrdering {
        @Suppress("UselessCallOnNotNull")
        return if (source.isNullOrBlank()) {
            DEFAULT
        } else {
            try {
                GroupMembershipRequestOrdering.valueOf(source.uppercase(Locale.getDefault()))
            } catch (ex: IllegalArgumentException) {
                DEFAULT
            }
        }
    }
}