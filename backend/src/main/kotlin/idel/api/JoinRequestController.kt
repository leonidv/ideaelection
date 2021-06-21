package idel.api

import idel.domain.*
import idel.infrastructure.security.IdelOAuth2User
import mu.KotlinLogging
import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.lang.IllegalArgumentException

@RestController
@RequestMapping("/joinrequests")
class JoinRequestController(val groupMembershipService: GroupMembershipService,
                            val joinRequestRepository: JoinRequestRepository) {
    private val log = KotlinLogging.logger {}

    data class JoinRequestParams(
        val joiningKey: String,
        val message: String
    );


    @PostMapping
    fun create(@AuthenticationPrincipal user: IdelOAuth2User, @RequestBody joinRequestParams: JoinRequestParams): EntityOrError<JoinRequest> {
        val eJoinRequest = groupMembershipService.requestMembership(
            joiningKey = joinRequestParams.joiningKey,
            userId = user.id,
            message = joinRequestParams.message)
        return DataOrError.fromEither(eJoinRequest, log)
    }


    @GetMapping(params = ["userId"])
    fun loadByUser(@AuthenticationPrincipal user: IdelOAuth2User,
                   @RequestParam(required = false, defaultValue = "") order: GroupMembershipRequestOrdering,
                   @RequestParam userId : String,
                   pagination: Repository.Pagination
    ): EntityOrError<List<JoinRequest>> {
        if (user.id != userId) {
           return DataOrError.forbidden("you can't get join requests another users")
        }

        val result = joinRequestRepository.loadByUser(userId, order, pagination)

        return DataOrError.fromEither(result, log)
    }

    @GetMapping(params = ["groupId"])
    fun loadByGroup(@AuthenticationPrincipal user: IdelOAuth2User,
                    @RequestParam(required = false, defaultValue = "") order: GroupMembershipRequestOrdering,
                    @RequestParam groupId: String,
                    pagination: Repository.Pagination
    ): EntityOrError<List<JoinRequest>> {
        val result = joinRequestRepository.loadByGroup(groupId, order, pagination)

        return DataOrError.fromEither(result, log)
    }

    data class Resolution(val status : AcceptStatus)
    @PatchMapping("{joinRequestId}/status")
    fun resolve(@AuthenticationPrincipal user: IdelOAuth2User,
                @PathVariable joinRequestId : String,
                @RequestBody resolution : Resolution) : EntityOrError<JoinRequest> {
        val result = groupMembershipService.resolveRequest(joinRequestId, resolution.status)
        return DataOrError.fromEither(result, log)
    }
}

class StringToGroupMembershipRequestOrderingConverter : Converter<String, GroupMembershipRequestOrdering> {
    val DEFAULT = GroupMembershipRequestOrdering.CTIME_DESC

    override fun convert(source: String): GroupMembershipRequestOrdering {
        return if (source.isNullOrBlank()) {
            DEFAULT
        } else {
            try {
                GroupMembershipRequestOrdering.valueOf(source.toUpperCase())
            } catch (ex: IllegalArgumentException) {
                DEFAULT
            }
        }
    }
}