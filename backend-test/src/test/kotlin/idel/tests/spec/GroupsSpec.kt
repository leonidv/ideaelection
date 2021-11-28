package idel.tests.spec

import arrow.core.Option
import arrow.core.Some
import idel.tests.apiobject.*
import idel.tests.infrastructure.*

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import io.kotest.matchers.shouldNotBe
import java.util.*

@Suppress("JoinDeclarationAndAssignment")
class GroupsSpec : DescribeSpec({

    val couchbase = EntityStorage()
    beforeSpec {
        couchbase.clearAll()
    }

    val userA = User("userA", "group creator")
    val userB = User("userB")
    val userC = User("userC")
    val userE = User("userE")
    val userD = User("userD", "not member")

    context("$userA, $userB, $userC, $userE, $userD user is registered in the app") {
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
                    val entryQuestion = "question $entryMode"
                    val response = userA.groups.create(
                        name = name,
                        entryMode = entryMode,
                        entryQuestion = entryQuestion,
                        description = description,
                        domainRestrictions = arrayOf("gmail.com", "facebook.com")
                    )

                    checkIsOk(
                        response,
                        groupHasName(name),
                        groupHasEntryMode(entryMode),
                        groupHasCreator(userA),
                        groupHasDescription(description),
                        groupHasMembersCount(1),
                        groupHasIdeasCount(0)
                    )
                }
            }
        }

        describe("loading members") {
            lateinit var groupId: String

            describe("$userA creates PUBLIC group with members $userB, $userC") {
                groupId = createGroup(userA, setOf(userB, userC)).groupId
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
                    first = Some("0"),
                    last = Some("1")
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
                    first = Some("1"),
                    last = Some("2")
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
                    usernameFilter = Some("user")
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
                    usernameFilter = Some("erB")
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
                    usernameFilter = Some(UUID.randomUUID().toString())
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
                groupId = createGroup(userA, setOf(userB, userC, userE)).groupId
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

            describe("$userA can leave group, because he is last admin") {
                val response = userA.groups.deleteMember(groupId, userA.id)
                checkIsBadRequest(response, 109)
            }
        }

        describe("changes members role") {
            lateinit var groupId: String

            describe("$userA creates PUBLIC group with members $userB") {
                groupId = createGroup(userA, setOf(userB)).groupId
                userA.role = "admin"
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
                describe("change role response") {
                    val response = userA.groups.changeRoleInGroup(groupId, userB.id, GroupsApi.ADMIN)
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

                checkIsBadRequest(response, 109)
            }
        }

        describe("change properties") {
            lateinit var groupId: String
            groupId = createGroup(userA, members = setOf(userB)).groupId

            describe("admin can change properties") {
                val nextName = "abc1"
                val nextDescription = "edf1"
                val nextEntryQuestion = "neq2"
                val nextDomainRestrictions = arrayOf(userA.domain, "dom.ai", "company.com")

                describe("change response should return new data") {
                    val response = userA.groups.changeProperties(
                        groupId = groupId,
                        name = nextName,
                        description = nextDescription,
                        entryMode = GroupsApi.PRIVATE,
                        entryQuestion = nextEntryQuestion,
                        domainRestrictions = nextDomainRestrictions
                    )

                    checkIsOk(
                        response,
                        groupHasName(nextName),
                        groupHasDescription(nextDescription),
                        groupHasEntryMode(GroupsApi.PRIVATE),
                        groupHasEntryQuestion(nextEntryQuestion),
                        groupHasDomainRestrictionsCount(nextDomainRestrictions.size),
                        groupHasDomainRestriction(nextDomainRestrictions[0]),
                        groupHasDomainRestriction(nextDomainRestrictions[1])
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
                            entryMode = GroupsApi.PUBLIC,
                            domainRestrictions = arrayOf(),
                            entryQuestion = "ok"
                        )

                        checkValidationErrors(
                            response,
                            ValidationError.leastCharacters(".name", 3),
                            ValidationError.leastCharacters(".description", 1),
                            ValidationError.mustBeBase64Image(".logo")
                        )
                    }

                    describe("big values") {
                        val logoPattern = "YWFh".repeat(40000) //YWFh is aaa in base64

                        val response = userA.groups.changeProperties(
                            groupId = groupId,
                            name = "n".repeat(256),
                            description = "d".repeat(301),
                            logo = "data:image/png;base64,$logoPattern",
                            entryMode = GroupsApi.PUBLIC,
                            domainRestrictions = arrayOf(),
                            entryQuestion = "q".repeat(201)
                        )

                        checkValidationErrors(
                            response,
                            ValidationError.mostCharacters(".name", 255),
                            ValidationError.mostCharacters(".description", 300),
                            ValidationError.mostCharacters(".logo", 150000),
                            ValidationError.mostCharacters(".entryQuestion", 200)
                        )
                    }
                }

            }
        }

        describe("joining key") {
            lateinit var groupId: String
            val groupInfo = createGroup(userA, members = setOf(userB))
            userB.role = "member"
            userC.role = "not member"

            groupId = groupInfo.groupId
            val initialJoiningKey = groupInfo.joiningKey

            describe("is changed") {
                val regenerateResponse = userA.groups.regenerateJoiningKey(groupId)
                checkIsOk(regenerateResponse)
                val changedJoiningKey = regenerateResponse.extractField(GroupsApi.Fields.JOINING_KEY)
                describe("response of regenerate contains new joiningKey") {
                    it("response joining key is different from original") {
                        changedJoiningKey.shouldNotBe(initialJoiningKey)
                    }
                }

                describe("response of load contains new joiningKey") {
                    val loadResponse = userA.groups.load(groupId)

                    checkIsOk(loadResponse, groupHasJoiningKey(changedJoiningKey))
                }

                describe("$userC can load group info by changed joiningKey") {
                    val loadResponse = userC.groups.loadByLinkToJoin(changedJoiningKey)
                    checkIsOk(loadResponse, entityIdIs(groupId))
                }

                describe("$userC can't load group info by original joiningKey") {
                    val loadResponse = userC.groups.loadByLinkToJoin(initialJoiningKey)
                    checkIsNotFound(loadResponse)
                }

            }

            describe("security checks") {
                describe("$userA can regeneration joiningKey") {
                    val response = userA.groups.regenerateJoiningKey(groupId)
                    checkIsOk(response)
                }

                describe("$userB can't regenerate joiningKey") {
                    val response = userB.groups.regenerateJoiningKey(groupId)
                    checkIsForbidden(response)
                }

                describe("$userC can't regenerate joiningKey") {
                    val response = userC.groups.regenerateJoiningKey(groupId)
                    checkIsForbidden(response)
                }
            }
        }

        describe("delete group") {
            lateinit var groupId: String
            lateinit var ideaId: String

            describe("init group with idea") {

                groupId = createGroup(userA, members = setOf(userB)).groupId
                userE.role = "non member"

                val addIdeaResponse = userA.ideas.quickAdd(groupId, "1")
                ideaId = addIdeaResponse.extractId("idea")

            }

            describe("security") {
                describe("$userB can't delete group") {
                    checkIsForbidden(userB.groups.delete(groupId))
                }
            }

            describe("operation on delete group is not allowed") {
                describe("$userA delete group") {
                    val response = userA.groups.delete(groupId)

                    checkIsOk(response, groupHasStateDeleted)
                }


                arrayOf(userA, userB).forEach {user ->
                    describe("$user don't see group in user's groups list") {
                        val response = user.groups.loadForUser(user.id)
                        checkIsOk(response, notIncludeGroup(groupId))
                    }
                }

                arrayOf(userA, userB, userE).forEach {user ->
                    describe("$user don't see group in the available groups ") {
                        val response = user.groups.loadAvailable()
                        checkIsOk(response, notIncludeGroup(groupId))
                    }
                }

                arrayOf(userA, userB).forEach {user ->
                    describe("$user can't list groups ideas") {
                        val response = user.ideas.list(groupId)
                        checkIsNotFound(response)
                    }
                }

                arrayOf(userA, userB).forEach {user ->
                    describe("$user can't load idea from deleted group by id") {
                        val response = user.ideas.load(ideaId)
                        checkIsNotFound(response)
                    }
                }

                arrayOf(userA, userB).forEach {user ->
                    describe("$user can't add idea to deleted group") {
                        val response = user.ideas.quickAdd(groupId, "-1")
                        checkIsNotFound(response)
                    }
                }

                describe("$userA can't change group properties") {
                    val response =
                        userA.groups.changeProperties(
                            groupId,
                            name = "next name",
                            description = "next description",
                            entryMode = GroupsApi.PUBLIC,
                            entryQuestion = "some new question",
                            domainRestrictions = arrayOf()
                        )
                    checkIsNotFound(response)
                }

                describe("$userA can't update idea in group") {
                    val response = userA.ideas.quickEdit(ideaId, "-1")
                    checkIsNotFound(response)
                }

                arrayOf(userA, userB).forEach {user ->
                    describe("$user can't vote for idea") {
                        val response = userA.ideas.vote(ideaId)
                        checkIsNotFound(response)
                    }
                }
            }
        }
    }


})

