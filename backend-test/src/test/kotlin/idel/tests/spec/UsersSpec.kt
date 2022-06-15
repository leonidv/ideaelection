package idel.tests.spec

import idel.tests.apiobject.*
import idel.tests.infrastructure.checkIsOk
import idel.tests.infrastructure.registerUsers
import idel.tests.infrastructure.shouldBeOk
import io.kotest.core.spec.style.DescribeSpec
import java.util.*

class UsersSpec : DescribeSpec({
    val entityStorage = EntityStorage()

    val userA = User("userA")
    val userB = User("userB")

    beforeSpec {
        entityStorage.clearAll()
    }

    describe("settings") {
        registerUsers(userA)

        describe("default settings") {
            val response = userA.users.loadSettings()
            with(UsersResponseChecks) {
                checkIsOk(
                    response,
                    hasNotificationFrequency(UserApi.NotificationFrequency.INSTANTLY),
                    hasSubscribedToNews(false)
                )
            }
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

                with(UsersResponseChecks) {
                    checkIsOk(
                        response,
                        hasMustReissueJwt(true),
                        hasNotificationFrequency(nextFrequency),
                        hasSubscribedToNews(nextSubscribed)
                    )
                }
            }

            describe("jwt contains new values") {
                val response = userA.jwt.issue().shouldBeOk()
                val jwt = JwtApi.extractPayload(response)
                JwtChecks.hasDisplayName(nextDisplayName).check(jwt)
                JwtChecks.hasSubscriptionPlan(nextPlan).check(jwt)
            }
        }

        describe("load by id") {
            describe("register $userB") {
                it("OK") {
                    User.instanceAdmin.users.register(
                        userB,
                        displayName = userB.name,
                        email = userB.email,
                        plan = UserApi.PLAN_ENTERPRISE
                    ).shouldBeOk()
                }
            }

            describe("$userA load $userB by id") {
                val response = userA.users.load(userB.id)

                with(UsersResponseChecks) {
                    checkIsOk(
                        response,
                        hasId(userB.id),
                        hasEmail(userB.email),
                        hasDisplayName(userB.name),
                        hasDomain(userB.domain),
                        hasSubscriptionPlan(UserApi.PLAN_ENTERPRISE)
                    )
                }
            }
        }

        describe("list") {
            var users = ('C'..'Z').map {User("user$it")}
            users.forEach {User.instanceAdmin.users.register(it, displayName = it.name)}
            users = listOf(userA, userB) + users

            describe("without params") {
                val response = userA.users.list()
                checkIsOk(
                    response,
                    *UsersListChecks.hasUsersInOrder(users.subList(0, 10))
                )
            }

            describe("pagination skip=3, count = 2") {
                val response = userA.users.list(skip = 3, count = 2)
                checkIsOk(response, *UsersListChecks.hasUsersInOrder(users.subList(3, 5)))
            }

            listOf("userB","serB","B@m").forEach {filter->
                describe("filter by [$filter]") {
                    val response = userA.users.list(filter = filter)
                    checkIsOk(response,
                        *UsersListChecks.hasUsersInOrder(listOf(userB))
                    )
                }
            }
        }

    }
})