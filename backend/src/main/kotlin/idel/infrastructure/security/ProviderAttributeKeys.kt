package idel.infrastructure.security

import mu.KotlinLogging


class ProviderAttributeKeys(
    val provider: String,
    val externalIdKey: String,
    val displayNameKey: String,
    val emailKey: String,
    val avatarKey: String
) {
    companion object {
        private val log = KotlinLogging.logger {}

        private val GOOGLE = ProviderAttributeKeys(
            provider = "google",
            externalIdKey = "sub",
            displayNameKey = "name",
            emailKey = "email",
            avatarKey = "picture"
        )

        private val MICROSOFT = ProviderAttributeKeys(
            provider = "microsoft",
            externalIdKey = "id",
            displayNameKey = "displayName",
            emailKey = "email", //should to check
            avatarKey = "" // should take second request to MS https://docs.microsoft.com/en-us/graph/api/profilephoto-get?view=graph-rest-1.0
        )

        private val FACEBOOK = ProviderAttributeKeys(
            provider = "facebook",
            externalIdKey = "id",
            displayNameKey = "name",
            emailKey = "email",
            avatarKey = ""
        )

        private val attributesPerProvider: Map<String, ProviderAttributeKeys> =
            listOf(GOOGLE, MICROSOFT, FACEBOOK)
                .associateBy {it.provider}

        fun get(providerRegistrationId: String): ProviderAttributeKeys {
            return attributesPerProvider.getOrElse(providerRegistrationId) {
                printIncorrectRegistrationWarning(providerRegistrationId)
                throw IllegalStateException(
                    "Can't find mapping for registrationId = $providerRegistrationId. " +
                            "Supported providers = ${attributesPerProvider.keys}"
                )

            }
        }

        private fun printIncorrectRegistrationWarning(registrationId: String) {
            log.warn {
                "Possible you add unsupportable provider with registrationId = ${registrationId}," +
                        " registered mappings = ${attributesPerProvider.keys} "
            }
            log.warn {
                "If you are developer you should add mapping which allowed to standardize provider " +
                        "user's attributes to unified saedi model. See ${ProviderAttributeKeys.javaClass} for more details"
            }
        }
    }



    private fun safeAttribute(
        attributes: MutableMap<String, Any>,
        key: String,
        attributeMessageName: String,
        required: Boolean = true
    ): String {
        return if (attributes.containsKey(key)) {
            attributes[key]!! as String
        } else {
            if (required) {
                throw IllegalStateException(
                    "Can't find required attribute " +
                            "[$attributeMessageName], attributeKey = [$key], attributes = [$attributes], provider = $provider "
                )
            } else {
                log.warn {"Can't find attribute = ${attributeMessageName} by key = ${key}, provider = ${provider}. Attributes keys = [${attributes.keys}]"}
                ""
            }
        }
    }

    fun externalId(attributes: MutableMap<String, Any>): String = safeAttribute(
        attributes = attributes,
        key = this.externalIdKey,
        attributeMessageName = "externalId",
        required = true
    )

    fun email(attributes: MutableMap<String, Any>): String = safeAttribute(
        attributes = attributes,
        key = this.emailKey,
        attributeMessageName = "email",
        required = true
    )

    fun displayName(attributes: MutableMap<String, Any>): String = safeAttribute(
        attributes = attributes,
        key = this.displayNameKey,
        attributeMessageName = "displayName",
        required = true
    );

    fun avatar(attributes: MutableMap<String, Any>): String = safeAttribute(
        attributes = attributes,
        key = this.avatarKey,
        attributeMessageName = "avatar",
        required = false
    );
}