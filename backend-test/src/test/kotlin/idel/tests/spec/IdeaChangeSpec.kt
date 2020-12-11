package idel.tests.spec

import arrow.core.Some
import idel.tests.apiobject.Couchbase
import idel.tests.apiobject.GroupsApi
import idel.tests.apiobject.User
import idel.tests.infrastructure.*
import idel.tests.infrastructure.JsonNodeExtensions.dataId
import idel.tests.shouldBeOk
import io.kotest.core.spec.style.DescribeSpec

class IdeaChangeSpec : DescribeSpec({

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


            describe("$userB (author) can update properties of idea (is an author of an unassigned idea)") {
                checkIsOk(userB.ideas.update(ideaId))
            }

            describe("$userC (group member) can't update properties of idea") {
                checkIsForbidden(userC.ideas.update(ideaId))
            }

        }


        describe("$userC assign idea himself (idea is yet not assigned)") {
            checkIsOk(
                    userC.ideas.assign(ideaId, userC),
                    checkAssignee(userC))
        }

        context("idea is assigned to $userC") {

            describe("$userC (assignee) can update properties of idea") {
                checkIsOk(userC.ideas.update(ideaId, summary = "Changes from userC"))
            }

            describe("$userB (author) can't assign to himself already assigned idea") {
                checkIsForbidden(userB.ideas.assign(ideaId, userB))
            }

            describe("$userB (author) can't update an idea which is assigned to another user") {
                checkIsForbidden(userB.ideas.update(ideaId))
            }

        }

        describe("$userA (group admin) can change assignee to $userB") {
            checkIsOk(
                    userA.ideas.assign(ideaId, userB),
                    checkAssignee(userB))
        }


        context("idea is assigned to $userB") {

            describe("$userB (assignee) can update properties of idea") {
                checkIsOk(userB.ideas.update(ideaId, summary = "Changes from userB"))
            }

            describe("$userC (group member) can't assign to himself already assigned idea") {
                checkIsForbidden(userC.ideas.assign(ideaId, userC))
            }
        }

        describe("$userA (group admin) can remove assignee") {
            checkIsOk(
                    userA.ideas.removeAssignee(ideaId),
                    checkNotAssigned())
        }

    }

})