package idel.tests.scenario

import arrow.core.Some
import idel.tests.*
import idel.tests.apiobject.*
import idel.tests.infrastructure.JsonNodeExtensions.dataId
import idel.tests.infrastructure.checkIsForbidden
import idel.tests.infrastructure.checkIsOk
import io.kotest.assertions.arrow.option.shouldNotBeNone
import io.kotest.core.spec.style.DescribeSpec
import java.net.HttpURLConnection
import kotlin.time.ExperimentalTime

@ExperimentalTime
class JoinToPublicGroupScenario : DescribeSpec({
    val couchbase = Couchbase()
    beforeSpec {
        couchbase.clearAll()
    }

    context("empty installation without any groups and users") {
        val userAdmin = User("userAdmin", "(super admin)")
        val userA = User("userA")
        val userB = User("userB")
        val userC = User("userC")
        val userD = User("userD", "not member")

        describe("register users") {
            listOf(userA, userB, userC, userD).forEach {user ->
                it("register user [${user.name}]") {
                    userAdmin.users.register(user.name).shouldBeOk()
                }
            }
        }


        describe("$userA adds new PUBLIC group without admins and members") {
            val createGroupResponse = userA.groups.create("public group", GroupsApi.PUBLIC)

            checkIsOk(createGroupResponse)

            userA.role = "group's admin"

            val oGroupId = createGroupResponse.body().dataId()
            it("has group id") {
                oGroupId.shouldNotBeNone()
            }

            if (oGroupId is Some) {
                val groupId = oGroupId.t

                describe("$userB sees group in the list of available") {

                    val availableResponse = userB.groups.loadAvailable()

                    checkIsOk(
                        availableResponse,
                        includeGroup(groupId)
                    )
                }

                describe("$userB can't add an idea to the group") {
                    val ideaResponse = userB.ideas.add(groupId)

                    it("can't do it 403 and error [OperationNotPermitted]") {
                        ideaResponse.shouldHasStatus(HttpURLConnection.HTTP_FORBIDDEN)
                        ideaResponse.shouldBeError(103)
                    }
                }

                describe("$userB joins to the group") {
                    val joinRequestResponse = userB.joinRequests.create(groupId)

                    checkIsOk(
                        joinRequestResponse,
                        joinRequestIsApproved)
                }

                describe("$userC joins to the group") {
                    val joinRequestResponse = userC.joinRequests.create(groupId)

                    checkIsOk(
                        joinRequestResponse,
                        joinRequestIsApproved
                    )

                }

                describe("$userB adds an idea to the group") {
                    val addIdeaResponse = userB.ideas.add(groupId)

                    checkIsOk(addIdeaResponse)

                    val oIdeaId = addIdeaResponse.body().dataId()

                    it("has an idea id") {
                        oIdeaId.shouldNotBeNone()
                    }

                    if (oIdeaId is Some) {
                        val ideaId = oIdeaId.t

                        context("load idea") {
                            describe("$userB (author) can load idea") {
                                checkIsOk(userB.ideas.load(ideaId))
                            }

                            describe("$userC (member) can load idea") {
                                checkIsOk(userC.ideas.load(ideaId))
                            }

                            describe("$userA (admin) can load idea") {
                                checkIsOk(userA.ideas.load(ideaId))
                            }

                            describe("$userD  can't load idea") {
                                checkIsForbidden(userD.ideas.load(ideaId))
                            }
                        }
                    }
                }

            }
        }

    }
})

