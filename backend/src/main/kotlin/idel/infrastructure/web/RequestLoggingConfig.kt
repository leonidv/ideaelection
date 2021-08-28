package idel.infrastructure.web

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.WebApplicationInitializer
import org.springframework.web.filter.CommonsRequestLoggingFilter
import javax.servlet.ServletContext

@Configuration
class RequestLoggingConfig  {

    @Bean
    fun logFilter(): FilterRegistrationBean<CommonsRequestLoggingFilter> {
        val filterRegistration = FilterRegistrationBean<CommonsRequestLoggingFilter>()
        filterRegistration.setName("saediFilterRegistration")
        filterRegistration.order = -200
        val filter = CommonsRequestLoggingFilter()
        filter.setIncludeQueryString(true)
        filter.setIncludeHeaders(true)
        filter.setIncludeClientInfo(true)

        filterRegistration.filter = filter
        filterRegistration.addUrlPatterns("/*")

        return filterRegistration
    }

}