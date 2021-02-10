package idel.tests.infrastructure

import arrow.core.Some
import com.fasterxml.jackson.databind.JsonNode
import idel.tests.*
import idel.tests.apiobject.GroupsApi
import idel.tests.apiobject.User
import idel.tests.apiobject.checkJoinRequestIsApproved
import idel.tests.infrastructure.JsonNodeExtensions.dataId
import idel.tests.infrastructure.JsonNodeExtensions.queryArraySize
import idel.tests.infrastructure.JsonNodeExtensions.queryString
import io.kotest.assertions.arrow.option.shouldBeSome
import io.kotest.assertions.asClue
import io.kotest.core.spec.style.scopes.DescribeScope
import java.net.HttpURLConnection
import java.net.http.HttpResponse


suspend fun DescribeScope.registryUsers(vararg users: User) {
    describe("register users") {
        users.forEach {user ->
            it("register user [${user.name}]") {
                User.instanceAdmin.users.register(user.name).shouldBeOk()
            }
        }
    }
}

/**
 * Create basic gr
 */
suspend fun DescribeScope.initGroup(groupAdmin: User, members: Set<User>): String {

    lateinit var groupId: String

    describe("$groupAdmin creates the public group") {
        val response = groupAdmin.groups.create("assignee spec group", GroupsApi.PUBLIC)

        checkIsOk(response)

        groupId = (response.body().dataId() as Some).t
    }

    members.forEach {user ->
        describe("$user join to group") {
            checkIsOk(user.joinRequests.create(groupId), checkJoinRequestIsApproved())
        }
    }

    return groupId
}

data class BodyFieldCheck(val testName: String, val jsonPath: String, val expectedValue: String)

suspend fun DescribeScope.checkIsOk(response: HttpResponse<JsonNode>, vararg fieldChecks: BodyFieldCheck) {
    val body = response.body()

    body.toPrettyString().asClue {
        it("response is 200 OK with data") {
            response.shouldBeOk()
            response.shouldBeData()
        }

        fieldChecks.forEach {
            it(it.testName) {
                body.shouldContains(it.jsonPath, it.expectedValue)
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