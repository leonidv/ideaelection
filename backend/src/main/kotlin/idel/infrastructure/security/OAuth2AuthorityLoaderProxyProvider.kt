package idel.infrastructure.security

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import idel.domain.EntityNotFound
import idel.domain.UserRepository
import mu.KotlinLogging
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken

class OAuth2AuthorityLoaderProxyProvider(private val provider: AuthenticationProvider,
                                         private val userRepository: UserRepository) : AuthenticationProvider {

    val log = KotlinLogging.logger {}

    private val googleAttributes = IdelOAuth2User.AttributesNames(
            externalId = "sub",
            displayName = "name",
            email = "email",
            avatar = "picture"
    )

    private val microsoftAttributes = IdelOAuth2User.AttributesNames(
            externalId = "id",
            displayName = "displayName",
            email = "email", //should to check
            avatar = "" // should take second request to MS https://docs.microsoft.com/en-us/graph/api/profilephoto-get?view=graph-rest-1.0
    )

    private val facebookAttributes = IdelOAuth2User.AttributesNames(
            externalId = "id",
            displayName = "name",
            email = "email",
            avatar = ""
    )

    private val attributesPerProvider = mapOf(
            "google" to googleAttributes,
            "facebook" to facebookAttributes,
            "microsoft" to microsoftAttributes
    )

    override fun authenticate(authentication: Authentication?): Authentication? {
        // process default case when token is null
        val token = this.provider.authenticate(authentication) ?: return null

        if (token !is OAuth2LoginAuthenticationToken) return token

        val oauth2token: OAuth2LoginAuthenticationToken = token

        val registrationId = oauth2token.clientRegistration.registrationId
        val attributesNames = Option.fromNullable(attributesPerProvider[registrationId])
        return when (attributesNames) {
            is Some ->
                makeIdelOAuth2User(oauth2token, attributesNames, userRepository)

            is None -> {
                printIncorrectRegistrationWarning(registrationId)
                throw IllegalStateException("Can't find mapping for registrationId = $registrationId. " +
                        "Supported providers = ${attributesPerProvider.keys}")
            }
        }

    }

    /**
     * Make new principal as [IdelOAuth2User].
     */
    private fun makeIdelOAuth2User(oauth2token: OAuth2LoginAuthenticationToken,
                                   attributesNames: Some<IdelOAuth2User.AttributesNames>,
                                   userRepository: UserRepository
    ): OAuth2LoginAuthenticationToken {
//        val authorities = mutableListOf<GrantedAuthority>(SimpleGrantedAuthority("IE_ADMIN"))
//        authorities.addAll(oauth2token.authorities)

        val principal = oauth2token.principal!!

        // create user because user.id is required.
        val idelUser = IdelOAuth2User(
            authorities = mutableListOf(IdelAuthorities.USER_AUTHORITY),
            attributes = principal.attributes,
            provider = oauth2token.clientRegistration.registrationId,
            attributesNames = attributesNames.value
        )


        val userFromRepository = when (val eUser = userRepository.load(idelUser.id)) {
            is Either.Left -> when (val ex = eUser.value) {
                is EntityNotFound -> None
                else -> throw ex
            }
            is Either.Right -> Some(eUser.value)
        }


        val authorities = when (userFromRepository) {
            is Some -> {
                val userRoles = userFromRepository.value.roles
                val authorities = IdelAuthorities.from(userRoles).toMutableList()
                // update user info from actual provider information. For example, change avatar.
                userRepository.update(idelUser.copy(authorities))
                authorities
            }

            is None -> {
                userRepository.add(idelUser)
                idelUser.authorities
            }
        }


        val newToken = OAuth2LoginAuthenticationToken(
                oauth2token.clientRegistration,
                oauth2token.authorizationExchange,
                idelUser.copy(authorities),
                authorities,
                oauth2token.accessToken,
                oauth2token.refreshToken
        )
        newToken.details = oauth2token.details
        return newToken
    }

    private fun printIncorrectRegistrationWarning(registrationId: String) {
        log.warn {
            "Possible you add unsupportable provider with registrationId = ${registrationId}," +
                    " registered mappings = ${attributesPerProvider.keys} "
        }
        log.warn {
            "If you are developer you should add mapping which allowed to standardize provider " +
                    "user's attributes to unified idel model"
        }
    }

    override fun supports(authentication: Class<*>?): Boolean {
        return provider.supports(authentication)
    }

}