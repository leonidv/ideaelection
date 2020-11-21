package idel.api

import idel.domain.GroupMemberRepository
import idel.domain.SecurityService
import idel.infrastructure.security.IdelOAuth2User
import mu.KotlinLogging
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/groupmembers")
class GroupMemberController(val groupMemberRepository: GroupMemberRepository, securityService: SecurityService) {
    private val log = KotlinLogging.logger {}

//    private val security = ApiSecurity(securityService, log)

    data class KickRequest(val groupId : String, val userId : String)

    @DeleteMapping
    fun remove(@AuthenticationPrincipal user: IdelOAuth2User, @RequestBody request: KickRequest) : EntityOrError<String> {
//        return security.asHimselfOrAdmin(request.groupId, user, request.userId) {
//            val result = groupMemberRepository.removeFromGroup(request.groupId, request.userId)
//            result.map {"ok"};
//        }
        return DataOrError.notImplemented()
    }

}