package idel.api

import idel.domain.GroupMembershipService
import idel.domain.JoinRequest
import idel.domain.JoinRequestRepository
import idel.infrastructure.security.IdelOAuth2User
import mu.KotlinLogging
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/joinrequests")
class JoinRequestController(val groupMembershipService: GroupMembershipService,
                            val joinRequestRepository: JoinRequestRepository) {
    private val log = KotlinLogging.logger {}

    data class GroupId(val groupId: String);

//    @PostMapping(value = [""])
//    fun add(@AuthenticationPrincipal user: IdelOAuth2User,
//            @RequestBody groupId: GroupId) : EntityOrError<JoinRequest> {
//        //val joinRequest = MembershipRequest.
//        //joinRequestRepository.add()
//        return ResponseOrError.notImplemented()
//    }

    @PostMapping
    fun add(@AuthenticationPrincipal user: IdelOAuth2User, @RequestBody groupId: GroupId): EntityOrError<JoinRequest> {
        return DataOrError.fromEither(groupMembershipService.requestMembership(groupId.groupId, user.id), log)
    }


    @GetMapping(params = ["userId"])
    fun loadByUser(@AuthenticationPrincipal user: IdelOAuth2User,
                   @RequestParam userId: String): EntityOrError<String> {
        return DataOrError.ok("user is $userId")
    }

    @GetMapping(params = ["groupId"])
    fun loadByGroup(@AuthenticationPrincipal user: IdelOAuth2User,
                    @RequestParam groupId: String): EntityOrError<String> {
        return DataOrError.ok("group is $groupId")
    }
}