package idel.infrastructure.security

import idel.domain.User
import mu.KotlinLogging
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.user.DefaultOAuth2User

open class IdelOAuth2User(
        authorities: MutableCollection<out GrantedAuthority>,
        attributes: MutableMap<String, Any>,
        private val provider: String,
        nameAttributeKey: String,
        emailAttributeKey: String,
        photoAttributeKey: String
) : DefaultOAuth2User(authorities, attributes, nameAttributeKey), User {
    private val log = KotlinLogging.logger {}

    private fun safeAttribute(key: String, attribute: String): String {
        return if (attributes.containsKey(key)) {
            getAttribute(key)!!
        } else {
            log.warn {"Can't find attribute = ${attribute} by key = ${key}, provider = ${provider}. Attributes keys = [${attributes.keys}]"}
            ""
        }
    }

    override val email: String = safeAttribute(emailAttributeKey, "email")
    override val profileName: String = name
    override val profilePhoto: String = safeAttribute(photoAttributeKey, "photo")

    override fun id() = "${name}@${provider}"
}