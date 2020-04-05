package ideael.tests

import assertk.assertThat
import assertk.assertions.*
import com.typesafe.config.ConfigFactory
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import io.restassured.path.json.JsonPath
import io.restassured.response.Response
import io.restassured.specification.RequestSpecification
import org.apache.http.HttpStatus
import org.spekframework.spek2.style.specification.Suite

val ideaelUrl: String = ConfigFactory.load().getString("ideael.url")

/**
 * Initialize RestAssured request with commons settings, like basic auth and conten type.
 */
fun initRequest(
    request: RequestSpecification,
    user: String,
    contentType: ContentType = ContentType.JSON
): RequestSpecification {
    request.auth().preemptive().basic(user, user)
    request.contentType(contentType)
    return request
}

/**
 * Send a request for adding idea to server.
 */
fun addIdea(user: String, title: String = "t", description: String = "d", link: String = "l"): String {
    val r = Given {
        initRequest(this, user)
        body("""{ "title": "$title", "description": "$description", "link": "$link" }""")
    } When {
        post("$ideaelUrl/ideas")
    } Then {
        statusCode(201)
        contentType("application/json")
    }
    val id = r.extract().path<String>("data.id")
    println("added idea id = [$id], user = [$user] ")
    return id
}

fun loadIdea(user : String, ideaId : String) : HashMap<String, Any> {
    val r = Given {
        initRequest(this,user)
    } When {
        get("$ideaelUrl/ideas/$ideaId")
    }

    r Then {
        statusCode(200)
        contentType("application/json")
    }

    return r.body().jsonPath()["data"]
}

fun changeAssignee(user : String, ideaId: String, newAssignee : String) {
    val r = Given {
        initRequest(this,user)
    } When {
        post("$ideaelUrl/ideas/$ideaId/assignee/$newAssignee")
    }

    r Then {
        statusCode(200)
        contentType("application/json")
    }

    return r.body().jsonPath()["data"]
}

fun markImplemented(user : String, ideaId : String, status : Boolean, expectedHttpCode : Int = HttpStatus.SC_OK) : Response {
    val url = "$ideaelUrl/ideas/$ideaId/implemented"
    val r = Given {
        initRequest(this,user)
    } When {
        if (status) {
            post(url)
        } else {
            delete(url)
        }
    }

    r Then {
        statusCode(expectedHttpCode)
        contentType("application/json")
    }

    return r
}

fun vote(user : String, ideaId : String) : Response {
    println("$user votes for $ideaId")
    val r = Given {
        initRequest(this,user)
    } When {
        post("$ideaelUrl/ideas/${ideaId}/voters")
    }

    r Then {
        statusCode(HttpStatus.SC_OK)
        contentType("application/json")
    }

    return r
}

fun checkResponse(suite: Suite, r: Response, httpStatus: Int) {
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

fun checkError(suite: Suite, r: Response, errorCode: Int, httpStatus: Int = 400) {
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

fun checkEntityData(suite: Suite, r: Response, httpStatus: Int = 200): HashMap<String, Any> {
    val json = checkData(suite, r, httpStatus)

    val data: Any = json["data"]

    suite.it("data is entity") {
        assertThat(data).isInstanceOf(HashMap::class.java)
    }

    return json["data"]
}

fun checkListData(suite: Suite, r: Response, httpStatus: Int = 200): List<HashMap<String, String>> {
    val json = checkData(suite, r, httpStatus)
    val data: Any = json.getList<HashMap<String, String>>("data")
    suite.it("data is list of entities") {
        assertThat(data).isInstanceOf(List::class.java)
    }
    return json.getList("data")
}

private fun checkData(suite: Suite, r: Response, httpStatus: Int): JsonPath {
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