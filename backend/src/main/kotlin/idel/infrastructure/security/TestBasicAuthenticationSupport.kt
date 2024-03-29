package idel.infrastructure.security

import arrow.core.Either
import idel.domain.*
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import java.util.*

class TestUser(
    override val id: UUID,
    authorities: MutableCollection<out GrantedAuthority>,
    attributes: MutableMap<String, Any>,
    private val password: String,
    private val username: String
) :
    IdelOAuth2User(
        id = UUID.randomUUID(),
        authorities = authorities,
        attributes = attributes,
        provider = PROVIDER,
        providerAttributeKeys = ATTRIBUTES_NAMES
    ), UserDetails {
    companion object {
        const val PROVIDER = "httpbasic"

        val ATTRIBUTES_NAMES = ProviderAttributeKeys(
            provider = PROVIDER,
            externalIdKey = "providerId",
            displayNameKey = "displayName",
            emailKey = "email",
            avatarKey = "avatar"
        )

    }

    override fun isEnabled() = true

    override fun getUsername() = username

    override fun isCredentialsNonExpired() = true

    override fun getPassword() = password

    override fun isAccountNonExpired() = true

    override fun isAccountNonLocked() = true

}

/**
 * Allow any username. Create new user with password equals username, if username not found.
 */
class TestUsersDetailsService(val userRepository: UserRepository) : UserDetailsService {
    companion object {
        const val PROVIDER = "httpbasic"

        fun makeExternalId(username: String): String = "$username@${TestUser.PROVIDER}"

    }

    override fun loadUserByUsername(username: String): TestUser {
        val role = username.split("__").last()

        // Разобраться, где здесь externalId

        val eUser = fTransaction {
            userRepository.loadByExternalId(makeExternalId(username))
        }

        return when (eUser) {
            is Either.Right -> {
                val user = eUser.value
                val attributes = mutableMapOf<String, Any>(
                    TestUser.ATTRIBUTES_NAMES.externalIdKey to username,
                    TestUser.ATTRIBUTES_NAMES.displayNameKey to user.displayName,
                    TestUser.ATTRIBUTES_NAMES.emailKey to user.email,
                    TestUser.ATTRIBUTES_NAMES.avatarKey to user.avatar
                )

                TestUser(
                    id = eUser.value.id,
                    authorities = IdelAuthorities.from(user.roles).toMutableList(),
                    attributes = attributes,
                    username = username,
                    password = username
                )
            }

            is Either.Left -> {
                if (eUser.value !is EntityNotFound) {
                    val error = eUser.value
                    if (error is ExceptionError) {
                        throw error.ex
                    } else {
                        throw IllegalArgumentException(error.message)
                    }

                }

                val authorities = when (role.lowercase(Locale.getDefault())) {
                    "super_user" -> mutableSetOf(IdelAuthorities.SUPER_USER_AUTHORITY)
                    else -> mutableSetOf(IdelAuthorities.USER_AUTHORITY)
                }

                val attributes = mutableMapOf<String, Any>(
                    TestUser.ATTRIBUTES_NAMES.externalIdKey to username,
                    TestUser.ATTRIBUTES_NAMES.displayNameKey to "${username} ${username}",
                    TestUser.ATTRIBUTES_NAMES.emailKey to "${username}@mail".lowercase(),
                    TestUser.ATTRIBUTES_NAMES.avatarKey to ""
                )

                return TestUser(
                    id = UUID.randomUUID(),
                    authorities = authorities,
                    attributes = attributes,
                    username = username,
                    password = username,
                )
            }
        }
    }
}