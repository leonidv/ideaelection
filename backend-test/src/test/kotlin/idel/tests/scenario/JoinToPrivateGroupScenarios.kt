package idel.tests.scenario

import idel.tests.apiobject.*
import idel.tests.infrastructure.*
import io.kotest.core.spec.style.DescribeSpec


class JoinToPrivateGroupScenarios : DescribeSpec({

    val couchbase = Couchbase()

    beforeSpec {
        couchbase.clearAll()
    }

    val userA = User("userA", "group admin")
    val userB = User("userB", "not member")
    val userC = User("userC", "not member")

    val entryMode = GroupsApi.PRIVATE

    context("$userA creates ${entryMode} group, userB and userC try to join") {
        lateinit var groupId: String
        describe("initialization") {
            registryUsers(userA, userB, userC)
            groupId = initGroup(groupAdmin = userA, members = setOf(), entryMode)
        }

        describe("$userB doesn't has group in his groups") {
            checkIsOk(
                userB.groups.loadForUser(),
                noGroups()
            )
        }

        describe("$userB successfully joins to the group") {
            lateinit var joinRequestId: String

            describe("$userB create join request to group") {
                val response = userB.joinRequests.create(groupId)
                checkIsOk(
                    response,
                    hasId,
                    joinRequestIsUnresolved()
                )

                joinRequestId = extractId(response)
            }

            describe("$userB see join request in his list") {
                val response = userB.joinRequests.loadForUser()
                checkIsOk(
                    response,
                    includeJoinRequest(joinRequestId)
                )
            }

            describe("$userB still don't see the group in the list of his groups") {
                checkIsOk(
                    userB.groups.loadForUser(),
                    noGroups()
                )
            }

            describe("$userA see join request in the the list of group's join requests") {
                val response = userA.joinRequests.loadForGroup(groupId)
                checkIsOk(
                    response,
                    includeJoinRequest(joinRequestId)
                )
            }

            describe("$userA approve join request") {
                val response = userA.joinRequests.changeStatus(joinRequestId, JoinRequestsApi.APPROVED)
                checkIsOk(response, joinRequestIsApproved())
            }

            describe("$userB see the group in the list of his groups") {
                val response = userB.groups.loadForUser()
                checkIsOk(
                    response,
                    includeGroup(groupId)
                )
            }

            describe("$userB see join request in list with status APPROVED") {
                val response = userB.joinRequests.loadForUser()
                checkIsOk(
                    response,
                    includeJoinRequest(joinRequestId),
                    BodyArrayElementExists("join request is approved", "$.data", "status", JoinRequestsApi.APPROVED)
                )
            }
        }


    }
})