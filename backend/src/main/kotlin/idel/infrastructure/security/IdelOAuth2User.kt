package idel.infrastructure.security

import idel.domain.User
import mu.KotlinLogging
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import java.lang.IllegalStateException


open class IdelOAuth2User(
        authorities: MutableCollection<out GrantedAuthority>,
        attributes: MutableMap<String, Any>,
        private val provider: String,
        private val attributesNames : AttributesNames
) : DefaultOAuth2User(authorities, attributes, attributesNames.externalId), User {

    class AttributesNames(val externalId : String, val displayName : String, val email : String, val avatar : String)

    private val log = KotlinLogging.logger {}

    private fun safeAttribute(key: String, attributeMessageName: String, required: Boolean = true): String {
        return if (attributes.containsKey(key)) {
            getAttribute(key)!!
        } else {
            if (required) {
                throw IllegalStateException("Can't find required attribute " +
                        "[$attributeMessageName], attributeKey = [$key], attributes = [$attributes], provider = $provider ")
            } else {
                log.warn {"Can't find attribute = ${attributeMessageName} by key = ${key}, provider = ${provider}. Attributes keys = [${attributes.keys}]"}
                ""
            }
        }
    }

    /**
     * Identifier of user in provider's system.
     */
    val externalId = safeAttribute(attributesNames.externalId,"externalId")

    override val email: String = safeAttribute(attributesNames.email, "email")
    override val displayName: String = safeAttribute(attributesNames.displayName,"displayName")
    override val avatar: String = safeAttribute(attributesNames.avatar, "avatar", required = false)

    override fun id() = "${externalId}@${provider}"

    override val roles = IdelAuthorities.asRoles(this.authorities).toSet()

    fun copy(authorities: MutableCollection<out GrantedAuthority>) : IdelOAuth2User {
        return IdelOAuth2User(
                authorities = authorities,
                attributes = this.attributes,
                attributesNames = this.attributesNames,
                provider = this.provider
        )
    }

    override fun toString(): String {
        return "IdelOAuth2User(provider='$provider', externalId='$externalId', email='$email', displayName='$displayName', avatar='$avatar', roles=$roles)"
    }


}