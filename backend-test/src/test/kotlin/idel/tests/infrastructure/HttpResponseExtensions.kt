package idel.tests.infrastructure

import arrow.core.getOrElse
import com.fasterxml.jackson.databind.JsonNode
import idel.tests.infrastructure.JsonNodeExtensions.queryString

import io.kotest.assertions.asClue
import java.lang.IllegalArgumentException
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
 * Extract id  from response body
 */
fun HttpResponse<JsonNode>.extractId(entityName: String = ""): String {
    val path = if (entityName.isNotEmpty()) {
        "$entityName.id"
    } else {
        "id"
    }
    return extractData(this).queryString(path)
        .getOrElse {throw IllegalStateException("id is not found in entity, path = [$path]")}
}

fun HttpResponse<JsonNode>.extractField(field: String): String {
    return extractData(this).get(field)?.asText() ?: throw IllegalArgumentException("$field is not found in result")
}

fun HttpResponse<JsonNode>.hasDataPayload(): Boolean = this.body()?.hasNonNull("data") ?: false