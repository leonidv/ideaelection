package idel.tests.spec

import arrow.core.Option
import idel.tests.apiobject.*
import idel.tests.infrastructure.*

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import java.util.*

class GroupsSpec : DescribeSpec({

    val couchbase = Couchbase()
    beforeSpec {
        couchbase.clearAll()
    }

    val userA = User("userA", "group creator")
    val userB = User("userB")
    val userC = User("userC")
    val userE = User("userE")
    val userD = User("userD", "not member")

    context("userA, userB, userC, user is registered in the app") {
        describe("initialization") {
            registryUsers(userA, userB, userC, userE, userD)
        }

        describe("positive scenarios") {
            describe("create a new group") {
                describe("add new group") {
                    val response = userA.groups.create(
                        name = "123",
                        entryMode = GroupsApi.PUBLIC,
                        description = "234",
                        admins = setOf(userB.id, userC.id)
                    )

                    checkIsOk(
                        response,
                        groupHasName("123"),
                        groupHasEntryMode(GroupsApi.PUBLIC),
                        groupHasDescription("234"),
                        groupHasAdmin(userA),
                        groupHasAdmin(userB),
                        groupHasAdmin(userC)
                    )
                }

                table(
                    headers("entry mode"),
                    row(GroupsApi.PUBLIC),
                    row(GroupsApi.CLOSED),
                    row(GroupsApi.PRIVATE)
                ).forAll {entryMode ->
                    describe("group with entry mode $entryMode") {
                        val response = userA.groups.create(
                            name = "test $entryMode",
                            entryMode = entryMode,
                        )

                        checkIsOk(
                            response,
                            groupHasEntryMode(entryMode)
                        )
                    }
                }
            }

            describe("members loading") {
                lateinit var groupId : String

                describe("$userA creates PUBLIC group with members $userB, $userC") {
                    groupId = initGroup(userA, setOf(userB, userC))
                    userB.role = "member"
                    userC.role = "member"
                }

                describe("loading members") {
                    listOf(userA, userB, userC).forEach {user ->
                        describe("$user can load members list") {
                            val response = user.groups.loadMembers(groupId)
                            checkIsOk(response,
                                dataListSize(2),
                                groupHasMember(userB),
                                groupHasMember(userC),
                                groupHasNotMember(userA)
                            )
                        }
                    }

                    describe("pageable (first = 0, last = 1) and return $userC") {
                        val response = userA.groups.loadMembers(
                            groupId,
                            first = Option.just("0"),
                            last = Option.just("1")
                        )

                        checkIsOk(response,
                            dataListSize(1),
                            groupHasMember(userC)
                        )

                    }

                    describe("pageable (first = 1, last = 2) and return $userB") {
                        val response = userA.groups.loadMembers(
                            groupId,
                            first = Option.just("1"),
                            last = Option.just("2")
                        )

                        checkIsOk(response,
                            dataListSize(1),
                            groupHasMember(userB)
                        )

                    }

                    describe("username filter [user] returns [$userC, $userB]") {
                        val response = userA.groups.loadMembers(
                            groupId,
                            usernameFilter = Option.just("user")
                        )

                        checkIsOk(response,
                            dataListSize(2),
                            groupHasMember(userC),
                            groupHasMember(userB)
                        )
                    }

                    describe("username filter is [erB] returns [$userB]") {
                        val response = userA.groups.loadMembers(
                            groupId,
                            usernameFilter = Option.just("erB")
                        )

                        checkIsOk(response,
                            dataListSize(1),
                            groupHasMember(userB)
                        )
                    }

                    describe("username filter is [generated uuid] returns empty array") {
                        val response = userA.groups.loadMembers(
                            groupId,
                            usernameFilter = Option.just(UUID.randomUUID().toString())
                        )

                        checkIsOk(response,
                            dataListSize(0)
                        )
                    }
                }
            }

            describe("deleting members") {
                lateinit var groupId : String

                describe("$userA creates PUBLIC group with members $userB, $userC, $userE") {
                    groupId = initGroup(userA, setOf(userB, userC, userE))
                    userB.role = "member"
                    userC.role = "member"
                    userE.role = "member"
                }


                describe("member list contains [$userE, $userC, $userB]") {
                    val response = userA.groups.loadMembers(groupId)
                    checkIsOk(response,
                        dataListSize(3),
                        groupHasMember(userC),
                        groupHasMember(userB),
                        groupHasMember(userE)
                    )
                }

                describe("$userB leaves the group") {
                    val response = userB.groups.deleteMember(groupId, userB.id)
                    checkIsOk(response)
                    userB.role = "ex-member"
                }

                describe("member list doesnt contains $userB and is [$userE, $userC]") {
                    val response = userA.groups.loadMembers(groupId)

                    checkIsOk(response,
                        dataListSize(2),
                        groupHasMember(userE),
                        groupHasMember(userC)
                    )
                }

                listOf(userB, userE, userD).forEach { user ->
                    describe("$user can't remove $userC") {
                        val response = user.groups.deleteMember(groupId, userC.id)

                        checkIsForbidden(response)
                    }
                }

                describe("$userA can kick $userC from the group") {
                    val response = userA.groups.deleteMember(groupId, userC.id)
                    userC.role = "ex-member"
                    checkIsOk(response)
                }

                describe("group contains only $userE") {
                    val response = userA.groups.loadMembers(groupId)

                    checkIsOk(response,
                        dataListSize(1),
                        groupHasMember(userE)
                    )
                }
            }
        }
    }
})

