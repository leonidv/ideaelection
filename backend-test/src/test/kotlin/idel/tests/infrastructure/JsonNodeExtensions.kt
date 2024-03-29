package idel.tests.infrastructure

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.getOrElse
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jayway.jsonpath.*
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import com.jayway.jsonpath.spi.mapper.MappingException

fun idField(id: String) = Pair("id", id)

class ValueNotExists(msg: String) : Exception(msg) {
    companion object {
        fun `throw`(field: String): Nothing = throw ValueNotExists("field = [$field]")
        fun throwForQuery(query: String): Nothing = throw ValueNotExists("query = [$query]")
    }
}

object JsonNodeExtensions {
    private val provider = JacksonJsonNodeJsonProvider(jacksonObjectMapper())
    private val mappingProvider = JacksonMappingProvider(jacksonObjectMapper())
    private val conf = Configuration.builder()
        .jsonProvider(provider)
        .mappingProvider(mappingProvider)
        .build()

    private val listOfAny = object : TypeRef<List<Any>>() {}
    private val listStringTypeRef = object : TypeRef<List<String>>() {}
    private val setStringTypeRef = object : TypeRef<Set<String>>() {}
    private val stringTypeRef = object : TypeRef<String>() {}
    private val intTypeRef = object : TypeRef<Int>() {}

    fun JsonNode.hasPath(jsonPath: String): Boolean {
        return this
            .queryString(jsonPath)
            .map {it.isNotBlank()}
            .getOrElse {false}
    }

    fun JsonNode.hasArrayElement(arrayPath: String, elementValue: String): Boolean {
        val jsonPath = """$arrayPath[?(@ == "$elementValue")]"""
        return try {
            val parsed = JsonPath.parse(this, conf)
            val data = parsed.read(jsonPath, listOfAny) as List<Any>
            data.isNotEmpty()
        } catch (e: PathNotFoundException) {
            false
        }

    }


    fun JsonNode.hasObjectWithFields(objectPath: String, vararg fields: Pair<String, String?>): Boolean {
        val jsonFilter =
            fields.joinToString(prefix = "?(", separator = " && ", postfix = ")") {field ->
                val name = field.first
                val value = if (field.second != null) "'${field.second}'" else "null"
                "@.$name == $value"
            }
        val jsonPath = """$objectPath[$jsonFilter]"""
        return try {
            val parsed = JsonPath.parse(this, conf)
            val data = parsed.read(jsonPath, listOfAny) as List<Any>
            data.isNotEmpty()
        } catch (e: InvalidPathException) {
            println("${e.message} for jsonPath=[${jsonPath}]")
            throw e
        } catch (e: PathNotFoundException) {
            false
        }
    }

    /**
     * Check that array contains set of object.
     * @return if all values are contained in the array return emptySet(). Otherwise, return a set with values that
     * are not found in the array.
     */
    fun JsonNode.containsObjects(arrayPath: String, field: String, values: Set<String>): Set<String> {
        val result = mutableSetOf<String>()
        values.filterNot {value ->
            this.hasObjectWithFields(arrayPath, Pair(field, value))
        }
        return result
    }

    /**
     * Check that array contains element at positions as they are given.
     * Return -1 if order is OK or index of first broken element.
     */
    fun JsonNode.hasArrayObjectsOrder(arrayPath: String, field: String, values: Array<String>): Int {
        var index = -1
        var objectExists: Boolean
        do {
            index += 1
            val elementPath = "$arrayPath[$index]"
            objectExists = this.hasObjectWithFields(elementPath, Pair(field, values[index]))
        } while (objectExists && index < values.size - 1)

        return if (objectExists) {
            -1
        } else {
            index
        }

    }

    fun JsonNode.queryString(jsonPath: String): Option<String> {
        return try {
            val parsed = JsonPath.parse(this, conf)
            val x = parsed.read<Any>(jsonPath)
            when (x) {
                is TextNode -> Option.fromNullable(x.textValue())
                is IntNode -> Option.fromNullable(x.intValue().toString()) // ugly dirty hack, only for test :)
                is BooleanNode -> Option.fromNullable(x.booleanValue().toString())
                is ArrayNode -> if (x.size() == 1) {
                    Option.fromNullable(x[0].textValue())
                } else {
                    throw IllegalArgumentException("query result is array with size != 1, jsonPath = $jsonPath")
                }
                else -> {
                    throw IllegalArgumentException("jsonPath doesn't contains string or array with 1 element, jsonPath = $jsonPath")
                }
            }
        } catch (e: MappingException) {
            None
        } catch (e: PathNotFoundException) {
            None
        } catch (e: InvalidPathException) {
            throw IllegalArgumentException("invalid path [$jsonPath], syntax error = ${e.message}")
        }

    }

    fun JsonNode.queryList(jsonPath: String): Option<List<String>> {
        return try {
            val parsed = JsonPath.parse(this, conf)
            val list = parsed.read(jsonPath, listStringTypeRef)
            Option.fromNullable(list)
        } catch (e: MappingException) {
            None
        } catch (e: PathNotFoundException) {
            None
        }
    }

    fun JsonNode.queryArraySize(jsonPath: String): Option<Int> {
        return try {
            val parsed = JsonPath.parse(this, conf)
            val size = parsed.read<Int>("${jsonPath}.length()")
            Some(size)
        } catch (e: PathNotFoundException) {
            None
        }
    }

    fun JsonNode.querySet(jsonPath: String): Option<Set<String>> {
        return try {
            val parsed = JsonPath.parse(this, conf)
            val list = parsed.read(jsonPath, setStringTypeRef)
            Option.fromNullable(list)
        } catch (e: PathNotFoundException) {
            None
        }

    }

    fun JsonNode.queryInt(jsonPath: String): Option<Int> {
        return try {
            queryString(jsonPath).map {it.toInt()}
        } catch (e: NumberFormatException) {
            None
        }
    }

}


