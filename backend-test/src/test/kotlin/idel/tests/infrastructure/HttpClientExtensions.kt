package idel.tests.infrastructure

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import java.net.http.HttpResponse
import java.util.*

val log = KotlinLogging.logger {"idel.tests.http-debug"}

fun asJSON(ri : HttpResponse.ResponseInfo): HttpResponse.BodySubscriber<JsonNode> {
    val upstream = HttpResponse.BodySubscribers.ofByteArray();
    return HttpResponse.BodySubscribers.mapping(upstream) {
        try {
            val jsonNode = jacksonObjectMapper().readValue(it) as JsonNode
            log.trace {"body = \n" + jsonNode.toPrettyString()}
            jsonNode
        } catch (ex : Exception) {
            val errorId = UUID.randomUUID().toString().replace("-","")
            log.warn {"$errorId can't extract json from response, ${ex.message}"}
            log.warn {"$errorId statusCode = ${ri.statusCode()} "}
            log.warn {"$errorId headers = ${ri.headers()!!.map()}"}

           throw ex;
        }
    }
}

fun ofJson(): HttpResponse.BodyHandler<JsonNode> = HttpResponse.BodyHandler {
        ri: HttpResponse.ResponseInfo -> asJSON(ri)
}
