package idel.tests.spec

import arrow.core.Some
import idel.tests.apiobject.*
import idel.tests.infrastructure.JsonNodeExtensions.dataId
import idel.tests.infrastructure.checkIsForbidden
import idel.tests.infrastructure.checkIsOk
import idel.tests.shouldBeOk
import io.kotest.core.spec.style.DescribeSpec

class IdeaImplementedSpec : DescribeSpec({
    val couchbase = Couchbase()
    beforeSpec {
        couchbase.clearAll()
    }
    context("userA is admin, userB is author, userC is member, userD is not member, userE is assignee") {
        val userAdmin = User("userAdmin")
        val userA = User("userA")
        val userB = User("userB")
        val userC = User("userC")
        val userD = User("userD")
        val userE = User("userE")

        lateinit var groupId: String
        lateinit var ideaId: String

        describe("init") {
            describe("register users") {
                listOf(userA, userB, userC, userD, userE).forEach {user ->
                    it("register user [${user.name}]") {
                        userAdmin.users.register(user.name).shouldBeOk()
                    }
                }
            }

            describe("$userA creates the public group and $userB, $userC, $userE join to it") {
                val response = userA.groups.create("assignee spec group", GroupsApi.PUBLIC)

                checkIsOk(response)

                groupId = (response.body().dataId() as Some).t
            }

            listOf(userB, userC, userE).forEach {user ->
                describe("$user join to group") {
                    checkIsOk(user.joinRequests.create(groupId), checkJoinRequestIsApproved())
                }
            }

            describe("$userB create an idea, by default idea is not done") {
                val response = userB.ideas.add(groupId, summary = "idea for assignee spec")
                checkIsOk(response, ideaIsNotImplemented)
                ideaId = (response.body().dataId() as Some).t
            }
        }

        describe("$userE assigns idea him self") {
            checkIsOk(userE.ideas.assign(ideaId, userE))
        }


        listOf(userB, userC, userD).forEach {user ->
            describe("$user can't mark as done idea, which is assigned to another user") {
                checkIsForbidden(user.ideas.implemented(ideaId))
            }
        }

        describe("$userE (assignee) mark as done") {
            checkIsOk(
                    userE.ideas.implementedg(ideaId),
                    ideaIsImplemented)
        }

        listOf(userB, userC, userD).forEach {user ->
            describe("$user can't mark as not done") {
                checkIsForbidden(user.ideas.notImplemented(ideaId))
            }
        }

        describe("$userE (assignee) can mark as not done") {
            checkIsOk(
                    userE.ideas.notImplemented(ideaId),
                    ideaIsNotImplemented)
        }

        describe("$userA (group admin) can mark as done") {
            checkIsOk(
                    userA.ideas.implemented(ideaId),
                    ideaIsImplemented)
        }

        describe("$userA (group admin) can mark as not done") {
            checkIsOk(
                    userA.ideas.notImplemented(ideaId),
                    ideaIsNotImplemented
            )
        }
    }
})