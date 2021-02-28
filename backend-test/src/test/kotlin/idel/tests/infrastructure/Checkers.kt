package idel.tests.infrastructure

import com.fasterxml.jackson.databind.JsonNode
import idel.tests.*
import idel.tests.infrastructure.JsonNodeExtensions.queryArraySize
import io.kotest.assertions.arrow.option.shouldBeSome
import io.kotest.assertions.asClue
import io.kotest.core.spec.style.scopes.DescribeScope
import io.kotest.matchers.shouldNot
import java.net.HttpURLConnection
import java.net.http.HttpResponse

interface ResponseChecker {
    val testName: String

    fun check(jsonNode: JsonNode)
}

class NotResponseChecker(val responseChecker: ResponseChecker) : ResponseChecker {
    override val testName: String = "NOT" + responseChecker.testName

    override fun check(jsonNode: JsonNode) {
        TODO("Not yet implemented")
    }
}

class BodyFieldValueChecker(
    override val testName: String,
    private val jsonPath: String,
    private val expectedValue: String
) : ResponseChecker {
    companion object {
        fun forField(fieldName: String, expectedValue: String) =
            BodyFieldValueChecker("$fieldName is [$expectedValue]", "$.data.$fieldName", expectedValue)
    }

    override fun check(jsonNode: JsonNode) {
        jsonNode.shouldContains(jsonPath, expectedValue)
    }
}

class BodyArrayElementExists(
    override val testName: String,
    private val arrayPath: String,
    private val elementKey: String,
    private val elementValue: String, ) : ResponseChecker {

    override fun check(jsonNode: JsonNode) {
        jsonNode.shouldContainsArrayElement(arrayPath, elementKey, elementValue)
    }
}

class NotBodyArrayElementExists(
    override val testName: String,
    private val arrayPath: String,
    private val elementKey: String,
    private val elementValue: String, ) : ResponseChecker {

    override fun check(jsonNode: JsonNode) {
        jsonNode.shouldNotContainsArrayElement(arrayPath, elementKey, elementValue)
    }
}


class BodyArrayObjectWithFields(
    override val testName: String,
    private val arrayPath: String,
    private val fields : Array<Pair<String,String>>
) : ResponseChecker {
    override fun check(jsonNode: JsonNode) {
        jsonNode.shouldContainsArrayObjectWithFields(arrayPath, *fields)
    }
}

class BodyArraySize(
    override val testName: String,
    private val arrayPath: String,
    private val size : Int
 ) : ResponseChecker {

    override fun check(jsonNode: JsonNode) {
        jsonNode.queryArraySize(arrayPath).shouldBeSome(size)
    }
}

class BodyElementIsPresent(
    override val testName: String,
    private val elementPath : String
) : ResponseChecker {
    override fun check(jsonNode: JsonNode) {
        jsonNode.shouldHasPath(elementPath)
    }
}

val hasId = BodyElementIsPresent("id is present","$.data.id")

suspend fun DescribeScope.checkIsOk(response: HttpResponse<JsonNode>, vararg fieldChecks: ResponseChecker) {
    val body = response.body()

    body.toPrettyString().asClue {
        it("response is 200 OK with data") {
            response.shouldBeOk()
            response.shouldBeData()
        }

        fieldChecks.forEach {checker ->
            it(checker.testName) {
                //body.shouldContains(it.jsonPath, it.expectedValue)
                checker.check(body)
            }
        }
    }
}

suspend fun DescribeScope.checkIsForbidden(response: HttpResponse<JsonNode>) {
    it("response is 403 with code 103") {
        response.shouldHasStatus(HttpURLConnection.HTTP_FORBIDDEN)
        response.shouldBeError(103)
    }
}

class ValidationError(
    /**
     *Path to an incorrect field in the request. It's not a path of the response!
     *For example, it's should be a ".summary" to check a validation of the field "summary" from the request..
     */
    val field: String,
    val message: String
) {
    companion object {
        fun leastCharacters(field: String, minValue: Int) = ValidationError(
            field = field,
            message = "must have at least $minValue characters"
        )

        fun mostCharacters(field: String, maxValue: Int) = ValidationError(
            field = field,
            message = "must have at most $maxValue characters"
        )

        fun mustBeUrl(field: String) = ValidationError(
            field = field,
            message = "must be URL"
        )
    }
}

suspend fun DescribeScope.checkValidationErrors(
    response: HttpResponse<JsonNode>,
    vararg expectedErrors: ValidationError
) {
    it("response is 400 with code 106") {
        response.shouldHasStatus(HttpURLConnection.HTTP_BAD_REQUEST)
        response.shouldBeError(106)
    }

    val body = response.body()


    body.toPrettyString().asClue {
        val errorsPath = "$.error.validationErrors"

        it("has ${expectedErrors.size} validation errors") {
            body.queryArraySize(errorsPath).shouldBeSome(expectedErrors.size)
        }

        expectedErrors.forEach {expectedError ->
            it("field [${expectedError.field}] has validation error [${expectedError.message}] ") {
                body.shouldContainsArrayElement(errorsPath, "dataPath", expectedError.field)
                body.shouldContainsArrayElement(errorsPath, "message", expectedError.message)
            }
        }
    }
}