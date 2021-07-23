package idel.tests.scenario

import idel.tests.apiobject.EntityStorage
import idel.tests.apiobject.GroupsApi
import idel.tests.apiobject.User
import idel.tests.infrastructure.*
import io.kotest.core.spec.style.DescribeSpec

class AccessToPrivateGroupScenarios : DescribeSpec({
    val entityStorage = EntityStorage()

    val userA = User("userA", "group admin")
    val userB = User("userB", "member")
    val userC = User("userC", "not member")

    lateinit var groupId : String
    lateinit var joiningKey : String

    beforeSpec {
        entityStorage.clearAll()
    }

    describe("create users") {
        registryUsers(userA, userB, userC)
    }



    describe("create private group") {
        val groupInfo = createGroup(groupAdmin = userA, members = setOf(userB), entryMode = GroupsApi.PRIVATE)
        groupId = groupInfo.groupId
        joiningKey = groupInfo.joiningKey
    }


    describe("group info") {

        describe("$userA can load group info by group link") {
            val groupResponse = userA.groups.load(groupId)
            checkIsOk(groupResponse)
        }

        describe("$userB can load group info by group link") {
            val groupResponse = userB.groups.load(groupId)
            checkIsOk(groupResponse)
        }

        describe("$userC can't load group info by group link") {
            val groupResponse = userC.groups.load(groupId)
            checkIsNotFound(groupResponse)
        }

        describe("$userC can load group info by invite link") {
            val groupResponse = userC.groups.loadByLinkToJoin(joiningKey)
            checkIsOk(groupResponse, entityIdIs(groupId))
        }

    }
})