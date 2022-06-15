package idel.infrastructure.security

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import idel.api.ApiSecurity
import idel.domain.*
import idel.domain.security.SecurityService
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.NoOpPasswordEncoder
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler
import org.springframework.security.web.DefaultRedirectStrategy
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Configuration
@EnableWebSecurity
@Order(2)
class WebSecurityConfig(private val userRepository: UserRepository, private val userService: UserService) : WebSecurityConfigurerAdapter() {
    val log = KotlinLogging.logger {}


    @Value("\${testmode}")
    var testMode = false

    @Value("\${security.httpbasic.realm}")
    lateinit var basicRealmName: String

    @Value("\${jwt.public.key}")
    lateinit var publicKey: RSAPublicKey;


    @Value("\${jwt.private.key}")
    lateinit var privateKey: RSAPrivateKey

    @Value("\${jwt.frontend.url}")
    lateinit var frontendUrl : String

    @Value("\${security.cors.allowed-origins}")
    var allowedOrigins: Array<String> = emptyArray()


    override fun configure(http: HttpSecurity) {
        val corsCfg = CorsConfiguration()
        log.info {"CORS is enabled for origins: ${allowedOrigins.joinToString(prefix = "[", postfix = "]")}"}
        corsCfg.allowedOrigins = allowedOrigins.toList()
        corsCfg.allowedMethods = listOf("*")
        corsCfg.allowedHeaders = listOf("*")
        corsCfg.allowCredentials = true
        val corsSource = UrlBasedCorsConfigurationSource()
        corsSource.registerCorsConfiguration("/**", corsCfg)
        http.cors().configurationSource(corsSource)


        http.authorizeRequests {
            it.anyRequest().authenticated()
        }
            .csrf {it.ignoringAntMatchers("/token")}
            .oauth2ResourceServer {it.jwt()}
            .oauth2ResourceServer().jwt {cstm ->
                cstm.jwtAuthenticationConverter(IdelPrincipalJwtConvertor())
            }
        http.exceptionHandling {
            it
                .authenticationEntryPoint(BearerTokenAuthenticationEntryPoint())
                .accessDeniedHandler(BearerTokenAccessDeniedHandler())
        }
        http.anonymous().disable()


        if (testMode) {
            log.warn("! Basic Authentication is enabled, please DON'T USE this mode in the production")
            log.warn("! By design of IdeaElection, you should use your organization SSO (Google OAuth, for example) for managing users")
            log.warn("! Read testing.md file in the project documentation for more information.")

            log.debug("Basic Authentication realm=[${basicRealmName}]")
            http
                .httpBasic()
                .realmName(basicRealmName)

        }

        val oauth2LoginConfigurer = OAuth2LoginConfigurer<HttpSecurity>();
        oauth2LoginConfigurer.successHandler(Oauth2JwtTokenSuccesHandler(jwtIssuer(), frontendUrl))
        val customOAuth2LoginConfigurer = WrapperOAuth2LoginConfigurer(oauth2LoginConfigurer, userRepository, userService)
        http.csrf().disable()
        http.apply(customOAuth2LoginConfigurer)
    }

    @Bean
    fun jwtDecoder(): JwtDecoder {
        return NimbusJwtDecoder.withPublicKey(this.publicKey).build()
    }

    @Bean
    fun jwtIssuer(): JwtIssuerService {
        return JwtIssuerService(privateKey, TimeUnit.DAYS.toMillis(365L))
    }

    @Suppress("DEPRECATION")
    override fun configure(auth: AuthenticationManagerBuilder) {
        val authProvider = DaoAuthenticationProvider()
        authProvider.setPasswordEncoder(NoOpPasswordEncoder.getInstance())
        authProvider.setUserDetailsService(TestUsersDetailsService(userRepository))
        auth.authenticationProvider(authProvider)
    }

    @Bean
    fun ApiSecurity(
        userSecurity: UserSecurity ,
        groupSecurity: GroupSecurity,
        ideaSecurity: IdeaSecurity,
        groupMemberSecurity: GroupMemberSecurity,
        inviteSecurity: InviteSecurity
    ): ApiSecurity {
        return idel.api.ApiSecurity(userSecurity,groupSecurity,ideaSecurity,groupMemberSecurity, inviteSecurity)
    }
}

@Configuration
@Order(1)
class BuildInfo : org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter() {
    override fun configure(http: HttpSecurity) {
        http.requestMatchers()
            .antMatchers("/m/info")
            .and()
            .anonymous()
    }
}

class JwtIssuerService(val key: RSAPrivateKey, val timeToLive: Long) {

    fun issueToken(user: User): String {
        val now = Instant.now()
        val claimsBuilder = JWTClaimsSet.Builder()
            .issuer("saedi")
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(timeToLive)))
        IdelPrincipal.copyToClaims(user, claimsBuilder)

        val claims = claimsBuilder.build()
        val header = JWSHeader.Builder(JWSAlgorithm.RS256).build()

        val jwt = SignedJWT(header, claims)
        jwt.sign(RSASSASigner(key))
        return jwt.serialize()
    }
}

class Oauth2JwtTokenSuccesHandler(
    private val jwtIssuerService: JwtIssuerService,
    private val frontendUrl: String
) : AuthenticationSuccessHandler {
   private val redirectStrategy = DefaultRedirectStrategy()

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val jwt = jwtIssuerService.issueToken(authentication.principal as User)
        redirectStrategy.sendRedirect(request, response, "$frontendUrl?jwt=$jwt")
    }

}