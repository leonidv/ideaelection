package idel.tests.scenario

import idel.tests.apiobject.*
import idel.tests.infrastructure.*
import io.kotest.core.spec.style.DescribeSpec


class JoinToPrivateGroupScenarios : DescribeSpec({

    val couchbase = Couchbase()

    val userA = User("userA", "group admin")
    val userB = User("userB", "not member")
    val userC = User("userC", "not member")

    listOf(GroupsApi.PRIVATE, GroupsApi.CLOSED).forEach {entryMode ->

        context("$userA creates ${entryMode} group, userB and userC try to join") {
            describe("register users") {
                couchbase.clearAll()
                registryUsers(userA, userB, userC)
            }


            lateinit var groupId: String
            describe("create group with entryMode $entryMode") {
                groupId = initGroup(groupAdmin = userA, members = setOf(), entryMode)
            }

            describe("$userB doesn't has group in his groups") {
                checkIsOk(
                    userB.groups.loadForUser(),
                    noGroups()
                )
            }

            describe("$userC doesn't has group in his groups") {
                checkIsOk(
                    userC.groups.loadForUser(),
                    noGroups()
                )
            }

            describe("$userB successfully joins to the group") {
                lateinit var joinRequestId: String

                describe("$userB creates a join request to the group") {
                    val response = userB.joinRequests.create(groupId)
                    checkIsOk(
                        response,
                        hasId,
                        joinRequestIsUnresolved
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
                    checkIsOk(response, joinRequestIsApproved)
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
                        includeJoinRequestWithStatus(joinRequestId, JoinRequestsApi.APPROVED)
                    )
                }
            }

            describe("$userC gets decline for his request") {
                lateinit var joinRequestId: String

                describe("$userC creates a join request to the group") {
                    val response = userC.joinRequests.create(groupId)
                    checkIsOk(
                        response,
                        hasId,
                        joinRequestIsUnresolved
                    )

                    joinRequestId = extractId(response)
                }

                describe("$userC don't see the group in the list of his groups") {
                    checkIsOk(
                        userC.groups.loadForUser(),
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

                describe("$userA declines join request") {
                    val response = userA.joinRequests.changeStatus(joinRequestId, JoinRequestsApi.DECLINED)
                    checkIsOk(response, joinRequestIsDeclined)
                }

                describe("$userC again don't see the group in the list of his groups") {
                    checkIsOk(
                        userC.groups.loadForUser(),
                        noGroups()
                    )
                }

                describe("$userC see join request in list with status DECLINED") {
                    val response = userC.joinRequests.loadForUser()
                    checkIsOk(
                        response,
                        includeJoinRequestWithStatus(joinRequestId, JoinRequestsApi.DECLINED),
                    )
                }

            }

        }
    }
})