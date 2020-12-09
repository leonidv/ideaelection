package idel.tests.infrastructure

import com.fasterxml.jackson.databind.JsonNode
import idel.tests.shouldBeData
import idel.tests.shouldHasStatus
import io.kotest.assertions.asClue
import java.net.http.HttpResponse

suspend fun extractData(response: HttpResponse<JsonNode>): JsonNode {

    "basic checks failed, tests are skipped".asClue {
        response.shouldHasStatus(200)
        response.shouldBeData()
    }

    return response.body().get("data")
}

/**
 * Extract id ($.data.id) from response body
 */
suspend fun extractId(response: HttpResponse<JsonNode>): String {
    return extractData(response).get("id")?.asText() ?: throw IllegalStateException("id is not found in result")
}