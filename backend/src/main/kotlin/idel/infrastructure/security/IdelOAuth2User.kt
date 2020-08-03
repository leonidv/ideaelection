package idel.infrastructure.security

import idel.domain.User
import mu.KotlinLogging
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.user.DefaultOAuth2User


open class IdelOAuth2User(
        authorities: MutableCollection<out GrantedAuthority>,
        attributes: MutableMap<String, Any>,
        private val provider: String,
        attributesNames : AttributesNames
) : DefaultOAuth2User(authorities, attributes, attributesNames.providerId), User {

    class AttributesNames(val providerId : String, val displayName : String, val email : String, val avatar : String)

    private val log = KotlinLogging.logger {}

    private fun safeAttribute(key: String, attributeMessageName: String): String {
        return if (attributes.containsKey(key)) {
            getAttribute(key)!!
        } else {
            log.warn {"Can't find attribute = ${attributeMessageName} by key = ${key}, provider = ${provider}. Attributes keys = [${attributes.keys}]"}
            ""
        }
    }

    val providerId = safeAttribute(attributesNames.providerId,"providerId")

    override val email: String = safeAttribute(attributesNames.email, "email")
    override val displayName: String = safeAttribute(attributesNames.displayName,"displayName")
    override val avatar: String = safeAttribute(attributesNames.avatar, "avatar")

    override fun id() = "${providerId}@${provider}"

    override val roles = this.authorities.map {it.authority}.toSet()
}