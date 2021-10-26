package idel.tests.spec.ideas

import idel.tests.apiobject.*
import idel.tests.infrastructure.*
import io.kotest.core.spec.style.DescribeSpec

class IdeaChangeGroupSpec : DescribeSpec({
    beforeSpec {
        EntityStorage().clearAll()
    }

    val userG1_A = User("userG1_A", "admin of group 1")
    val userG2_A = User("userG2_A", "admin of group 2")
    val userG2_B = User("userG2_B", "member of group 2")
    val userB = User("userG1_B", "member of group 1 and group 2")

    lateinit var g1_id: String
    lateinit var g2_id: String
    describe("initialization") {
        registryUsers(userG1_A, userG2_A, userG2_B, userB)

        g1_id = createGroup(userG1_A, members = setOf(userB)).groupId
        g2_id = createGroup(userG2_A, members = setOf(userG2_B, userB)).groupId
    }

    describe("positive scenario") {
        lateinit var ideaId: String
        describe("$userB add idea to group 1") {
            val response = userB.ideas.quickAdd(g1_id, "1")
            checkIsOk(response,
                ideaHasGroup(g1_id)
            )
            ideaId = response.extractId("idea")
        }

        describe("$userB move idea to group 2") {
            val response = userB.ideas.changeGroup(ideaId, g2_id)
            checkIsOk(response,
                ideaHasGroup(g2_id)
            )
        }

        arrayOf(userG1_A, userB).forEach {user ->
            describe("$user doesn't see an idea in group 1") {
                val response = user.ideas.list(g1_id)
                checkIsOk(response,
                    notIncludesIdea(ideaId)
                )
            }
        }

        arrayOf(userG2_A, userG2_B, userB).forEach {user ->
            describe("$user see an idea in group 2") {
                val response = user.ideas.list(g2_id)
                checkIsOk(response,
                    includeIdea(ideaId)
                )
            }
        }
    }
})