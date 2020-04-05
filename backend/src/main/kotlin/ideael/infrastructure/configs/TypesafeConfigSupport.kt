package ideael.infrastructure.configs

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.PropertySource
import org.springframework.core.io.support.EncodedResource
import org.springframework.core.io.support.PropertySourceFactory

class TypesafeConfigPropertySource(name: String, source: Config) : PropertySource<Config>(name, source) {

    override fun getProperty(name: String): Any? {
        if (name.contains('[') || name.contains(':'))
            return null;

        return if (source.hasPath(name)) {
            source.getAnyRef(name)
        } else
            null
    }

}

class TypesafeConfigPropertySourceFactory : PropertySourceFactory {
    override fun createPropertySource(name: String?, resource: EncodedResource): PropertySource<*> {
        val config = ConfigFactory.load(resource.resource.filename).resolve()
        val safeName = name ?: "typesafe"
        return TypesafeConfigPropertySource(safeName, config)
    }
}


@Configuration
@org.springframework.context.annotation.PropertySource(
    factory = TypesafeConfigPropertySourceFactory::class,
    value = ["application.conf"]
)
class TypesafePropertyLoader