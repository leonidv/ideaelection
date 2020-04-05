package ideael.infrastructure.configs

import ideael.domain.Voter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.NoOpPasswordEncoder
import java.util.*
import java.util.concurrent.ConcurrentHashMap


@Configuration
@EnableWebSecurity
class TestWebSecurityConfig : WebSecurityConfigurerAdapter() {
    val log = LoggerFactory.getLogger(TestWebSecurityConfig::class.java)!!


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


    }

    @Suppress("DEPRECATION")
    override fun configure(auth: AuthenticationManagerBuilder) {
        val authProvider = DaoAuthenticationProvider()
        authProvider.setPasswordEncoder(NoOpPasswordEncoder.getInstance())
        authProvider.setUserDetailsService(this.makeDetailsService())
        auth.authenticationProvider(authProvider)
    }

    fun makeDetailsService(): UserDetailsService {
        /**
         * Order of services is important:
         * 1. In first we try to find user in Couchbase, this standard way to make authentication.
         * 2. If we did not find user in Couchbase and test mode is enabled, we make fake authentication
         *
         * This order allows QA engineers to test UI (with OAuth authentication) and API (with Basic authentication)
         *
         */
        val services = mutableListOf<KUserDetailsService>()

        if (basicEnabled) {
            services.add(TestUsersDetailsService())
        }

        return CompositeUsersDetailsService(services)

    }
}

class Y : User {
    constructor(username: String?, password: String?, authorities: MutableCollection<out GrantedAuthority>?) : super(
        username,
        password,
        authorities
    )
}

class VoterUser(username: String, password: String, authorities: MutableCollection<out GrantedAuthority>) :
    User(username, password, authorities) {

    fun getVoter() : Voter {
        return Voter(
            email = username,
            name = username,
            profilePhoto = ""
        )
    }
}

/**
 * Special version of @see UserDetailsService
 */
interface KUserDetailsService {
    /**
     * Humane readable name of service.
     */
    val name: String

    /**
     * Load user by name.
     *
     * @return Option with @see UserDetails if user has been load, or Optional.empty.
     * @see [UserDetailsService#loadUserByUsername]
     */
    fun loadUserByUsername(username: String): Optional<VoterUser>

}

class CompositeUsersDetailsService(private val services: List<KUserDetailsService>) : UserDetailsService {
    private val log = LoggerFactory.getLogger(CompositeUsersDetailsService::class.java)

    private val servicesNames = services.map { it.name };

    init {
        log.debug("services = ${servicesNames.joinToString(",", "[", "]")}")
    }

    override fun loadUserByUsername(username: String): VoterUser {
        lateinit var details: Optional<VoterUser>
        for (service in services) {
            details = service.loadUserByUsername(username)
            if (details.isPresent) {
                log.debug("Found user [${username}] with service.name=${service.name}")
                return details.get()
            }
        }

        log.debug("Can't find user [${username}] in [${servicesNames}]")
        throw UsernameNotFoundException("Can't find user [${username}]")
    }

}


/**
 * Allow any username. Create new user with password equals username, if username not found.
 */
class TestUsersDetailsService : KUserDetailsService {
    override val name = "test"

    // https://howtodoinjava.com/java/multi-threading/best-practices-for-using-concurrenthashmap/
    private val users = ConcurrentHashMap<String, VoterUser>(16, 0.9f, 1)


    override fun loadUserByUsername(username: String): Optional<VoterUser> {
        val user = users.getOrDefault(username, VoterUser(username, username, mutableSetOf()))
        return Optional.of(user)
    }
}