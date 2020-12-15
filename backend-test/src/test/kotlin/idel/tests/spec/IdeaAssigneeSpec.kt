package idel.tests.spec

import arrow.core.Some
import idel.tests.apiobject.*
import idel.tests.infrastructure.*
import idel.tests.infrastructure.JsonNodeExtensions.dataId
import idel.tests.shouldBeOk
import io.kotest.core.spec.style.DescribeSpec

class IdeaAssigneeSpec : DescribeSpec({
    val couchbase = Couchbase()
    beforeSpec {
        couchbase.clearAll()
    }

    context("userA is admin, userB is author, userC is member, userD is not member") {
        val userAdmin = User("userAdmin")
        val userA = User("userA")
        val userB = User("userB")
        val userC = User("userC")
        val userD = User("userD")

        lateinit var groupId: String
        lateinit var ideaId: String

        describe("init") {
            describe("register users") {
                listOf(userA, userB, userC, userD).forEach {user ->
                    it("register user [${user.name}]") {
                        userAdmin.users.register(user.name).shouldBeOk()
                    }
                }
            }

            describe("create public group and add userB, userC to it") {
                val response = userA.groups.create("assignee spec group", GroupsApi.PUBLIC)

                checkIsOk(response)

                groupId = (response.body().dataId() as Some).t
            }

            describe("$userB join to group") {
                checkIsOk(userB.joinRequests.create(groupId), checkJoinRequestIsApproved())
            }

            describe("$userC join to group") {
                checkIsOk(userC.joinRequests.create(groupId), checkJoinRequestIsApproved())
            }

            describe("$userB create an idea") {
                val response = userB.ideas.add(groupId, summary = "idea for assignee spec")
                checkIsOk(response)
                ideaId = (response.body().dataId() as Some).t
            }
        }

        context("idea is unassigned") {
            describe("$userD (not member) can't assign idea himself") {
                checkIsForbidden(userD.ideas.assign(ideaId, userD))
            }
        }

        describe("$userA (admin) can't assign to $userD (not member)") {
            checkIsForbidden(userA.ideas.assign(ideaId, userD))
        }

        describe("$userC assign idea himself (idea is yet not assigned)") {
            checkIsOk(
                    userC.ideas.assign(ideaId, userC),
                    ideaAssigneeIs(userC))
        }

        context("idea is assigned to $userC") {
            describe("$userC (assignee) can't assignee to another ($userB)") {
                checkIsForbidden(userC.ideas.assign(ideaId, userB))
            }

            describe("$userB (author) can't assign to himself already assigned idea") {
                checkIsForbidden(userB.ideas.assign(ideaId, userB))
            }

            describe("$userD (not member) can't assign to himself already assigned idea") {
                checkIsForbidden(userB.ideas.assign(ideaId, userD))
            }

            describe("$userB (author) can't remove assignee") {
                checkIsForbidden(userB.ideas.removeAssignee(ideaId))
            }
        }


//        describe("$userC (assignee)")

        describe("$userA (group admin) change assignee to $userB") {
            checkIsOk(
                    userA.ideas.assign(ideaId, userB),
                    ideaAssigneeIs(userB))
        }


        context("idea is assigned to $userB") {
            describe("$userC (ex-assignee) can't assign to himself already assigned idea") {
                checkIsForbidden(userC.ideas.assign(ideaId, userC))
            }

            describe("$userB can deny to implement idea (remove himself as assignee)") {
                checkIsOk(
                        userB.ideas.removeAssignee(ideaId),
                        ideaNotAssigned()
                )
            }
        }

        describe("$userA (admin) assign idea to $userB") {
            checkIsOk(
                    userA.ideas.assign(ideaId, userB),
                    ideaAssigneeIs(userB)
            )
        }

        describe("$userA (admin) change assignee to $userC") {
            checkIsOk(
                    userA.ideas.assign(ideaId, userC),
                    ideaAssigneeIs(userC)
            )
        }

        describe("$userA (admin) can remove assignee") {
            checkIsOk(
                    userA.ideas.removeAssignee(ideaId),
                    ideaNotAssigned())
        }

    }
})