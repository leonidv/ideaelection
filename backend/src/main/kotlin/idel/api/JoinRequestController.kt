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

    data class GroupId(val groupId: String);


    @PostMapping
    fun add(@AuthenticationPrincipal user: IdelOAuth2User, @RequestBody groupId: GroupId): EntityOrError<JoinRequest> {
        return DataOrError.fromEither(groupMembershipService.requestMembership(groupId.groupId, user.id), log)
    }


    @GetMapping(params = ["userId"])
    fun loadByUser(@AuthenticationPrincipal user: IdelOAuth2User,
                   @RequestParam(required = false, defaultValue = "0") first: Int,
                   @RequestParam(required = false, defaultValue = "10") last: Int,
                   @RequestParam(required = false, defaultValue = "") order: GroupMembershipRequestOrdering,
                   @RequestParam userId : String
    ): EntityOrError<List<JoinRequest>> {
        if (user.id != userId) {
           return DataOrError.forbidden("you can't get join requests another users")
        }

        val pagination = Repository.Pagination(first, last)

        val result = joinRequestRepository.loadByUser(userId, order, pagination)

        return DataOrError.fromEither(result, log)
    }

    @GetMapping(params = ["groupId"])
    fun loadByGroup(@AuthenticationPrincipal user: IdelOAuth2User,
                    @RequestParam(required = false, defaultValue = "0") first: Int,
                    @RequestParam(required = false, defaultValue = "10") last: Int,
                    @RequestParam(required = false, defaultValue = "") order: GroupMembershipRequestOrdering,
                    @RequestParam groupId: String): EntityOrError<List<JoinRequest>> {
        val pagination = Repository.Pagination(first, last)

        val result = joinRequestRepository.loadByGroup(groupId, order, pagination)

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