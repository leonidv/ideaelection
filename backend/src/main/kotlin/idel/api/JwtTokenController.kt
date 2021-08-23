package idel.api

import idel.infrastructure.security.IdelOAuth2User
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.Mapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/token")
class JwtTokenController {

    @GetMapping("")
    fun issueToken(@AuthenticationPrincipal user: IdelOAuth2User) : String {
        val now = Instant.now()
        val expire = 36000L
//        val scope = user.
    return ":)"
    }
}