package idel.infrastructure.security

import com.nimbusds.jwt.JWTClaimsSet
import idel.domain.SubscriptionPlan
import idel.domain.User
import idel.domain.UserInfo
import idel.domain.generateId
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import java.util.*

class ClaimsNotFull(claim: String) : Exception("Claims not full, not found $claim")

data class IdelPrincipal(
    override val id: UUID,
    override val externalId: String,
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
                id = UUID.fromString(jwt.subject),
                externalId = loadClaim("externalId"),
                email = UserInfo.normalizeEmail(loadClaim("email")),
                displayName = loadClaim("displayName"),
                avatar = loadClaim("avatar"),
                roles = loadClaim("roles").split(",").toSet(),
                subscriptionPlan = SubscriptionPlan.valueOf(loadClaim("subscriptionPlan"))
            )
        }

        fun copyToClaims(user: User, jwt: JWTClaimsSet.Builder): JWTClaimsSet.Builder {
            return jwt.subject(user.id.toString())
                .claim("externalId",user.externalId)
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