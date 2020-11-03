package idel.tests.infrastructure

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.net.http.HttpResponse

fun asJSON(): HttpResponse.BodySubscriber<JsonNode> {
    val upstream = HttpResponse.BodySubscribers.ofByteArray();
    return HttpResponse.BodySubscribers.mapping(upstream) {
        jacksonObjectMapper().readValue(it) as JsonNode
    }
}

fun ofJson() : HttpResponse.BodyHandler<JsonNode> {
    return HttpResponse.BodyHandler { asJSON() }
}
