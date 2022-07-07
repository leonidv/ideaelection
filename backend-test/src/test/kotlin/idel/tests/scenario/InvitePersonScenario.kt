package idel.tests.scenario

import idel.tests.apiobject.*
import idel.tests.infrastructure.checkIsOk
import idel.tests.infrastructure.createGroup
import idel.tests.infrastructure.registerUsers
import io.kotest.core.spec.style.DescribeSpec

class InvitePersonScenario : DescribeSpec({

    beforeSpec {
        EntityStorage().clearAll()
    }

    describe("initialization") {
        val userA = User("userA", "admin")
        val userB = User("userB")
        val userC = User("userC")
        val userD = User("userD")


        registerUsers(userA)

        val userPerGroup = mapOf(
            GroupsApi.EntryMode.PUBLIC to userB,
            GroupsApi.EntryMode.CLOSED to userC,
            GroupsApi.EntryMode.PRIVATE to userD
        )

        listOf(GroupsApi.EntryMode.PUBLIC, GroupsApi.EntryMode.CLOSED, GroupsApi.EntryMode.PRIVATE).forEach {entryMode ->
            describe("invite persons (not yet registered) to $entryMode group") {
                val groupId = createGroup(groupAdmin = userA, members = emptySet(), entryMode = entryMode).groupId

                val user = userPerGroup[entryMode]!!

                describe("$userA invites person by email [${user.email}] ") {
                    val response = userA.invites.create(
                        groupId,
                        registeredUsers = emptyArray(),
                        newUsersEmails = arrayOf(user.email)
                    )
                    checkIsOk(
                        response,
                        hasInviteForPerson(groupId, user.email),
                    )
                }

                describe("$userA can see invite in the group's invites") {
                    val response = userA.invites.loadForGroup(groupId)
                    checkIsOk(response,
                        hasInviteForPerson(groupId, user.email)
                    )
                }

                registerUsers(user)

                describe("$user see the invite in the his invite's list") {
                        val response = user.invites.loadForUser()
                    checkIsOk(response,
                        hasInviteForUser(groupId, user)
                    )
                }

            }
        }
    }
})