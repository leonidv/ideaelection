package idel.tests.infrastructure

import arrow.core.Option
import arrow.core.getOrElse
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException
import com.jayway.jsonpath.TypeRef
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import idel.tests.infrastructure.JsonNodeExtensions.queryInt
import java.lang.IllegalArgumentException
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

object JsonNodeExtensions {
    private val provider = JacksonJsonNodeJsonProvider(jacksonObjectMapper())
    private val mappingProvider = JacksonMappingProvider(jacksonObjectMapper())
    private val conf = Configuration.builder()
        .jsonProvider(provider)
        .mappingProvider(mappingProvider)
        .build()

    private val listStringTypeRef = object : TypeRef<List<String>>() {}
    private val setStringTypeRef = object : TypeRef<Set<String>>() {}
    private val stringTypeRef = object : TypeRef<String>(){}
    private val intTypeRef = object : TypeRef<Int>(){}

    fun JsonNode.hasPath(jsonPath: String): Boolean {
        return this
            .queryString(jsonPath)
            .map {!it.isNullOrBlank()}
            .getOrElse {false}
    }

    fun JsonNode.queryString(jsonPath: String): Option<String> {
        return try {
            val parsed = JsonPath.parse(this, conf)
            Option.fromNullable(parsed.read(jsonPath, stringTypeRef))
        } catch (e: PathNotFoundException) {
            Option.empty()
        }

    }

    fun JsonNode.queryList(jsonPath: String): Option<List<String>> {
        return try {
            val parsed = JsonPath.parse(this, conf)
            val list = parsed.read(jsonPath, listStringTypeRef)
            Option.fromNullable(list)
        } catch (e: PathNotFoundException) {
            Option.empty()
        }

    }

    fun JsonNode.querySet(jsonPath: String): Option<Set<String>> {
        return try {
            val parsed = JsonPath.parse(this, conf)
            val list = parsed.read(jsonPath, setStringTypeRef)
            Option.fromNullable(list)
        } catch (e: PathNotFoundException) {
            Option.empty()
        }

    }

    fun JsonNode.queryInt(jsonPath: String): Option<Int> {
        return try {
            queryString(jsonPath).map {it.toInt()}
        } catch (e: NumberFormatException) {
            Option.empty()
        }
    }

    /**
     * extract id of entity. Is always has same path in response.
     */
    fun JsonNode.dataId() : Option<String> {
        return this.queryString("$.data.id")
    }
}


