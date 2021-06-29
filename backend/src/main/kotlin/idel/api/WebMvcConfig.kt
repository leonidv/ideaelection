package idel.api

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@Configuration
class WebMvcConfig : WebMvcConfigurer {

    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(StringToIdeaSortingConverter())
        registry.addConverter(StringToGroupSortingConverter())
        registry.addConverter(StringToGroupMembershipRequestOrderingConverter())
    }
}


@Configuration
class IdelControllerAdvices {
    @Bean
    fun exceptionHandler() : ResponseEntityExceptionHandler {
        return DataOrErrorExceptionHandler()
    }
}


@ControllerAdvice
class DataOrErrorExceptionHandler : ResponseEntityExceptionHandler() {
    @Value("\${testmode}")
    private var testmode = false

    override fun handleExceptionInternal(ex: Exception, body: Any?, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<Any> {
        return if (testmode) {
            DataOrError.badRequest<Any>(ex) as ResponseEntity<Any>
        } else {
            super.handleExceptionInternal(ex, body, headers, status, request)
        }
    }
}