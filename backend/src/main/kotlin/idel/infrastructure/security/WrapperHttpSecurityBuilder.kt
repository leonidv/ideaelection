package idel.infrastructure.security

import idel.domain.UserRepository
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.config.annotation.SecurityConfigurer
import org.springframework.security.config.annotation.SecurityConfigurerAdapter
import org.springframework.security.config.annotation.web.HttpSecurityBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.DefaultSecurityFilterChain
import javax.servlet.Filter

typealias B = SecurityConfigurer<DefaultSecurityFilterChain, HttpSecurity>
typealias CB = Class<B>

class WrapperHttpSecurityBuilder(private val http: HttpSecurity,
                                 private val userRepository: UserRepository) : HttpSecurityBuilder<WrapperHttpSecurityBuilder> {

    override fun <C : Any?> setSharedObject(sharedType: Class<C>?, o: C) {
        this.http.setSharedObject(sharedType, o)
    }

    override fun <C : Any?> getSharedObject(sharedType: Class<C>?): C {
        return this.http.getSharedObject(sharedType)
    }

    override fun authenticationProvider(authenticationProvider: AuthenticationProvider): WrapperHttpSecurityBuilder {
        val provider = OAuth2AuthorityLoaderProxyProvider(authenticationProvider, userRepository)
        this.http.authenticationProvider(provider)
        return this
    }

    @Suppress("UNCHECKED_CAST")
    override fun <C : SecurityConfigurer<DefaultSecurityFilterChain, WrapperHttpSecurityBuilder>> getConfigurer(clazz: Class<C>): C {
        val cb = clazz as CB
        val cfgB = this.http.getConfigurer(cb)
        return cfgB as C
    }

    override fun addFilter(filter: Filter): WrapperHttpSecurityBuilder {
       this.http.addFilter(filter)
       return this
    }

    override fun userDetailsService(userDetailsService: UserDetailsService?): WrapperHttpSecurityBuilder {
        this.http.userDetailsService(userDetailsService)
        return this;
    }

    override fun build(): DefaultSecurityFilterChain {
        return this.http.build();
    }

    override fun addFilterAfter(filter: Filter?, afterFilter: Class<out Filter>?): WrapperHttpSecurityBuilder {
        this.http.addFilterAt(filter, afterFilter)
        return this;
    }

    override fun addFilterBefore(filter: Filter?, beforeFilter: Class<out Filter>?): WrapperHttpSecurityBuilder {
        this.http.addFilterBefore(filter, beforeFilter);
        return this;
    }

    override fun <C : SecurityConfigurer<DefaultSecurityFilterChain, WrapperHttpSecurityBuilder>?> removeConfigurer(clazz: Class<C>?): C {
        TODO("Not yet implemented")
    }

}