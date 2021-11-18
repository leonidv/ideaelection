package idel.tests.scenario

import arrow.core.getOrElse
import com.fasterxml.jackson.databind.JsonNode
import idel.tests.apiobject.*
import idel.tests.infrastructure.JsonNodeExtensions.queryString
import idel.tests.infrastructure.ValueNotExists
import idel.tests.infrastructure.checkIsOk
import idel.tests.infrastructure.registryUsers
import idel.tests.infrastructure.shouldBeOk
import io.kotest.core.spec.style.DescribeSpec
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.time.Duration
import java.util.*

class JwtAuthentication : DescribeSpec({
    val entityStorage = EntityStorage()
    beforeSpec {
        entityStorage.clearAll()
    }

    val userA = User("userA")

    describe("initialization") {
        User.instanceAdmin.users.register(userA, displayName = userA.name, email = userA.email).shouldBeOk()
    }

    describe("user can issue jwt") {
        val response = userA.jwt.issue().shouldBeOk()
        val jwt = JwtApi.extractPayload(response)

        describe("jwt includes user's id as sub") {
            jwtHasUserIdAsSub(userA.id).check(jwt)
        }

        describe("jwt includes user's display name") {
            jwtHasDisplayName(userA.name).check(jwt)
        }

        describe("jwt includes user's subscription plan") {
            jwtHasSubscriptionPlan(UserApi.PLAN_FREE).check(jwt)
        }

        describe("jwt includes user's email") {
            jwtHasEmail(userA.email).check(jwt)
        }

        describe("jwt issuer is saedi") {
            jwtHasIssuerSaedi.check(jwt)
        }
    }

    describe("user can authenticates by jwt") {
        lateinit var jwt : String
        describe("user get jwt") {
            val response = userA.jwt.issue().shouldBeOk()
            jwt = response.body()!!.queryString("$.data").getOrElse {ValueNotExists.`throw`("data")}
        }

        describe("user make request with jwt") {
            val response = userA.users.jwtAuth(jwt)
            checkIsOk(response)
        }

    }

})