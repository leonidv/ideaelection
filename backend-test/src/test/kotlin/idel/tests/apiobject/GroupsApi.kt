package idel.tests.apiobject

import com.fasterxml.jackson.databind.JsonNode
import idel.tests.Idel
import mu.KotlinLogging
import java.net.http.HttpResponse


class GroupsApi(username: String, idelUrl: String = Idel.URL) : AbstractObjectApi(username, idelUrl, "groups") {
    private val log = KotlinLogging.logger {}

    companion object {
        const val PUBLIC = "PUBLIC"
        const val CLOSED = "CLOSED"
        const val PRIVATE = "PRIVATE"
    }


    fun create(name: String,
               entryMode: String,
               description: String = "$name, $entryMode",
               admins: Set<String> = setOf("$username@httpbasic")): HttpResponse<JsonNode> {
        val body = """
            {
                "name": "$name",
                "description": "$description",
                "logo": "data:image/png;base64,dGVzdA==",
                "entryMode" : "$entryMode",
                "administrators": ${asJson(admins)}
            }
        """.trimIndent()
        
        return post("",body)
    }

    /**
     * Return available groups
     */
    fun loadAvailable() = get("?onlyAvailable")
}