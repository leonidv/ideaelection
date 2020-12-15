package idel.tests.infrastructure

import com.fasterxml.jackson.databind.JsonNode
import idel.tests.*
import io.kotest.assertions.asClue
import io.kotest.core.spec.style.scopes.DescribeScope
import java.net.HttpURLConnection
import java.net.http.HttpResponse

class BodyFieldCheck(val testName: String, val jsonPath: String, val expectedValue: String)

suspend fun DescribeScope.checkIsOk(response: HttpResponse<JsonNode>, vararg fieldChecks: BodyFieldCheck) {
    it("response is 200 OK with data") {
        response.shouldBeOk()
        response.shouldBeData()
    }

    val body = response.body()

    body.toPrettyString().asClue {
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