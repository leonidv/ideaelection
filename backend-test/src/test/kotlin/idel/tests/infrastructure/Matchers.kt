package idel.tests.infrastructure

import arrow.core.None
import arrow.core.Some
import arrow.core.getOrElse
import com.fasterxml.jackson.databind.JsonNode
import idel.tests.infrastructure.JsonNodeExtensions.hasArrayElement
import idel.tests.infrastructure.JsonNodeExtensions.hasArrayObjectsOrder
import idel.tests.infrastructure.JsonNodeExtensions.hasObjectWithFields
import idel.tests.infrastructure.JsonNodeExtensions.hasPath
import idel.tests.infrastructure.JsonNodeExtensions.queryInt
import idel.tests.infrastructure.JsonNodeExtensions.queryString
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
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
                passed = response.hasDataPayload(),
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

    fun hasArrayElement(arrayPath: String, elementValue: String) = object : Matcher<JsonNode> {
        override fun test(value: JsonNode): MatcherResult {
            val hasElement = value.hasArrayElement(arrayPath, elementValue)
            val msg = """contains element of array at path $arrayPath with value [$elementValue]"""
            return MatcherResult(
                passed = hasElement,
                failureMessage = "Json should $msg",
                negatedFailureMessage = "Json should not $msg"

            )
        }
    }

    fun hasObjectWithFields(objectPath: String, vararg fields: Pair<String, String>) = object : Matcher<JsonNode> {
        override fun test(value: JsonNode): MatcherResult {
            val hasObject = value.hasObjectWithFields(objectPath, *fields)
            val msg = """contains object of array at path $objectPath with fields ${fields.joinToString()}"""
            return MatcherResult(
                passed = hasObject,
                failureMessage = "Json should $msg",
                negatedFailureMessage = "Json should not $msg"
            )
        }
    }

    fun hasArrayObjectsOrder(arrayPath: String, field: String, values: Array<String>) = object : Matcher<JsonNode> {
        override fun test(value: JsonNode): MatcherResult {
            val incorrectIndex = value.hasArrayObjectsOrder(arrayPath, field, values)
            val failureMsg = if (incorrectIndex == -1) {
                ""
            } else {
                "order is incorrect, first incorrect index is $incorrectIndex," +
                        " incorrect value = [${values[incorrectIndex]}], expected order = [$values]"
            }

            return MatcherResult(
                passed = incorrectIndex == -1,
                failureMessage = failureMsg,
                negatedFailureMessage = "order is same as given, but should not"
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
fun JsonNode.shouldContainsArrayElement(arrayPath: String, elementValue: String) =
    this should ResponseMatchers.hasArrayElement(arrayPath, elementValue)

fun JsonNode.shouldNotContainsArrayElement(arrayPath: String, elementValue: String) =
    this shouldNot ResponseMatchers.hasArrayElement(arrayPath, elementValue)

fun JsonNode.shouldContainsObject(arrayPath: String, vararg fields: Pair<String, String>) =
    this should ResponseMatchers.hasObjectWithFields(arrayPath, *fields)

fun JsonNode.shouldNotContainsObject(arrayPath: String, vararg fields: Pair<String, String>) =
    this shouldNot ResponseMatchers.hasObjectWithFields(arrayPath, *fields)


fun JsonNode.shouldHasArrayObjectsOrder(arrayPath: String, field : String, values: Array<String>) =
    this should ResponseMatchers.hasArrayObjectsOrder(arrayPath, field, values)