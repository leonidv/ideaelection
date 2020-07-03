package idel.api

import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
class UserController {


    data class MeResult(val name: String, val provider: String, val authorities: List<String>)

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    fun me(authentication: Authentication): MeResult {
        return if (authentication is OAuth2AuthenticationToken) {
            val principal = authentication.principal
            MeResult(
                    name = principal.name,
                    provider = authentication.authorizedClientRegistrationId,
                    authorities = authentication.authorities.map {it.authority}
            )
        } else {
            return MeResult("", "", emptyList())
        }
    }
}