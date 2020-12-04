package idel.tests.scenario

import arrow.core.Some
import idel.tests.apiobject.Couchbase
import idel.tests.apiobject.GroupsApi
import idel.tests.apiobject.JoinRequestsApi
import idel.tests.apiobject.User
import idel.tests.containsString
import idel.tests.infrastructure.JsonNodeExtensions.dataId
import idel.tests.isData
import idel.tests.isOk
import io.kotest.assertions.arrow.option.shouldNotBeNone
import io.kotest.assertions.asClue
import io.kotest.core.spec.style.DescribeSpec
import kotlin.time.ExperimentalTime

@ExperimentalTime
class JoinToGroupScenarios : DescribeSpec({
    val couchbase = Couchbase()
    beforeSpec {
        couchbase.clearAll()
    }

    context("empty installation without any groups and users") {
        val userAdmin = User("userAdmin")
        val userA = User("userA")
        val userB = User("userB")
        val userC = User("userC")
        val userD = User("userD")

        describe("register users") {
            listOf(userA, userB, userC, userD).forEach {user ->
                it("register user [${user.name}]") {
                    userAdmin.users.register(user.name).isOk()
                }
            }
        }

        describe("join to public group") {
            describe("[${userA.name}] adds new PUBLIC group without admins and members") {
                val createGroupResponse = userA.groups.create("public group", GroupsApi.PUBLIC)

                it("200 OK") {
                    createGroupResponse.isOk()
                }

                val oGroupId = createGroupResponse.body().dataId()
                it("has group id") {
                    oGroupId.shouldNotBeNone()
                }

                if (oGroupId is Some) {
                    val groupId = oGroupId.t

                    describe("[${userB.name}] see group is list of available") {

                        val availableResponse = userB.groups.loadAvailable()

                        it("response is 200 OK with data") {
                            availableResponse.isOk()
                            availableResponse.isData()
                        }

                        it("group is in the list") {
                            val body = availableResponse.body()
                            body.toPrettyString().asClue {
                                body.containsString("$.data[0].id", groupId)
                            }
                        }
                    }

                    describe("[${userB.name}] joins to group") {
                        val joinRequestResponse = userB.joinRequests.create(groupId)

                        it("response is 200 OK with data") {
                            joinRequestResponse.isOk()
                            joinRequestResponse.isData()
                        }

                        it("join request is approved") {
                            joinRequestResponse.isOk()
                            val body = joinRequestResponse.body()
                            body.toPrettyString().asClue {
                                body.containsString("$.data.status", JoinRequestsApi.APPROVED)
                            }
                        }
                    }

                    describe("[${userB.name}] adds an idea to group") {
                        val addIdeaResponse = userB.ideas.add(groupId)

                        it("response idea is 200 OK with data") {
                            addIdeaResponse.isOk()
                            addIdeaResponse.isData()
                        }

                        val oIdeaId = addIdeaResponse.body().dataId()

                        it("has idea id") {
                            oIdeaId.shouldNotBeNone()
                        }

                        if (oIdeaId is Some) {
                            // add assignee, votes and another cases
                        }
                    }
                }

            }
        }

    }
})