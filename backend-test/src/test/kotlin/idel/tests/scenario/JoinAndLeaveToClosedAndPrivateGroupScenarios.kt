package idel.tests.scenario

import idel.tests.apiobject.*
import idel.tests.infrastructure.*
import io.kotest.core.spec.style.DescribeSpec


class JoinAndLeaveToClosedAndPrivateGroupScenarios : DescribeSpec({

    val couchbase = EntityStorage()

    val userA = User("userA", "group admin")
    val userB = User("userB", "not member")
    val userC = User("userC", "not member")

    val entryMode = GroupsApi.CLOSED

    context("$userA creates $entryMode group, userB and userC try to join") {
        describe("register users") {
            couchbase.clearAll()
            registryUsers(userA, userB, userC)
        }


        lateinit var groupId: String
        lateinit var joiningKey: String
        describe("create group with entryMode $entryMode") {
            val createGroupResponse = userA.groups.create("$entryMode group", entryMode)
            checkIsOk(
                createGroupResponse,
                hasId,
                groupHasMembersCount(1)
            )
            groupId = createGroupResponse.extractId()
            joiningKey = createGroupResponse.extractField(GroupsApi.Fields.JOINING_KEY)
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


            describe("$userB creates a join request to the group by group's link") {
                val response = userB.joinRequests.create(joiningKey)
                checkIsOk(
                    response,
                    hasId,
                    joinRequestIsUnresolved
                )

                joinRequestId = response.extractId()
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

            describe("$userA see in the group info that the group's has 2 members") {
                val loadGroupResponse = userA.groups.load(groupId)
                checkIsOk(loadGroupResponse, groupHasMembersCount(2))
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

            listOf(userA, userB).forEach {user ->
                describe("$user can see $userB in the group's member list") {
                    val response = userB.groups.loadMembers(groupId)
                    checkIsOk(
                        response,
                        groupHasMember(userB)
                    )
                }
            }

            describe("$userB leave groups") {
                val response = userB.groups.deleteMember(groupId, userB.id)
                checkIsOk(response)
            }

            describe("$userB can't see members of group anymore") {
                val response = userB.groups.loadMembers(groupId)
                checkIsForbidden(response)
            }

            describe("$userA see in the group info that the group's has 1 members") {
                val loadGroupResponse = userA.groups.load(groupId)
                checkIsOk(loadGroupResponse, groupHasMembersCount(1))
            }

            describe("$userA don't see $userB in the list of group's members") {
                val response = userA.groups.loadMembers(groupId)
                checkIsOk(response, groupHasNotMember(userB))
            }

        }

        describe("$userC gets decline for his request") {
            lateinit var joinRequestId: String

            describe("$userC creates a join request to the group") {
                val response = userC.joinRequests.create(joiningKey)
                checkIsOk(
                    response,
                    hasId,
                    joinRequestIsUnresolved
                )

                joinRequestId = response.extractId()
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

            describe("$userA don't see $userC in the list of group's members") {
                checkIsOk(
                    userA.groups.loadMembers(groupId),
                    groupHasNotMember(userC)
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
})