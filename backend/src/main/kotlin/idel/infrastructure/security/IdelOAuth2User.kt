package idel.infrastructure.security

import idel.domain.SubscriptionPlan
import idel.domain.User
import mu.KotlinLogging
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import java.util.*


open class IdelOAuth2User(
        authorities: MutableCollection<out GrantedAuthority>,
        attributes: MutableMap<String, Any>,
        override val id : UUID,
        private val provider: String,
        private val providerAttributeKeys : ProviderAttributeKeys
) : DefaultOAuth2User(authorities, attributes, providerAttributeKeys.externalIdKey), User {

    private val log = KotlinLogging.logger {}

    /**
     * Identifier of user in provider's system.
     */
    override val externalId = providerAttributeKeys.externalId(attributes)
    override val email: String = providerAttributeKeys.email(attributes)
    override val displayName: String = providerAttributeKeys.displayName(attributes)
    override val avatar: String = providerAttributeKeys.avatar(attributes)

    override val roles = IdelAuthorities.asRoles(this.authorities).toSet()

    override val subscriptionPlan: SubscriptionPlan = SubscriptionPlan.FREE

    fun copy(authorities: MutableCollection<out GrantedAuthority>) : IdelOAuth2User {
        return IdelOAuth2User(
                id = this.id,
                authorities = authorities,
                attributes = this.attributes,
                providerAttributeKeys = this.providerAttributeKeys,
                provider = this.provider
        )
    }

    override fun toString(): String {
        return "IdelOAuth2User(provider='$provider', externalId='$externalId', email='$email', displayName='$displayName', avatar='$avatar', roles=$roles)"
    }


}