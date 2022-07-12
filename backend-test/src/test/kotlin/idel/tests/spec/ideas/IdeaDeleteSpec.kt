package idel.tests.spec.ideas

import idel.tests.apiobject.EntityStorage
import idel.tests.apiobject.User
import idel.tests.apiobject.includeIdea
import idel.tests.apiobject.notIncludesIdea
import idel.tests.infrastructure.*
import io.kotest.core.spec.style.DescribeSpec

class IdeaDeleteSpec : DescribeSpec({
    beforeSpec {
        EntityStorage().clearAll()
    }

    val userA = User("userA", "group admin")
    val userB = User("userB", "member")

    lateinit var groupId : String
    lateinit var ideaId: String
    describe("initialization") {
        registerUsers(userA, userB)

        groupId = createGroup(userA, members = setOf(userB)).groupId

        describe("add idea to group") {
            val addIdeaResponse = userA.ideas.quickAdd(groupId, "1")
            checkIsOk(addIdeaResponse)

            ideaId = addIdeaResponse.extractId("idea")
        }

        describe("idea is available in the list of group's ideas") {
            val response = userA.ideas.list(groupId)
            checkIsOk(
                response,
                includeIdea(ideaId)
            )
        }
    }

    describe("$userA delete idea") {
        val response = userA.ideas.delete(ideaId)
        checkIsOk(response)
    }

    describe("$userA don't see idea in the list of group's idea") {
        val response = userA.ideas.list(groupId)
        checkIsOk(response,
            notIncludesIdea(ideaId)
        )
    }

    describe("$userA can't load idea") {
        val response = userA.ideas.load(ideaId)
        checkIsNotFound(response)
    }

    describe("$userA can't edit idea") {
        val response = userA.ideas.quickEdit(ideaId, "2")
        checkIsNotFound(response)
    }

    describe("$userA can't change assignee") {
        val response = userA.ideas.assign(ideaId, userB)
        checkIsNotFound(response)
    }

    describe("$userA can't vote for idea") {
        checkIsNotFound(
            userA.ideas.vote(ideaId)
        )
    }

    describe("$userA can't devote for idea") {
        checkIsNotFound(
            userA.ideas.devote(ideaId)
        )
    }

    describe("$userA can't archive idea") {
        checkIsNotFound(
            userA.ideas.changeArchived(ideaId, true)
        )
    }

    describe("$userA can't unarchive idea") {
        checkIsNotFound(
            userA.ideas.changeArchived(ideaId, false)
        )
    }

    describe("$userA can't delete idea") {
        checkIsNotFound(
            userA.ideas.delete(ideaId)
        )
    }
})