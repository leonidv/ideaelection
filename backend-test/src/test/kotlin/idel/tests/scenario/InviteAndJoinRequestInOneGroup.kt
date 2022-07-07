package idel.tests.scenario

import idel.tests.apiobject.*
import idel.tests.infrastructure.checkIsOk
import idel.tests.infrastructure.createGroup
import idel.tests.infrastructure.extractId
import idel.tests.infrastructure.registerUsers
import io.kotest.core.spec.style.DescribeSpec

class InviteAndJoinRequestInOneGroup : DescribeSpec({
    beforeSpec {
        EntityStorage().clearAll()
    }
    val userA = User("userA", "admin")
    val userB = User("userB")
    context("create users") {
        registerUsers(userA, userB)
    }

    context("user creates a join request, admin creates an invite") {

        listOf(GroupsApi.EntryMode.CLOSED, GroupsApi.EntryMode.PRIVATE).forEach {entryMode ->
            describe("group's entryMode = $entryMode") {
                lateinit var groupId: String
                lateinit var joiningKey: String

                lateinit var inviteId: String
                lateinit var joinRequestId: String

                val groupInfo = createGroup(groupAdmin = userA, members = emptySet(), entryMode = entryMode)
                groupId = groupInfo.groupId
                joiningKey = groupInfo.joiningKey


                describe("$userB creates join request") {
                    val response = userB.joinRequests.create(joiningKey)

                    joinRequestId = response.extractId("joinRequest")

                    checkIsOk(response)
                }

                describe("$userA creates an invite, which is auto approved by join request") {
                    val response =
                        userA.invites.create(groupId, registeredUsers = arrayOf(userB), newUsersEmails = emptyArray())
                    inviteId = extractInviteId(userB, groupId, response)
                    checkIsOk(
                        response,
                        hasInviteForUser(groupId, userB, InvitesApi.Fields.approved)
                    )
                }

                describe("$userB is a member of the group") {
                    var response = userB.groups.loadMembers(groupId)
                    checkIsOk(response, groupHasMember(userB))
                }

                describe("$userB doesn't see an invite in his invites") {
                    val response = userB.invites.loadForUser()

                    checkIsOk(
                        response,
                        notIncludeInvite(inviteId)
                    )
                }

                describe("$userB doesnt see a join request in his joinrequests") {
                    val response = userB.joinRequests.loadForUser()
                    checkIsOk(
                        response,
                        notIncludeJoinRequest(joinRequestId)
                    )
                }

                describe("$userA doesn't see an invite in group's invites") {
                    val response = userA.invites.loadForGroup(groupId)
                    checkIsOk(
                        response,
                        notIncludeInvite(inviteId)
                    )
                }

                describe("$userA doesn't see the join request in the group's joinrequests") {
                    val response = userA.joinRequests.loadForGroup(groupId)

                    checkIsOk(
                        response,
                        notIncludeJoinRequest(joinRequestId)
                    )
                }
            }
        }
    }

    context("admin creates an invite, user creates join requests") {
        listOf(GroupsApi.EntryMode.CLOSED, GroupsApi.EntryMode.PRIVATE).forEach {entryMode ->
            describe("group's entryMode = $entryMode") {

                lateinit var groupId: String
                lateinit var joiningKey: String

                lateinit var inviteId: String
                lateinit var joinRequestId: String


                val groupInfo =
                    createGroup(groupAdmin = userA, members = emptySet(), entryMode = entryMode)
                groupId = groupInfo.groupId
                joiningKey = groupInfo.joiningKey

                describe("$userA creates invite") {
                    val response =
                        userA.invites.create(groupId, registeredUsers = arrayOf(userB), newUsersEmails = emptyArray())

                    checkIsOk(
                        response,
                        hasInviteForUser(groupId, userB, InvitesApi.Fields.unResolved)
                    )

                    inviteId = extractInviteId(userB, groupId, response)
                }

                describe("$userB see the invite in his invites") {
                    val response = userB.invites.loadForUser()

                    checkIsOk(
                        response,
                        includeInvite(inviteId)
                    )
                }

                describe("$userB creates a joinRequest, which is auto approved by the invite") {
                    val response = userB.joinRequests.create(joiningKey)

                    joinRequestId = response.extractId("joinRequest")

                    println("!!!!!!!!!" + response.body()!!.toPrettyString())

                    checkIsOk(
                        response,
                        joinRequestIsApproved
                    )
                }

                describe("$userB has access to group") {
                    val response = userB.groups.loadMembers(groupId)

                    checkIsOk(
                        response,
                        groupHasMember(userB)
                    )
                }

                describe("$userB doesn't see the invite in his invites") {
                    val response = userB.invites.loadForUser()
                    checkIsOk(
                        response,
                        hasInvitesCount(0)
                    )
                }

                describe("$userB doesn't see join request in his join requests") {
                    val response = userB.joinRequests.loadForUser()
                    checkIsOk(
                        response,
                        hasJoinRequestsCount(0)
                    )
                }

                describe("$userA doesn't see join request in group's join requests") {
                    val response = userA.joinRequests.loadForGroup(groupId)
                    checkIsOk(
                        response,
                        hasJoinRequestsCount(0)

                    )
                }

                describe("$userA doesn't see invites in group's invites") {
                    val response = userA.invites.loadForGroup(groupId)

                    checkIsOk(
                        response,
                        hasInvitesCount(0)
                    )
                }
            }
        }
    }
})