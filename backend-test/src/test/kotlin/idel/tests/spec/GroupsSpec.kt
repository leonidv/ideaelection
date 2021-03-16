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

@Suppress("JoinDeclarationAndAssignment")
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


        describe("create a new group") {
            table(
                headers("entry mode"),
                row(GroupsApi.PUBLIC),
                row(GroupsApi.CLOSED),
                row(GroupsApi.PRIVATE)
            ).forAll {entryMode ->
                describe("group with entry mode $entryMode") {
                    val name = "test $entryMode"
                    val description = "test $entryMode description"
                    val response = userA.groups.create(
                        name = name,
                        entryMode = entryMode,
                        description = description
                    )

                    checkIsOk(
                        response,
                        groupHasName(name),
                        groupHasEntryMode(entryMode),
                        groupHasCreator(userA),
                        groupHasDescription(description)
                    )
                }
            }
        }

        describe("loading members") {
            lateinit var groupId: String

            describe("$userA creates PUBLIC group with members $userB, $userC") {
                groupId = initGroup(userA, setOf(userB, userC))
                userB.role = "member"
                userC.role = "member"
            }


            listOf(userA, userB, userC).forEach {user ->
                describe("$user can load members list") {
                    val response = user.groups.loadMembers(groupId)
                    checkIsOk(
                        response,
                        dataListSize(3),
                        groupHasAdmin(userA),
                        groupHasMember(userB),
                        groupHasMember(userC),
                    )
                }
            }

            describe("pageable (first = 0, last = 1) and return $userC") {
                val response = userA.groups.loadMembers(
                    groupId,
                    first = Option.just("0"),
                    last = Option.just("1")
                )

                checkIsOk(
                    response,
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

                checkIsOk(
                    response,
                    dataListSize(1),
                    groupHasMember(userB)
                )

            }

            describe("username filter [user] returns [$userC, $userB, $userA]") {
                val response = userA.groups.loadMembers(
                    groupId,
                    usernameFilter = Option.just("user")
                )

                checkIsOk(
                    response,
                    dataListSize(3),
                    groupHasMember(userC),
                    groupHasMember(userB),
                    groupHasAdmin(userA)
                )
            }

            describe("username filter is [erB] returns [$userB]") {
                val response = userA.groups.loadMembers(
                    groupId,
                    usernameFilter = Option.just("erB")
                )

                checkIsOk(
                    response,
                    dataListSize(1),
                    groupHasMember(userB)
                )
            }

            describe("username filter is [generated uuid] returns empty array") {
                val response = userA.groups.loadMembers(
                    groupId,
                    usernameFilter = Option.just(UUID.randomUUID().toString())
                )

                checkIsOk(
                    response,
                    dataListSize(0)
                )
            }

        }

        describe("deleting members") {
            lateinit var groupId: String

            describe("$userA creates PUBLIC group with members $userB, $userC, $userE") {
                groupId = initGroup(userA, setOf(userB, userC, userE))
                userB.role = "member"
                userC.role = "member"
                userE.role = "member"
            }


            describe("member list contains [$userE, $userC, $userB, $userA]") {
                val response = userA.groups.loadMembers(groupId)
                checkIsOk(
                    response,
                    dataListSize(4),
                    groupHasAdmin(userA),
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

            describe("member list doesnt contains $userB and is [$userE, $userC, $userA]") {
                val response = userA.groups.loadMembers(groupId)

                checkIsOk(
                    response,
                    dataListSize(3),
                    groupHasAdmin(userA),
                    groupHasMember(userE),
                    groupHasMember(userC)
                )
            }

            listOf(userB, userE, userD).forEach {user ->
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

            describe("group contains [$userE, $userA]") {
                val response = userA.groups.loadMembers(groupId)

                checkIsOk(
                    response,
                    dataListSize(2),
                    groupHasAdmin(userA),
                    groupHasMember(userE)
                )
            }
        }

        describe("changes members role") {
            lateinit var groupId: String

            describe("$userA creates PUBLIC group with members $userB") {
                groupId = initGroup(userA, setOf(userB))
                userB.role = "member"
            }

            describe("$userB is member of group") {
                val response = userA.groups.loadMembers(groupId)
                checkIsOk(
                    response,
                    groupHasMember(userB)
                )
            }

            describe("$userA makes $userB a group administrator") {
                val response = userA.groups.changeRoleInGroup(groupId, userB.id, GroupsApi.ADMIN)

                describe("change role response") {
                    checkIsOk(response)

                    userB.role = "admin"

                }


                describe("checks $userB is admin") {
                    val membersResponse = userA.groups.loadMembers(groupId)
                    checkIsOk(
                        membersResponse,
                        groupHasAdmin(userA),
                        groupHasAdmin(userB)
                    )
                }
            }

            describe("$userB removes administrator rights from $userA") {
                val response = userB.groups.changeRoleInGroup(groupId, userA.id, GroupsApi.MEMBER)

                describe("change role response") {
                    checkIsOk(response)
                    userA.role = "member"
                }

                describe("check that $userA is member") {
                    val membersResponse = userA.groups.loadMembers(groupId)
                    checkIsOk(
                        membersResponse,
                        groupHasAdmin(userB),
                        groupHasMember(userA)
                    )
                }
            }

            describe("$userB can't remove administrator right if group doesn't have another administrators") {
                val response = userB.groups.changeRoleInGroup(groupId, userB.id, GroupsApi.MEMBER)

                checkIsBadRequest(response, errorCode = 109)
            }
        }

        describe("change properties") {
            lateinit var groupId: String
            groupId = initGroup(userA, members = setOf(userB))

            describe("admin can change properties") {
                val nextName = "abc1"
                val nextDescription = "edf1"

                describe("change response should return new data") {
                    val response = userA.groups.changeProperties(
                        groupId = groupId,
                        name = nextName,
                        description = nextDescription,
                        entryMode = GroupsApi.PRIVATE
                    )

                    checkIsOk(
                        response,
                        groupHasName(nextName),
                        groupHasDescription(nextDescription),
                        groupHasEntryMode(GroupsApi.PRIVATE)
                    )
                }

                describe("reload group contains new properties") {
                    val response = userA.groups.load(groupId)

                    checkIsOk(
                        response,
                        groupHasName(nextName),
                        groupHasDescription(nextDescription),
                        groupHasEntryMode(GroupsApi.PRIVATE)
                    )

                }

                describe("properties should be valid") {
                    describe("small values and incorrect logo") {

                        val response = userA.groups.changeProperties(
                            groupId = groupId,
                            name = "1",
                            description = "",
                            logo = "http://image.io",
                            entryMode = GroupsApi.PUBLIC
                        )

                        checkValidationErrors(
                            response,
                            ValidationError.leastCharacters(".name", 3),
                            ValidationError.leastCharacters(".description", 1),
                            ValidationError.mustBeBase64Image(".logo")
                        )
                    }

                    describe("big values") {
                        val logoPattern = "YWFh".repeat(40000) //aaa in base64

                        val response = userA.groups.changeProperties(
                            groupId = groupId,
                            name = "n".repeat(256),
                            description = "d".repeat(301),
                            logo = "data:image/png;base64,$logoPattern",
                            entryMode = GroupsApi.PUBLIC
                        )

                        checkValidationErrors(
                            response,
                            ValidationError.mostCharacters(".name", 255),
                            ValidationError.mostCharacters(".description", 300),
                            ValidationError.mostCharacters(".logo", 150000),
                        )
                    }
                }

            }
        }
    }
})

