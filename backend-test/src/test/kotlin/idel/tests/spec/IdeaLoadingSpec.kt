package idel.tests.spec

import arrow.core.Some
import idel.tests.apiobject.*
import idel.tests.infrastructure.*
import io.kotest.core.spec.style.DescribeSpec

import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table


class IdeaLoadingSpec : DescribeSpec({

    beforeSpec {
        EntityStorage().clearAll()
    }

    val userA = User("userA", "group admin")
    val userB = User("userB", "member")
    val userC = User("userC", "notmember")

    val voters = (1..9).map {User("user0$it", "voter")}.toTypedArray()


    describe("init") {
        registryUsers(userA, userB, userC, *voters)
    }

    describe("security") {
        lateinit var groupId: String

        describe("init group") {
            groupId = createGroup(userA, members = setOf(userB)).groupId
        }

        describe("add idea by $userA") {
            userA.ideas.quickAdd(groupId, "1")
        }

        describe("add idea by $userB") {
            userB.ideas.quickAdd(groupId, "2")
        }

        describe("$userA can load ideas") {
            checkIsOk(userA.ideas.list(groupId))
        }

        describe("$userB can load ideas") {
            checkIsOk(userB.ideas.list(groupId))
        }

        describe("$userC can't load ideas") {
            checkIsForbidden(userC.ideas.list(groupId))
        }
    }

    describe("filter by text") {
        lateinit var groupId: String
        describe("init group") {
            groupId = createGroup(userA, members = setOf()).groupId
        }

        describe("add idea with summary [my best idea] and description [the best description]") {
            val response = userA.ideas.add(
                groupId = groupId,
                summary = "my best idea",
                description = "the best description"
            )

            checkIsOk(response)
            Thread.sleep(1000) // time for couchbase to index changes
        }

        describe("add idea with summary [another idea] and description [all should vote for it!]") {
            val response = userA.ideas.add(
                groupId = groupId,
                summary = "another idea",
                description = "all should vote for it"
            )

            checkIsOk(response)
            Thread.sleep(1000) // time for couchbase to index changes
        }

        val idea1Summary = "my best idea"
        val idea2Summary = "another idea"

        table(
            headers("text", "summary of expected idea"),
            row("best", arrayOf(idea1Summary)),
            row("idea", arrayOf(idea1Summary, idea2Summary)),
            row("vote", arrayOf(idea2Summary)),
            row("another", arrayOf(idea2Summary))
        ).forAll {filter: String, ideasSummary: Array<String> ->
            describe("text = [$filter]") {
                val response = userA.ideas.list(groupId = groupId, text = Some(filter))

                val containsIdeaChecks = ideasSummary.map {ideasContainsIdeaWithSummary(it)}.toTypedArray()

                checkIsOk(response, ideasCount(containsIdeaChecks.size), usersCount(1),  *containsIdeaChecks)
            }
        }
    }

    describe("sorting") {
        lateinit var groupId: String
        lateinit var ideaIds: Array<String>
        lateinit var ideasForVoting: Array<String>
        describe("init") {
            groupId = createGroup(userA, members = voters.toSet()).groupId

            describe("add 20 ideas") {
                ideaIds = (1..20).map {i ->
                    val response = userA.ideas.quickAdd(groupId, i.toString())
                    response.extractId("idea")
                }.toTypedArray()
            }

            ideasForVoting = ideaIds.copyOfRange(5,15)

            describe("first voter votes for nine ideas, second for eight ... last for one.") {
                voters.mapIndexed {index, user -> user to ideasForVoting.copyOf(voters.size - index)}
                    .forEach {(user, ideaIds) ->
                        ideaIds.forEach {ideaId ->
                            user.ideas.vote(ideaId!!) // why here may be null???
                        }
                    }
            }
        }

        describe("$userA load ideas sorted by votes") {
            val response = userA.ideas.list(groupId, ordering = Some(IdeasApi.ORDER_VOTES_DESC))
            val expectedOrder = ideasForVoting.copyOf(voters.size).map {it!!}.toTypedArray()
            checkIsOk(response, ideasOrder(expectedOrder))
        }

        describe("$userA load latest 10 ideas (ctime desc)") {
            val response = userA.ideas.list(groupId, ordering = Some(IdeasApi.ORDER_CTIME_DESC))
            val expectedOrder = ideaIds.reversedArray().copyOf(10).map {it!!}.toTypedArray()
            checkIsOk(response, ideasOrder(expectedOrder))
        }

        describe("$userA load earliest 10 ideas (ctime asc)") {
            val response = userA.ideas.list(groupId, ordering = Some(IdeasApi.ORDER_CTIME_ASC))
            val expectedOrder = ideaIds.copyOf(10).map {it!!}.toTypedArray()
            checkIsOk(response, ideasOrder(expectedOrder))
        }

    }
})