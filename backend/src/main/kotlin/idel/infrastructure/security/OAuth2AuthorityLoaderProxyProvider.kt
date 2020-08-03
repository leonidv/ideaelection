package idel.infrastructure.security

import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken
import org.springframework.security.oauth2.core.user.OAuth2User

class OAuth2AuthorityLoaderProxyProvider(private val provider: AuthenticationProvider) : AuthenticationProvider {
    override fun authenticate(authentication: Authentication?): Authentication? {
        val token = this.provider.authenticate(authentication) ?: return null
        return if (token is OAuth2LoginAuthenticationToken) {
            val oauth2token : OAuth2LoginAuthenticationToken = token

            val authorities = mutableListOf<GrantedAuthority>(SimpleGrantedAuthority("IE_ADMIN"))
            authorities.addAll(oauth2token.authorities)

//            val principal = token.principal as OAuth2User;
//            val idelUser = IdelOAuth2User(
//                    authorities = principal.authorities,
//                    attributes = principal.attributes,
//                    provider = oauth2token.clientRegistration.registrationId,
//                    nameAttributeKey = "",
//                    emailAttributeKey = "",
//                    photoAttributeKey = ""
//            )

            val newToken = OAuth2LoginAuthenticationToken(
                    token.clientRegistration,
                    token.authorizationExchange,
                    token.principal,
                    authorities,
                    token.accessToken,
                    token.refreshToken
            )
            newToken.details = token.details
            newToken
        } else {
            token
        }

    }

    override fun supports(authentication: Class<*>?): Boolean {
        return provider.supports(authentication)
    }

}