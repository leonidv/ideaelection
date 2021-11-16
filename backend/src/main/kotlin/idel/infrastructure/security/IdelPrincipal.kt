package idel.infrastructure.security

import com.nimbusds.jwt.JWT
import com.nimbusds.jwt.JWTClaimsSet
import idel.domain.SubscriptionPlan
import idel.domain.User
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt

class ClaimsNotFull(claim: String) : Exception("Claims not full, not found $claim")

data class IdelPrincipal(
    override val id: String,
    override val email: String,
    override val displayName: String,
    override val avatar: String,
    override val roles: Set<String>,
    override val subscriptionPlan: SubscriptionPlan
) : User {
    companion object {
        fun fromJwt(jwt: Jwt): IdelPrincipal {
            fun loadClaim(key: String): String = jwt.claims.getOrElse(key) {throw ClaimsNotFull(key)} as String
            return IdelPrincipal(
                id = jwt.subject,
                email = loadClaim("email"),
                displayName = loadClaim("displayName"),
                avatar = loadClaim("avatar"),
                roles = loadClaim("roles").split(",").toSet(),
                subscriptionPlan = SubscriptionPlan.valueOf(loadClaim(loadClaim("subscriptionPlan")))
            )
        }

        fun copyToClaims(user: User, jwt: JWTClaimsSet.Builder): JWTClaimsSet.Builder {
            return jwt.subject(user.id)
                .claim("email", user.email)
                .claim("displayName", user.displayName)
                .claim("avatar", user.avatar)
                .claim("roles", user.roles.joinToString(","))
                .claim("subscriptionPlan", user.subscriptionPlan)
        }
    }

}


class IdelAuthenticationToken(val jwt: Jwt, val user: IdelPrincipal) :
    AbstractAuthenticationToken(IdelAuthorities.from(user.roles)) {

    override fun getCredentials() = jwt // borrowed from JwtAuthenticationToken

    override fun getPrincipal() = user

    override fun isAuthenticated() = true // decoding of jwt is authentication
}

class IdelPrincipalJwtConvertor : Converter<Jwt, IdelAuthenticationToken> {
    override fun convert(jwt: Jwt): IdelAuthenticationToken {
        val principal = IdelPrincipal.fromJwt(jwt)
        return IdelAuthenticationToken(jwt, principal)
    }
}