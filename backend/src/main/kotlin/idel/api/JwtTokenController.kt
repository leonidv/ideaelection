package idel.api

import arrow.core.continuations.either
import idel.domain.DomainError
import idel.domain.User
import idel.domain.UserRepository
import idel.domain.fTransaction
import idel.infrastructure.security.JwtIssuerService
import mu.KotlinLogging
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/token")
class JwtTokenController(
    val userRepository: UserRepository,
    val jwtIssuer: JwtIssuerService
) {

    private val log = KotlinLogging.logger {}

    @GetMapping("")
    fun issueToken(@AuthenticationPrincipal user: User): ResponseDataOrError<String> {
        val result = fTransaction {
            either.eager<DomainError, String> {
                val currentUser = userRepository.load(user.id).bind()
                jwtIssuer.issueToken(currentUser)
            }
        }

        return DataOrError.fromEither(result, log)
    }

}