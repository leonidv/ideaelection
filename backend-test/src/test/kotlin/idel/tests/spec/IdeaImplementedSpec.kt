package idel.tests.spec

import arrow.core.Some
import idel.tests.apiobject.*
import idel.tests.infrastructure.*
import idel.tests.infrastructure.JsonNodeExtensions.dataId
import io.kotest.core.spec.style.DescribeSpec

class IdeaImplementedSpec : DescribeSpec({
    val couchbase = EntityStorage()
    beforeSpec {
        couchbase.clearAll()
    }
    context("userA is admin, userB is author, userC is member, userD is not member, userE is assignee") {
        val userA = User("userA")
        val userB = User("userB")
        val userC = User("userC")
        val userD = User("userD")
        val userE = User("userE")

        lateinit var groupId: String
        lateinit var ideaId: String

        describe("init") {
            registryUsers(userA, userB, userC, userD, userE)

            groupId = createGroup(userA, setOf(userB, userC, userE)).groupId

            describe("$userB create an idea, by default idea is not implemented") {
                val response = userB.ideas.add(groupId, summary = "idea for assignee spec")
                checkIsOk(response, ideaIsNotImplemented)
                ideaId = response.extractId("idea")
            }
        }

        describe("$userE assigns idea him self") {
            checkIsOk(userE.ideas.assign(ideaId, userE))
        }


        listOf(userB, userC, userD).forEach {user ->
            describe("$user can't mark as implemented idea, which is assigned to another user") {
                checkIsForbidden(user.ideas.implemented(ideaId))
            }
        }

        describe("$userE (assignee) mark as implemented") {
            checkIsOk(
                    userE.ideas.implemented(ideaId),
                    ideaIsImplemented)
        }

        listOf(userB, userC, userD).forEach {user ->
            describe("$user can't mark as not implemented") {
                checkIsForbidden(user.ideas.notImplemented(ideaId))
            }
        }

        describe("$userE (assignee) can mark as not implemented") {
            checkIsOk(
                    userE.ideas.notImplemented(ideaId),
                    ideaIsNotImplemented)
        }

        describe("$userA (group admin) can mark as implemented") {
            checkIsOk(
                    userA.ideas.implemented(ideaId),
                    ideaIsImplemented)
        }

        describe("$userA (group admin) can mark as not implemented") {
            checkIsOk(
                    userA.ideas.notImplemented(ideaId),
                    ideaIsNotImplemented
            )
        }
    }
})