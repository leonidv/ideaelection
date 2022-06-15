package idel.tests.scenario

import idel.tests.apiobject.*
import idel.tests.infrastructure.*
import io.kotest.core.spec.style.DescribeSpec

class InviteRegisteredUserScenario : DescribeSpec({

    val entityStorage = EntityStorage()
    val userA = User("userA", "group's admin")
    val userB = User("userB", "not member")
    val userC = User("userC", "not member")

    beforeSpec {
        entityStorage.clearAll()
    }

    describe("initialization") {
        registerUsers(userA, userB, userC)
    }


    listOf(GroupsApi.PUBLIC, GroupsApi.CLOSED, GroupsApi.PRIVATE).forEach {entryMode ->
        describe("group's entryMode = $entryMode") {
            val groupId = createGroup(groupAdmin = userA, members = emptySet(), entryMode = entryMode).groupId

            describe("$userA creates invite for [$userB, $userC]") {
                val response = userA.invites.create(
                    groupId,
                    registeredUsers = arrayOf(userB, userC),
                    newUsersEmails = emptyArray()
                )
                checkIsOk(
                    response,
                    hasInviteForUser(groupId, userB),
                    hasInviteForUser(groupId, userC)
                )
            }

            describe("$userB approve invite") {

                lateinit var inviteId: String
                describe("$userB sees invite in his invites") {
                    val response = userB.invites.loadForUser()
                    checkIsOk(
                        response,
                        hasInviteForUser(groupId, userB)
                    )

                    inviteId = extractInviteId(userB, groupId, response)
                    println(inviteId)
                }

                describe("$userB can't see content of the group") {
                    checkIsForbidden(userB.ideas.list(groupId))
                }

                describe("$userB approves invitation") {
                    checkIsOk(userB.invites.approve(inviteId))
                }

                describe("$userB can see content of the group") {
                    checkIsOk(userB.ideas.list(groupId))
                }

                describe("$userB does not have the invite in his invites") {
                    val response = userB.invites.loadForUser()
                    checkIsOk(
                        response,
                        hasNotInviteForUser(groupId, userB)
                    )
                }

                describe("$userA does not see invite in group's invites") {
                    val response = userA.invites.loadForGroup(groupId)
                    checkIsOk(response,
                        hasNotInviteForUser(groupId, userB),
                        hasInviteForUser(groupId, userC)
                    )
                }

                describe("$userA see $userB in the group's members") {
                    val response = userA.groups.loadMembers(groupId)
                    checkIsOk(response,
                        groupHasMember(userB)
                    )
                }

            }

            describe("$userC decline invite") {

                lateinit var inviteId: String
                describe("$userC sees the invite in his invites") {
                    val response = userC.invites.loadForUser()
                    checkIsOk(
                        response,
                        hasInviteForUser(groupId, userC)
                    )
                    inviteId = extractInviteId(userC, groupId, response)
                }

                describe("$userC can't see access of the group") {
                    checkIsForbidden(userC.ideas.list(groupId))
                }

                describe("$userC decline the invite") {
                    checkIsOk(userC.invites.decline(inviteId))
                }

                describe("$userC don't see invite in his invites") {
                    val response = userC.invites.loadForUser()
                    checkIsOk(response,
                        hasNotInviteForUser(groupId, userC)
                    )
                }

                describe("$userC can't see content of the group") {
                    checkIsForbidden(userC.ideas.list(groupId))
                }

                describe("$userA can't see the invite in the group's invites") {
                    val response = userA.invites.loadForGroup(groupId)
                    checkIsOk(response,
                        hasNotInviteForUser(groupId, userC)
                    )
                }

                describe("$userA doesn't see $userC in the group's members ") {
                    val response = userA.groups.loadMembers(groupId)
                    checkIsOk(response,
                        groupHasNotMember(userC)
                    )
                }
            }
        }
    }

})
