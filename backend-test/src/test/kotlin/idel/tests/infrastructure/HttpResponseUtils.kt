package idel.tests.infrastructure

import com.fasterxml.jackson.databind.JsonNode

import io.kotest.assertions.asClue
import java.net.http.HttpResponse

fun extractData(response: HttpResponse<JsonNode>): JsonNode {

    val body = response.body();

    body.toPrettyString().asClue {
        "basic checks failed, tests are skipped".asClue {
            response.shouldHasStatus(200)
            response.shouldBeData()
        }
    }
    return response.body().get("data")
}

/**
 * Extract id ($.data.id) from response body
 */
fun HttpResponse<JsonNode>.extractId(): String {
    return extractData(this).get("id")?.asText() ?: throw IllegalStateException("id is not found in result")
}

fun HttpResponse<JsonNode>.hasDataPayload() : Boolean = this.body()?.hasNonNull("data") ?: false