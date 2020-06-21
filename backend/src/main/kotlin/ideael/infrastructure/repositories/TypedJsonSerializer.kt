package ideael.infrastructure.repositories

import com.couchbase.client.core.error.DecodingFailureException
import com.couchbase.client.core.logging.RedactableArgument
import com.couchbase.client.java.codec.JsonSerializer
import com.couchbase.client.java.codec.TypeRef
import com.couchbase.client.java.json.JsonObject
import com.couchbase.client.java.json.JsonValueModule
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import ideael.domain.Idea
import mu.KotlinLogging
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class UnexpectedTypeDecodingFailure : Exception {
    constructor(m: String, t: Throwable) : super(m, t)
    constructor(m: String) : super(m)
    constructor(t: Throwable) : super(t)
    constructor() : super()
}

/**
 * mapper is not modified
 */
class TypedJsonSerializer<T>(
    mapper: ObjectMapper,
    rootName: String,
    private val type: String,
    private val typedClass: Class<T>
) : JsonSerializer {
    private val mapperForUnwrappedJson: ObjectMapper
    private val mapperForWrappedJson: ObjectMapper

    private val log = KotlinLogging.logger {}


    init {
        // один маппер который разворачивает тип (первоначальная конвертаци)
        // и второй, который уже оперирует без типа (вторая конвертаци) и вызывать эти мапперы в convertTyped

        mapperForUnwrappedJson = mapper.copy()

        mapperForUnwrappedJson
            .registerModule(JsonValueModule())

        mapperForWrappedJson = mapperForUnwrappedJson.copy()
        mapperForWrappedJson
            .enable(DeserializationFeature.UNWRAP_ROOT_VALUE)

        val cfg = mapper.deserializationConfig.withRootName(rootName)
        mapperForWrappedJson.setConfig(cfg)


    }

    val typeField = "_type"


    override fun serialize(input: Any?): ByteArray {
        if (input is ByteArray) {
            return input
        }

        val json = mapperForUnwrappedJson.valueToTree<ObjectNode>(input)
        json.put("_type", type)
        val x = mapperForUnwrappedJson.writeValueAsBytes(json) // for debug
        return x;
    }

    override fun <T> deserialize(target: Class<T>, input: ByteArray): T {
        if (target.isAssignableFrom(ByteArray::class.java)) {
            return input as T
        }

        log.trace {"dezerialize: target=[$target], input=${String(input)}"}

        return try {
            if (typedClass.isAssignableFrom(target)) {
                convertTypedObj(input, target)
            } else {
                mapperForUnwrappedJson.readValue(input, target)
            }
        } catch (e: UnexpectedTypeDecodingFailure) {
            throw e
        } catch (t: Throwable) {
            throw DecodingFailureException(
                "Deserialization of content into target " + target
                        + " failed; encoded = " + RedactableArgument.redactUser(
                    String(input, StandardCharsets.UTF_8)
                ), t
            );
        }

    }


    private fun <T> convertTypedObj(input: ByteArray, target: Class<T>): T {
        /* If parse collection result object is wrapped, but it's unwrapped in case loading by Id.
        *  No way to detect in which case we are now.
        *  So it's ugly hack with catching parsing exception and trying another mapper.
        */
        val json =
            try {
                mapperForWrappedJson.readTree(input) as ObjectNode
            } catch (e: JsonProcessingException) {
                mapperForUnwrappedJson.readTree(input) as ObjectNode
            }

        if (!json.has(typeField)) {
            log.warn("Document is not typed ([_type] is null), input = [${String(input)}]")
            throw UnexpectedTypeDecodingFailure("Field [_type] is not exists in document")
        }

        val inputType = json[typeField].asText()
        if (type != inputType) {
            log.warn("Required type of document is [$type], but was [$inputType], input = [${String(input)}]}")
            throw UnexpectedTypeDecodingFailure("Required type of document is [$type], but was [$inputType]")
        } else {
            json.remove(typeField)
            return mapperForUnwrappedJson.treeToValue(json, target)
        }
    }

    override fun <T> deserialize(target: TypeRef<T>, input: ByteArray): T {
        TODO("Won't be implemented")
    }

}
