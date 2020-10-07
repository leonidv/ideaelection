package idel.api

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.lang.Exception

@Configuration
class WebMvcConfig : WebMvcConfigurer {



    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(StringToIdeaSortingConverter())
        registry.addConverter(StringToGroupSortingConverter())
    }
}

//@ControllerAdvice("idel.api")
@Configuration
class IdelControllerAdvices {

//    fun exceptionHandler

    @Bean
    fun exceptionHandler() : ResponseEntityExceptionHandler {
        println("************************")
        return ResponseOrErrorExceptionHandler()
    }
}


@ControllerAdvice
class ResponseOrErrorExceptionHandler : ResponseEntityExceptionHandler() {
    init {
        println("!!!!!!!!!!!!!!!!!!!")
    }

    override fun handleExceptionInternal(ex: Exception, body: Any?, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<Any> {
        println(ex)
        return super.handleExceptionInternal(ex, body, headers, status, request)
    }
}