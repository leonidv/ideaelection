package idel.infrastructure.security

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.NoOpPasswordEncoder

@Configuration
@EnableWebSecurity
class WebSecurityConfig : WebSecurityConfigurerAdapter() {
    val log = KotlinLogging.logger {}


    @Value("\${security.httpbasic.enabled}")
    var basicEnabled = false

    @Value("\${security.httpbasic.realm}")
    lateinit var basicRealmName: String

    override fun configure(http: HttpSecurity) {
        http
            .authorizeRequests()
            .anyRequest()
            .authenticated()

        http.anonymous().disable()

        if (basicEnabled) {
            log.warn("Basic Authentication is enabled, please don't use this mode in the production")
            log.warn("By design of IdeaElection, you should use your organization SSO (Google OAuth, for example) for managing users")
            log.warn("Read testing.md file in the project documentation for more information.")

            log.debug("Basic Authentication realm=[${basicRealmName}]")
            http
                .httpBasic()
                .realmName(basicRealmName)

            http.csrf().disable()
        }

        val oauth2LoginConfigurer = OAuth2LoginConfigurer<HttpSecurity>();
        val customOAuth2LoginConfigurer = WrapperOAuth2LoginConfigurer(oauth2LoginConfigurer)
        http.apply(customOAuth2LoginConfigurer)


    }

    override fun configure(web: WebSecurity) {
        web.debug(true)
    }

    @Suppress("DEPRECATION")
    override fun configure(auth: AuthenticationManagerBuilder) {
        val authProvider = DaoAuthenticationProvider()
        authProvider.setPasswordEncoder(NoOpPasswordEncoder.getInstance())
        authProvider.setUserDetailsService(TestUsersDetailsService())
        auth.authenticationProvider(authProvider)
    }

}

class TestUser(authorities: MutableCollection<out GrantedAuthority>,
               attributes: MutableMap<String, Any>,
               private val password: String,
               private val username: String
) :
        IdelOAuth2User(authorities, attributes, "basic", "name", "email", "photo"), UserDetails {
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
        val authority =
                if (username.endsWith("_group_admin", ignoreCase = true)) {
                    IdelAuthorities.GROUP_ADMIN_AUTHORITY
                } else {
                    IdelAuthorities.USER_AUTHORITY
                }
        val attributes = mutableMapOf<String,Any>(
                "name" to "${username} ${username}",
                "email" to "${username}@mail",
                "photo" to ""
        )


        return TestUser(
                authorities = mutableSetOf(authority),
                attributes = attributes,
                username = username,
                password = username
        )
    }
}