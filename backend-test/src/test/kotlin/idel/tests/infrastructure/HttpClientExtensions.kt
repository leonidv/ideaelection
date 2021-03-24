package idel.tests.infrastructure

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import java.net.http.HttpResponse

val log = KotlinLogging.logger {}

fun asJSON(): HttpResponse.BodySubscriber<JsonNode> {
    val upstream = HttpResponse.BodySubscribers.ofByteArray();
    return HttpResponse.BodySubscribers.mapping(upstream) {
        val jsonNode = jacksonObjectMapper().readValue(it) as JsonNode
        log.trace {"body = \n" + jsonNode.toPrettyString()}
        jsonNode
    }
}

fun ofJson(): HttpResponse.BodyHandler<JsonNode> {
    return HttpResponse.BodyHandler {asJSON()}
}
