package idel.api

import arrow.core.Either
import arrow.core.computations.either
import idel.domain.*
import mu.KotlinLogging
import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/joinrequests")
class JoinRequestController(
    private val groupMembershipService: GroupMembershipService,
    private val joinRequestRepository: JoinRequestRepository,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val security: ApiSecurity
) : DataOrErrorHelper {
    override val log = KotlinLogging.logger {}

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

    private fun enrichJoinRequests(joinRequests: List<JoinRequest>): Either<DomainError, JoinRequestsPayload> {
        return either.eager {
            val groups = joinRequests.map {it.groupId}.toSet()
                .map {groupId -> groupRepository.load(groupId).bind()}
            val users = joinRequests.map {it.userId}.toSet()
                .map {userId -> userRepository.load(userId).bind()}
            JoinRequestsPayload(joinRequests, groups.toSet(), users.toSet())
        }
    }

    @PostMapping
    fun create(
        @AuthenticationPrincipal user: User,
        @RequestBody joinRequestParams: JoinRequestParams
    ): ResponseDataOrError<JoinRequestPayload> {
        return groupMembershipService.requestMembership(
            joiningKey = joinRequestParams.joiningKey,
            userId = user.id,
            message = joinRequestParams.message
        )
            .map(JoinRequestPayload::onlyJoinRequest)
            .asHttpResponse()
    }


    @GetMapping(params = ["userId"])
    fun loadByUser(
        @AuthenticationPrincipal user: User,
        @RequestParam(required = false, defaultValue = "") order: GroupMembershipRequestOrdering,
        @RequestParam userId: UUID,
        pagination: Repository.Pagination
    ): ResponseDataOrError<JoinRequestsPayload> {
        if (user.id != userId) {
            return DataOrError.forbidden("you can't get join requests from another users")
        }

        val defaultStatuses = setOf(AcceptStatus.UNRESOLVED, AcceptStatus.DECLINED)

        return fTransaction {
            either.eager {
                val joinRequests =
                    joinRequestRepository.loadByUser(userId, defaultStatuses, order, pagination).bind()
                enrichJoinRequests(joinRequests).bind()
            }
        }.asHttpResponse()


    }

    @GetMapping(params = ["groupId"])
    fun loadByGroup(
        @AuthenticationPrincipal user: User,
        @RequestParam(required = false, defaultValue = "") order: GroupMembershipRequestOrdering,
        @RequestParam groupId: GroupId,
        pagination: Repository.Pagination
    ): ResponseDataOrError<JoinRequestsPayload> {
        return security.group.asAdmin(groupId, user) {_ ->
            either.eager {
                val joinRequests =
                    joinRequestRepository.loadByGroup(groupId, setOf(AcceptStatus.UNRESOLVED), order, pagination).bind()
                enrichJoinRequests(joinRequests).bind()
            }
        }.asHttpResponse()
    }

    data class Resolution(val status: AcceptStatus)

    @PatchMapping("{joinRequestId}/status")
    fun resolve(
        @AuthenticationPrincipal user: User,
        @PathVariable joinRequestId: UUID,
        @RequestBody resolution: Resolution
    ): ResponseDataOrError<JoinRequestPayload> {
        val result = groupMembershipService.resolveRequest(joinRequestId, resolution.status)
            .map(JoinRequestPayload::onlyJoinRequest)
        return DataOrError.fromEither(result, log)
    }

    @DeleteMapping("{joinRequestId}")
    fun delete(
        @AuthenticationPrincipal user: User,
        @PathVariable joinRequestId: UUID
    ): ResponseDataOrError<JoinRequest> {
        val result = groupMembershipService.resolveRequest(joinRequestId, AcceptStatus.REVOKED)
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