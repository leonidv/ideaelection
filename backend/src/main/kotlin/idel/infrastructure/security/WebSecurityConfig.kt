package idel.infrastructure.security

import idel.domain.UserRepository
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.password.NoOpPasswordEncoder
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler

@Configuration
@EnableWebSecurity
@Order(2)
class WebSecurityConfig(private val userRepository: UserRepository) : WebSecurityConfigurerAdapter() {
    val log = KotlinLogging.logger {}


    @Value("\${testmode}")
    var basicEnabled = false

    @Value("\${security.httpbasic.realm}")
    lateinit var basicRealmName: String


    override fun configure(http: HttpSecurity) {
        http.authorizeRequests {
            it.anyRequest().authenticated()
        }.exceptionHandling {
            it
                .authenticationEntryPoint(BearerTokenAuthenticationEntryPoint())
                .accessDeniedHandler(BearerTokenAccessDeniedHandler())
        }.sessionManagement {it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)}
//        http
//            .authorizeRequests()
//            .anyRequest()
//            .authenticated()

        http.anonymous().disable()

        if (basicEnabled) {
            log.warn("Basic Authentication is enabled, please DON'T USE this mode in the production")
            log.warn("By design of IdeaElection, you should use your organization SSO (Google OAuth, for example) for managing users")
            log.warn("Read testing.md file in the project documentation for more information.")

            log.debug("Basic Authentication realm=[${basicRealmName}]")
            http
                .httpBasic()
                .realmName(basicRealmName)
        }


        val oauth2LoginConfigurer = OAuth2LoginConfigurer<HttpSecurity>();
        val customOAuth2LoginConfigurer = WrapperOAuth2LoginConfigurer(oauth2LoginConfigurer, userRepository)
        http.csrf().disable()
        http.apply(customOAuth2LoginConfigurer)


    }

    override fun configure(web: WebSecurity) {
         web.debug(true)
    }

    @Suppress("DEPRECATION")
    override fun configure(auth: AuthenticationManagerBuilder) {
        val authProvider = DaoAuthenticationProvider()
        authProvider.setPasswordEncoder(NoOpPasswordEncoder.getInstance())
        authProvider.setUserDetailsService(TestUsersDetailsService(userRepository))
        auth.authenticationProvider(authProvider)
    }

}

@Configuration
@Order(1)
class Probes : WebSecurityConfigurerAdapter() {
    override fun configure(web: WebSecurity) {
        web.debug(true)
    }

    override fun configure(http: HttpSecurity) {
        http
            .antMatcher("/m/info").anonymous()
            //.antMatcher("/probes/**").anonymous()

    }
}