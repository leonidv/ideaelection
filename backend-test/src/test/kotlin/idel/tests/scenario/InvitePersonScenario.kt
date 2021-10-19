package idel.tests.scenario

import idel.tests.apiobject.*
import idel.tests.infrastructure.checkIsOk
import idel.tests.infrastructure.createGroup
import idel.tests.infrastructure.registryUsers
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


        registryUsers(userA)

        val userPerGroup = mapOf(
            GroupsApi.PUBLIC to userB,
            GroupsApi.CLOSED to userC,
            GroupsApi.PRIVATE to userD
        )

        listOf(GroupsApi.PUBLIC, GroupsApi.CLOSED, GroupsApi.PRIVATE).forEach {entryMode ->
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

                registryUsers(user)

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