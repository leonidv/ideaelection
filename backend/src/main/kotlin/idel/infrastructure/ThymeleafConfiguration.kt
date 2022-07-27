package idel.infrastructure

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.thymeleaf.TemplateEngine
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

@Configuration
class ThymeleafConfiguration {
    @Bean
    fun thymeleafEngine() : TemplateEngine {
        val resolver = ClassLoaderTemplateResolver()
        resolver.prefix = "templates/"
        resolver.checkExistence = true

        val engine = TemplateEngine()
        engine.templateResolvers = setOf(resolver)

        return engine;
    }
}