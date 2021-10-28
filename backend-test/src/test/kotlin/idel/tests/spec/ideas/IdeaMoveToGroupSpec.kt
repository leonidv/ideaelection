package idel.tests.spec.ideas

import idel.tests.apiobject.*
import idel.tests.infrastructure.*
import io.kotest.core.spec.style.DescribeSpec

class IdeaMoveToGroupSpec : DescribeSpec({
    beforeSpec {
        EntityStorage().clearAll()
    }

    val userG1_A = User("userG1_A", "admin of group 1")
    val userG2_A = User("userG2_A", "admin of group 2")
    val userG2_B = User("userG2_B", "member of group 2")
    val userB = User("userG1_B", "author, member of g1 and g2")
    val userC = User("userC", "assignee, member of g1 and g2")
    val userD = User("userD", "member of g1 and g2")


    lateinit var g1_id: String
    lateinit var g2_id: String
    describe("initialization") {
        registryUsers(userG1_A, userG2_A, userG2_B, userB, userC, userD)

        g1_id = createGroup(userG1_A, members = setOf(userB, userC, userD)).groupId
        g2_id = createGroup(userG2_A, members = setOf(userG2_B, userB, userC, userD)).groupId
    }

    describe("positive scenario") {
        lateinit var ideaId: String
        describe("$userB add idea to group 1") {
            val response = userB.ideas.quickAdd(g1_id, "1")
            checkIsOk(
                response,
                ideaHasGroup(g1_id)
            )
            ideaId = response.extractId("idea")
        }

        describe("$userB move idea to group 2") {
            val response = userB.ideas.changeGroup(ideaId, g2_id)
            checkIsOk(
                response,
                ideaHasGroup(g2_id)
            )
        }

        arrayOf(userG1_A, userB).forEach {user ->
            describe("$user doesn't see an idea in group 1") {
                val response = user.ideas.list(g1_id)
                checkIsOk(
                    response,
                    notIncludesIdea(ideaId)
                )
            }
        }

        arrayOf(userG2_A, userG2_B, userB).forEach {user ->
            describe("$user see an idea in group 2") {
                val response = user.ideas.list(g2_id)
                checkIsOk(
                    response,
                    includeIdea(ideaId)
                )
            }
        }
    }

    describe("security") {
        lateinit var ideaId: String
        describe("$userB post idea to group 1") {
            val response = userB.ideas.quickAdd(g1_id, "1")
            checkIsOk(
                response,
                ideaHasGroup(g1_id)
            )

            ideaId = response.extractId("idea")
        }

        describe("$userG1_A not member of target group and can't move idea") {
            val response = userG1_A.ideas.changeGroup(ideaId, g2_id)
            checkIsForbidden(response)
        }

        arrayOf(userG2_A, userG2_B).forEach {user ->
            describe("$user not member of idea's group and can't move idea") {
                val response = user.ideas.changeGroup(ideaId, g2_id)
                checkIsForbidden(response)
            }
        }

        describe("$userD can't move idea") {
            val response = userD.ideas.changeGroup(ideaId, g2_id)
            checkIsForbidden(response)
        }

        describe("assignee can move idea") {
            lateinit var ideaForAssign_id : String
            describe("$userB add idea") {
                val response = userB.ideas.quickAdd(g1_id,"1")
                ideaForAssign_id = response.extractId("idea")
            }

            describe("$userC assigns idea himself") {
                userC.ideas.assign(ideaForAssign_id,userC).shouldBeOk()
            }

            describe("$userC can move idea") {
                val response = userC.ideas.changeGroup(ideaForAssign_id,g2_id)
                checkIsOk(response,
                    ideaHasGroup(g2_id)
                )
            }
        }
    }
})