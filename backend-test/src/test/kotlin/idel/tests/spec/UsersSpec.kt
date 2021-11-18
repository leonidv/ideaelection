package idel.tests.spec

import idel.tests.apiobject.*
import idel.tests.infrastructure.checkIsOk
import idel.tests.infrastructure.registryUsers
import idel.tests.infrastructure.shouldBeData
import idel.tests.infrastructure.shouldBeOk
import io.kotest.core.spec.style.DescribeSpec
import java.util.*

class UsersSpec : DescribeSpec({
    val entityStorage = EntityStorage()

    val userA = User("userA")

    beforeSpec {
        entityStorage.clearAll()
    }

    describe("settings") {
        registryUsers(userA)

        describe("default settings") {
            val response = userA.users.loadSettings()

            checkIsOk(
                response,
                hasNotificationFrequency(UserApi.NotificationFrequency.INSTANTLY),
                hasSubscribedToNews(false)
            )
        }

        describe("update") {
            describe("change all settings")

            val nextDisplayName = userA.name + " ${UUID.randomUUID()}"
            val nextPlan = UserApi.PLAN_FREE
            val nextFrequency = UserApi.NotificationFrequency.WEEKLY
            val nextSubscribed = true
            describe("settings is changed") {
                val response = userA.users.updateSettings(
                    displayName = nextDisplayName,
                    subscriptionPlan = nextPlan,
                    notificationFrequency = nextFrequency,
                    subscribedToNews = nextSubscribed
                )

                checkIsOk(
                    response,
                    hasMustReissueJwt(true),
                    hasNotificationFrequency(nextFrequency),
                    hasSubscribedToNews(nextSubscribed)
                )
            }

            describe("jwt contains new values") {
                val response = userA.jwt.issue().shouldBeOk()
                val jwt = JwtApi.extractPayload(response)
                jwtHasDisplayName(nextDisplayName).check(jwt)
                jwtHasSubscriptionPlan(nextPlan).check(jwt)
            }
        }
    }
})