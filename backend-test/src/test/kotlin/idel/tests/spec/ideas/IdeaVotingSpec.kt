package idel.tests.spec.ideas

import idel.tests.apiobject.*
import idel.tests.infrastructure.*
import io.kotest.core.spec.style.DescribeSpec

class IdeaVotingSpec : DescribeSpec({

    beforeSpec {
        EntityStorage().clearAll()
    }

    val userA = User("userA", "admin")
    val userB = User("userB", "member")
    val userC = User("userC", "not member")

    lateinit var groupId : String

    lateinit var ideaId : String

    describe("init") {
        registerUsers(userA, userB, userC)
        groupId = createGroup(userA, members = setOf(userB)).groupId


        describe("$userA adds idea") {
            val response = userA.ideas.quickAdd(groupId, "1")
            checkIsOk(response)
            ideaId = response.extractId("idea")
        }
    }

    describe("positive scenarios") {

        describe("$userA votes for his idea") {
            val response = userA.ideas.vote(ideaId)
            checkIsOk(response)
        }

        describe("$userA is in the voters list") {
            val response = userA.ideas.load(ideaId)
            checkIsOk(
                response,
                ideaHasVoter(userA),
                usersInfoCount(1),
                usersInfoContains(setOf(userA))
            )
        }

        describe("$userB votes for the idea of $userB ") {
            val response = userB.ideas.vote(ideaId)
            checkIsOk(response)
        }

        describe("voters is [$userA, $userB]") {
            val response = userA.ideas.load(ideaId)

            checkIsOk(
                response,
                ideaHasVoterCount(2),
                ideaHasVoter(userA),
                ideaHasVoter(userB),
                usersInfoCount(2),
                usersInfoContains(setOf(userA, userB))
            )
        }

        describe("$userA remove his vote") {
            val response = userA.ideas.devote(ideaId)
            checkIsOk(response)
        }

        describe("voters is [$userB]") {
            val response = userA.ideas.load(ideaId)
            checkIsOk(
                response,
                ideaHasVoterCount(1),
                ideaHasVoter(userB),
                usersInfoCount(2),
                usersInfoContains(setOf(userA, userB))
            )
        }

        describe("$userB remove his vote") {
            val response = userB.ideas.devote(ideaId)
            checkIsOk(
                response,
                ideaHasVoterCount(0))
        }

        describe("no voters") {
            var response = userA.ideas.load(ideaId)
            checkIsOk(
                response,
                ideaHasVoterCount(0),
                usersInfoCount(1),
                usersInfoContains(setOf(userA))
            )
        }
    }

    describe("security") {
        describe("$userC can't vote for idea") {
            val response = userC.ideas.vote(ideaId)
            checkIsForbidden(response)
        }
    }
})