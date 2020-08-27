package idel

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = ["idel.infrastructure","idel.api"])
class Application

fun main() {
    SpringApplication.run(Application::class.java)
}