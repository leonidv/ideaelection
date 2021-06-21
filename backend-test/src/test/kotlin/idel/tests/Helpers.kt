package idel.tests

import assertk.assertThat
import assertk.assertions.*
import com.typesafe.config.ConfigFactory
import io.kotest.core.spec.style.scopes.DescribeScope
import io.restassured.filter.Filter
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import io.restassured.path.json.JsonPath
import io.restassured.response.Response
import io.restassured.specification.RequestSpecification
import org.apache.http.HttpStatus


val idelUrl = Idel.URL


/**
 * Initialize RestAssured request with commons settings, like basic auth and conten type.
 */
fun initRequest(
        request: RequestSpecification,
        user: String,
        contentType: ContentType = ContentType.JSON,
        debug : Boolean = false
): RequestSpecification {
    request.auth().preemptive().basic(user, user)
    request.contentType(contentType)
    if (debug) {
        request.filters(listOf(ResponseLoggingFilter(), RequestLoggingFilter()))
    }
    return request
}

suspend fun checkResponse(suite: DescribeScope, r: Response, httpStatus: Int) {
    suite.it("http status is $httpStatus") {
        assertThat(r.statusCode()).isEqualTo(httpStatus)
    }

    suite.it("content type is application/json") {
        assertThat(r.contentType).isEqualTo("application/json")
    }

    suite.it("http body is present") {
        assertThat(r.body().asString()).isNotNull()
        assertThat(r.body().asString()).isNotEmpty()
    }

}

suspend fun checkError(suite: DescribeScope, r: Response, errorCode: Int, httpStatus: Int = 400) {
    checkResponse(suite, r, httpStatus)

    val json = r.body.jsonPath()
    suite.it("data is null") {
        assertThat(json.getString("data")).isNull()
    }

    suite.it("error.code is $errorCode") {
        assertThat(json.getInt("error.code")).isEqualTo(errorCode)
    }

    suite.it("error.message is present") {
        assertThat(json.getString("error.message")).isNotNull()
        assertThat(json.getString("error.message")).isNotEmpty()
    }
}

suspend fun checkEntityData(suite: DescribeScope, r: Response, httpStatus: Int = 200): HashMap<String, Any> {
    val json = checkData(suite, r, httpStatus)

    val data: Any = json["data"]

    suite.it("data is entity") {
        assertThat(data).isInstanceOf(HashMap::class.java)
    }

    return json["data"]
}

suspend fun checkListData(suite: DescribeScope, r: Response, httpStatus: Int = 200): List<HashMap<String, String>> {
    val json = checkData(suite, r, httpStatus)
    val data: Any = json.getList<HashMap<String, String>>("data")
    suite.it("data is list of entities") {
        assertThat(data).isInstanceOf(List::class.java)
    }
    return json.getList("data")
}

private suspend fun checkData(suite: DescribeScope, r: Response, httpStatus: Int): JsonPath {
    checkResponse(suite, r, httpStatus)
    val json = r.body.jsonPath()
    suite.it("error is null") {
        assertThat(json.getString("error")).isNull()
    }

    suite.it("data is present") {
        assertThat(json.getString("data")).isNotNull()
        assertThat(json.getString("data")).isNotEmpty()
    }
    return json
}