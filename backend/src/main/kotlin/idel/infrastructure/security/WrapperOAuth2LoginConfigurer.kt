package idel.infrastructure.security

import org.springframework.security.config.annotation.ObjectPostProcessor
import org.springframework.security.config.annotation.web.HttpSecurityBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher

class WrapperOAuth2LoginConfigurer<B : HttpSecurityBuilder<B>>(
        private val cfg : OAuth2LoginConfigurer<B>) :
        AbstractAuthenticationFilterConfigurer<B, OAuth2LoginConfigurer<B>, OAuth2LoginAuthenticationFilter>()
{

    @Suppress("UNCHECKED_CAST")
    private fun wrap(http : B) : B = WrapperHttpSecurityBuilder(http as HttpSecurity) as B

    override fun createLoginProcessingUrlMatcher(loginProcessingUrl: String?): RequestMatcher {
        return AntPathRequestMatcher(loginProcessingUrl)
    }

    override fun configure(http: B) {
        this.cfg.configure(wrap(http))
    }

    override fun init(http: B) {
        this.cfg.init(wrap(http))
    }

    override fun setBuilder(builder: B) {
        this.cfg.setBuilder(wrap(builder))
    }

    override fun and(): B {
        return this.cfg.and()
    }

    override fun loginProcessingUrl(loginProcessingUrl: String?): OAuth2LoginConfigurer<B> {
        return this.cfg.loginProcessingUrl(loginProcessingUrl)
    }

    override fun withObjectPostProcessor(objectPostProcessor: ObjectPostProcessor<*>?): OAuth2LoginConfigurer<B> {
        return this.cfg.withObjectPostProcessor(objectPostProcessor)
    }

    override fun addObjectPostProcessor(objectPostProcessor: ObjectPostProcessor<*>?) {
        this.cfg.addObjectPostProcessor(objectPostProcessor)
    }

    override fun disable(): B {
        return this.cfg.disable()
    }
}