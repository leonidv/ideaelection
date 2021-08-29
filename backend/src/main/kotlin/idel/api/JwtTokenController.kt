package idel.api

import idel.domain.User
import idel.infrastructure.security.JwtIssuerService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/token")
class JwtTokenController(val jwtIssuer: JwtIssuerService) {

    @GetMapping("")
    fun issueToken(@AuthenticationPrincipal user: User): String {
        return jwtIssuer.issueToken(user)
    }

}