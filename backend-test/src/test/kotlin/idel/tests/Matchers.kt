package idel.tests

import arrow.core.None
import arrow.core.Some
import arrow.core.getOrElse
import com.fasterxml.jackson.databind.JsonNode
import idel.tests.infrastructure.JsonNodeExtensions.hasArrayElement
import idel.tests.infrastructure.JsonNodeExtensions.hasArrayObjectWithFields
import idel.tests.infrastructure.JsonNodeExtensions.hasPath
import idel.tests.infrastructure.JsonNodeExtensions.queryInt
import idel.tests.infrastructure.JsonNodeExtensions.queryString
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import java.net.http.HttpResponse

object ResponseMatchers {
    fun <T> hasStatus(code: Int) = object : Matcher<HttpResponse<T>> {
        override fun test(value: HttpResponse<T>): MatcherResult =
            MatcherResult(
                value.statusCode() == code,
                "Status should be $code instead of ${value.statusCode()}",
                "Status should not be $code"
            )
    }

    fun hasDataPayload() = object : Matcher<HttpResponse<JsonNode>> {
        override fun test(response: HttpResponse<JsonNode>): MatcherResult =
            MatcherResult(
                passed = response.body()?.hasNonNull("data") ?: false,
                failureMessage = "Payload should be data",
                negatedFailureMessage = "Payload should be error"
            )

    }

    fun hasError(code: Int) = object : Matcher<HttpResponse<JsonNode>> {
        override fun test(response: HttpResponse<JsonNode>): MatcherResult {
            val oCode = response.body().queryInt("$.error.code")
            val codeIsCorrect = oCode.map {it == code}.getOrElse {false}

            return MatcherResult(
                passed = codeIsCorrect,
                failureMessage = "Should be error with code $code, actualCode = $oCode",
                negatedFailureMessage = "Should not be error with code $code, actualCode = $oCode"
            )
        }
    }

    fun hasStringValue(jsonPath: String, value: String) = object : Matcher<JsonNode> {
        override fun test(node: JsonNode): MatcherResult {
            val oValue = node.queryString(jsonPath)
            return when (oValue) {
                is Some -> MatcherResult(
                    passed = oValue.t == value,
                    failureMessage = "Should has [$value] at [$jsonPath], actual value = [${oValue.t}]",
                    negatedFailureMessage = "Should not has [$value] at [$jsonPath], actual value = [${oValue.t}]"
                )
                is None -> MatcherResult(
                    passed = false,
                    failureMessage = "Should has [$value] at [$jsonPath], but path is not found",
                    negatedFailureMessage = "Should has not [$value] at [$jsonPath], but path is not found"
                )
            }
        }
    }

    fun hasIntValue(jsonPath: String, value: Int) = object : Matcher<JsonNode> {
        override fun test(node: JsonNode): MatcherResult {
            val oValue = node.queryInt(jsonPath)
            val codeIsCorrect = oValue.map {it == value}.getOrElse {false}

            return MatcherResult(
                passed = codeIsCorrect,
                failureMessage = "Should has $value at $jsonPath, actual value = $oValue",
                negatedFailureMessage = "Should not has $value at $jsonPath, actual value = $oValue"
            )
        }

    }

    fun hasJsonPath(jsonPath: String) = object : Matcher<JsonNode> {
        override fun test(node: JsonNode): MatcherResult {
            return MatcherResult(
                passed = node.hasPath(jsonPath),
                failureMessage = "Json should contain a property with a path $jsonPath",
                negatedFailureMessage = "Json should not contains a property with a path $jsonPath"
            )

        }
    }

    fun hasArrayElement(arrayPath: String, elementKey: String, elementValue: String) = object : Matcher<JsonNode> {
        override fun test(value: JsonNode): MatcherResult {
            val hasElement = value.hasArrayElement(arrayPath, elementKey, elementValue)
            val msg = """contains element of array at path $arrayPath with field {"$elementKey" : "$elementValue"}"""
            return MatcherResult(
                passed = hasElement,
                failureMessage = "Json should $msg",
                negatedFailureMessage = "Json should not $msg"

            )
        }
    }

    fun hasArrayObjectWithFields(arrayPath: String, vararg fields : Pair<String,String>) = object : Matcher<JsonNode> {
        override fun test(value: JsonNode): MatcherResult {
            val hasObject = value.hasArrayObjectWithFields(arrayPath, *fields)
            val msg = """contains object of array at path $arrayPath with fields ${fields.joinToString()}"""
            return MatcherResult(
                passed = hasObject,
                failureMessage = "Json should $msg",
                negatedFailureMessage = "Json should not $msg"
            )
        }
    }
}

/**
 * Check code status.
 */
fun <T> HttpResponse<T>.shouldHasStatus(code: Int) = this should ResponseMatchers.hasStatus(code)

fun <T> HttpResponse<T>.shouldBeOk() = this should ResponseMatchers.hasStatus(200)

/**
 * Check that body contains data section.
 */
fun HttpResponse<JsonNode>.shouldBeData() = this should ResponseMatchers.hasDataPayload()

/**
 * Check that body contains error section with specific code.
 */
fun HttpResponse<JsonNode>.shouldBeError(code: Int) = this should ResponseMatchers.hasError(code)

/*
 * Check that json contains specific value.
 */
fun JsonNode.shouldHasPath(jsonPath: String) = this should ResponseMatchers.hasJsonPath(jsonPath)

/**
 * Check that json contains path.
 */
fun JsonNode.shouldContains(jsonPath: String, value: String) =
    this should ResponseMatchers.hasStringValue(jsonPath, value)

/**
 * Check that json contains int.
 */
fun JsonNode.shouldContains(jsonPath: String, value: Int) = this should ResponseMatchers.hasIntValue(jsonPath, value)

/**
 * Check that JSON contains element of array.
 */
fun JsonNode.shouldContainsArrayElement(arrayPath: String, elementKey: String, elementValue: String) =
    this should ResponseMatchers.hasArrayElement(arrayPath, elementKey, elementValue)

fun JsonNode.shouldContainsArrayObjectWithFields(arrayPath: String, vararg fields : Pair<String,String>) =
    this should ResponseMatchers.hasArrayObjectWithFields(arrayPath, *fields)



