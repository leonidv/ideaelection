package idel.api

import idel.domain.GroupMembershipService
import idel.domain.JoinRequest
import idel.domain.JoinRequestRepository
import idel.domain.MembershipRequest
import idel.infrastructure.security.IdelOAuth2User
import mu.KLogger
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/joinrequests")
class JoinRequestController(val groupMembershipService: GroupMembershipService) {
    private val log = KotlinLogging.logger {}

    data class GroupId(val groupId : String);

//    @PostMapping(value = [""])
//    fun add(@AuthenticationPrincipal user: IdelOAuth2User,
//            @RequestBody groupId: GroupId) : EntityOrError<JoinRequest> {
//        //val joinRequest = MembershipRequest.
//        //joinRequestRepository.add()
//        return ResponseOrError.notImplemented()
//    }

    @PostMapping
   fun add(@AuthenticationPrincipal user : IdelOAuth2User, @RequestBody groupId: GroupId) : EntityOrError<JoinRequest> {
       return ResponseOrError.fromEither(groupMembershipService.requestMembership(groupId.groupId, user.id()), log)
   }
}