package idel.infrastructure.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService

class TestUser(authorities: MutableCollection<out GrantedAuthority>,
               attributes: MutableMap<String, Any>,
               private val password: String,
               private val username: String,
               attributesNames: AttributesNames
) :
        IdelOAuth2User(authorities, attributes, "httpbasic", attributesNames), UserDetails {
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
class TestUsersDetailsService : UserDetailsService {
  override fun loadUserByUsername(username: String): TestUser {
        val role = username.split("__").last()

        val authorities =  when(role.toLowerCase()) {
            "super_user" -> mutableSetOf(IdelAuthorities.SUPER_USER_AUTHORITY)
            else -> mutableSetOf(IdelAuthorities.USER_AUTHORITY)
        }

      val attributesNames = IdelOAuth2User.AttributesNames(
              externalId = "providerId",
              displayName = "displayName",
              email = "email",
              avatar = "avatar"
      )

        val attributes = mutableMapOf<String,Any>(
                attributesNames.externalId to username,
                attributesNames.displayName to "${username} ${username}",
                attributesNames.email to "${username}@mail",
                attributesNames.avatar to ""
        )

        return TestUser(
                authorities = authorities,
                attributes = attributes,
                username = username,
                password = username,
                attributesNames = attributesNames
        )
    }
}