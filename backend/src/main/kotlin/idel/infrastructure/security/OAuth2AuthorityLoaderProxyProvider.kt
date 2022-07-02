package idel.infrastructure.security

import arrow.core.Either
import arrow.core.None
import arrow.core.Some
import idel.domain.*
import mu.KotlinLogging
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken
import java.util.UUID

class OAuth2AuthorityLoaderProxyProvider(
    private val provider: AuthenticationProvider,
    private val userRepository: UserRepository,
    private val userService: UserService
) : AuthenticationProvider {

    val log = KotlinLogging.logger {}


    override fun authenticate(authentication: Authentication?): Authentication? {
        // process default case when token is null
        val token = this.provider.authenticate(authentication) ?: return null

        if (token !is OAuth2LoginAuthenticationToken) return token

        val oauth2token: OAuth2LoginAuthenticationToken = token

        val registrationId = oauth2token.clientRegistration.registrationId
        val attributesNames = ProviderAttributeKeys.get(registrationId);
        return makeIdelOAuth2User(oauth2token, attributesNames, userRepository)
    }


    /**
     * Make new principal as [IdelOAuth2User].
     */
    private fun makeIdelOAuth2User(
        oauth2token: OAuth2LoginAuthenticationToken,
        providerAttributeKeys: ProviderAttributeKeys,
        userRepository: UserRepository
    ): OAuth2LoginAuthenticationToken {
        val principal = oauth2token.principal!!

        val externalId = providerAttributeKeys.externalId(principal.attributes)

        val userFromRepository = when (val eUser = fTransaction {userRepository.loadByExternalId(externalId)}) {
            is Either.Left -> when (val error = eUser.value) {
                is EntityNotFound -> None
                is ExceptionError -> throw error.ex
                else -> throw RuntimeException("Can't authenticate user, cause = [${error.message}]")
            }
            is Either.Right -> Some(eUser.value)
        }


        val idelUser: IdelOAuth2User = when (userFromRepository) {
            is Some -> {
                val user = userFromRepository.value
                val userRoles = userFromRepository.value.roles
                val authorities = IdelAuthorities.from(userRoles).toMutableList()
                val oAuth2User = IdelOAuth2User(
                    id = user.id,
                    authorities = authorities,
                    attributes = principal.attributes,
                    provider = oauth2token.clientRegistration.registrationId,
                    providerAttributeKeys = providerAttributeKeys
                )
                // update user info from actual provider information. For example, change avatar.
                // TODO shouldn't update if information changed from Saedi UI
                fTransaction {userRepository.update(oAuth2User.copy(authorities))}
                oAuth2User
            }

            is None -> {
                val newUser = IdelOAuth2User(
                    id = UUID.randomUUID(),
                    authorities = mutableListOf(IdelAuthorities.USER_AUTHORITY),
                    attributes = principal.attributes,
                    provider = oauth2token.clientRegistration.registrationId,
                    providerAttributeKeys = providerAttributeKeys
                )

                userService.register(newUser)
                newUser
            }
        }


        val newToken = OAuth2LoginAuthenticationToken(
            oauth2token.clientRegistration,
            oauth2token.authorizationExchange,
            idelUser,
            idelUser.authorities,
            oauth2token.accessToken,
            oauth2token.refreshToken
        )
        newToken.details = oauth2token.details
        return newToken
    }


    override fun supports(authentication: Class<*>?): Boolean {
        return provider.supports(authentication)
    }

}