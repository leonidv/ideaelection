package idel.api

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import idel.domain.Roles
import idel.domain.User
import idel.infrastructure.security.IdelAuthorities
import idel.infrastructure.security.IdelOAuth2User
import idel.infrastructure.security.IdelPrincipal
import idel.infrastructure.security.JwtIssuerService
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.Mapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.RSAPrivateKey
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/token")
class JwtTokenController(val jwtIssuer : JwtIssuerService) {

    @GetMapping("")
    fun issueToken(@AuthenticationPrincipal user: User): String {
        return jwtIssuer.issueToken(user)
    }

}