package idel.tests.spec

import arrow.core.Some
import idel.tests.apiobject.*
import idel.tests.infrastructure.*
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import java.util.*

class GroupLoadingSpec : DescribeSpec({
    val entityStorage = EntityStorage()

    describe("filter by name") {
        val userA = User("userA", "group creator")
        val userB = User("userB")
        val userC = User("userC", "not member any group")

        val groupName_ArchitectsReports = "Architect's reports"
        val groupName_ThuesdayPub = "Pub on Thursday"
        val groupName_ReportsInPub = "reports in a pub"
        val groupName_ArchitectsBestPractices = "Architect's best practice in a PUB!!!"
        val groupName_PubOnFriday = "Pub on Friday"

        val userB_memberGroups = listOf(groupName_ArchitectsReports, groupName_ReportsInPub, groupName_ThuesdayPub)
        val userB_notMemberGroups = listOf(groupName_ArchitectsBestPractices, groupName_PubOnFriday)

        val allGroups = userB_memberGroups + userB_notMemberGroups

        val groupsInfo = mutableMapOf<String, GroupInfo>()


        describe("initialization") {
            describe("clear database") {
                entityStorage.clearAll()
                it("OK") {}
            }


            registerUsers(userA, userB, userC)


            allGroups.forEach {groupName ->
                describe("init group with name [$groupName]") {
                    describe("create group") {
                        val createGroupResponse = userA.groups.create(
                            name = groupName,
                            entryMode = GroupsApi.PUBLIC,
                        )
                        checkIsOk(createGroupResponse)
                        val groupInfo = GroupInfo(
                            groupId = createGroupResponse.extractId(),
                            joiningKey = createGroupResponse.extractField(GroupsApi.Fields.JOINING_KEY)
                        )
                        groupsInfo[groupName] = groupInfo
                    }
                }
            }

            userB_memberGroups.forEach {groupName ->
                describe("add userB to [$groupName]") {
                    val joiningKey = groupsInfo[groupName]!!.joiningKey

                    if (groupName != groupName_ArchitectsBestPractices) {
                        describe("userB join to group") {
                            val createJoinRequestResponse = userB.joinRequests.create(joiningKey)
                            checkIsOk(createJoinRequestResponse, joinRequestIsApproved)
                        }
                    }
                }
            }
        }


        describe("load by user") {
            describe("name=[arch] should return group [$groupName_ArchitectsReports] ") {
                val loadGroupsResponse = userB.groups.loadForUser(name = Some("arch"))
                checkIsOk(
                    loadGroupsResponse,
                    hasGroupsCount(1),
                    includeGroup(groupsInfo[groupName_ArchitectsReports]!!.groupId)
                )
            }

            listOf("pub", "PUB", "pUb").forEach {filter ->
                describe("$userB name=[$filter] should return groups [$groupName_ReportsInPub, $groupName_ThuesdayPub]") {
                    val loadGroupResponse = userB.groups.loadForUser(name = Some("pub"))
                    checkIsOk(
                        loadGroupResponse,
                        hasGroupsCount(2),
                        includeGroup(groupsInfo[groupName_ReportsInPub]!!.groupId),
                        includeGroup(groupsInfo[groupName_ThuesdayPub]!!.groupId),
                    )
                }
            }

            val randomName = UUID.randomUUID().toString()
            describe("$userB random name (uuid) should return nothing") {
                val loadGroupsResponse = userB.groups.loadForUser(name = Some(randomName))
                checkIsOk(loadGroupsResponse, noGroups())
            }
        }

        describe("load available") {
            describe("$userB name = [arch] should return one group $groupName_ArchitectsBestPractices") {
                val loadGroupsResponse = userB.groups.loadAvailable(name = Some("aRch"))
                checkIsOk(
                    loadGroupsResponse,
                    hasGroupsCount(1),
                    includeGroup(groupsInfo[groupName_ArchitectsBestPractices]!!.groupId)
                )
            }

            listOf("pub", "PUB", "pUb").forEach {filter ->
                describe("$userC name=[$filter] should return groups [$groupName_ArchitectsBestPractices, $groupName_PubOnFriday]") {
                    val loadGroupsResponse = userB.groups.loadAvailable(name = Some("pub"))
                    checkIsOk(
                        loadGroupsResponse,
                        hasGroupsCount(2),
                        includeGroup(groupsInfo[groupName_ArchitectsBestPractices]!!.groupId),
                        includeGroup(groupsInfo[groupName_PubOnFriday]!!.groupId),
                    )
                }

            }

            describe("can search minimum 3 symbols") {
                describe("load for user") {
                    describe("$userB get error for name = [a]") {
                        val loadGroupsResponse = userB.groups.loadForUser(name = Some("a"))
                        checkIsBadRequest(loadGroupsResponse, 100)
                    }

                    describe("$userC also get bad request, not access denied for name = [aa]") {
                        val loadGroupsResponse = userC.groups.loadForUser(name = Some("aa"))
                        checkIsBadRequest(loadGroupsResponse, 100)
                    }
                }
            }
        }
    }

    describe("domain restrictions") {

        val RESTRICTION_MAIL = arrayOf("mail")
        val RESTRICTION_EMAIL = arrayOf("email")
        val RESTRICTION_COMPANY = arrayOf("company")
        val RESTRICTION_MAIL_EMAIL = arrayOf("mail", "email")
        val RESTRICTION_EMPTY = emptyArray<String>()

        val userA = User("userA")
        val userB_mail = User("userB", domain = "mail")
        val userC_email = User("userC", domain = "email")
        val userE_post = User("userE", domain = "post")

        val allRestrictions = arrayOf(
            RESTRICTION_MAIL,
            RESTRICTION_EMAIL,
            RESTRICTION_COMPANY,
            RESTRICTION_MAIL_EMAIL,
            RESTRICTION_EMPTY
        )

        describe("load available") {
            describe("groups without users") {
                describe("initialization") {
                    describe("clear database") {
                        entityStorage.clearAll()
                        it("OK") {}
                    }

                    registerUsers(userA, userB_mail, userC_email, userE_post)

                    val groupsInfo = mutableMapOf<GroupParams, GroupInfo>()

                    describe("create groups") {
                        arrayOf(GroupsApi.PUBLIC, GroupsApi.CLOSED, GroupsApi.PRIVATE).forEach {entryMode ->
                            allRestrictions.forEach {domainRestrictions ->
                                val groupParams = GroupParams(entryMode, domainRestrictions)
                                val groupInfo = createGroup(
                                    groupAdmin = userA,
                                    members = emptySet(),
                                    entryMode = entryMode,
                                    domainRestrictions = domainRestrictions
                                )

                                groupsInfo[groupParams] = groupInfo
                            }
                        }
                    }

                    describe("load available") {

                        testAvailableGroup(
                            user = userB_mail,
                            groupsInfo = groupsInfo,
                            entryModes = arrayOf(GroupsApi.PUBLIC, GroupsApi.CLOSED),
                            domainRestrictions = arrayOf(RESTRICTION_MAIL, RESTRICTION_MAIL_EMAIL, RESTRICTION_EMPTY)
                        )

                        testAvailableGroup(
                            user = userC_email,
                            groupsInfo = groupsInfo,
                            entryModes = arrayOf(GroupsApi.PUBLIC, GroupsApi.CLOSED),
                            domainRestrictions = arrayOf(RESTRICTION_EMAIL, RESTRICTION_MAIL_EMAIL, RESTRICTION_EMPTY)
                        )

                        testAvailableGroup(
                            user = userE_post,
                            groupsInfo = groupsInfo,
                            entryModes = arrayOf(GroupsApi.PUBLIC, GroupsApi.CLOSED),
                            domainRestrictions = arrayOf(RESTRICTION_EMPTY)
                        )
                    }

                    describe("load") {
                        testLoadGroup(
                            user = userA, // creator can load regardless of domain restriction
                            canLoad = true,
                            isCreator = true,
                            groupsInfo = groupsInfo,
                            entryModes = arrayOf(GroupsApi.PUBLIC, GroupsApi.CLOSED, GroupsApi.PRIVATE),
                            domainRestrictions = allRestrictions
                        )

                        testLoadGroup(
                            user = userB_mail,
                            canLoad = true,
                            groupsInfo = groupsInfo,
                            entryModes = arrayOf(GroupsApi.PUBLIC, GroupsApi.CLOSED, GroupsApi.PRIVATE),
                            domainRestrictions = arrayOf(RESTRICTION_MAIL, RESTRICTION_MAIL_EMAIL, RESTRICTION_EMPTY)
                        )

                        testLoadGroup(
                            user = userB_mail,
                            canLoad = false,
                            groupsInfo = groupsInfo,
                            entryModes = arrayOf(GroupsApi.PUBLIC, GroupsApi.CLOSED, GroupsApi.PRIVATE),
                            domainRestrictions = arrayOf(RESTRICTION_EMAIL, RESTRICTION_COMPANY)
                        )

                        testLoadGroup(
                            user = userC_email,
                            canLoad = true,
                            groupsInfo = groupsInfo,
                            entryModes = arrayOf(GroupsApi.PUBLIC, GroupsApi.CLOSED, GroupsApi.PRIVATE),
                            domainRestrictions = arrayOf(RESTRICTION_EMAIL, RESTRICTION_MAIL_EMAIL, RESTRICTION_EMPTY)
                        )

                        testLoadGroup(
                            user = userC_email,
                            canLoad = false,
                            groupsInfo = groupsInfo,
                            entryModes = arrayOf(GroupsApi.PUBLIC, GroupsApi.CLOSED, GroupsApi.PRIVATE),
                            domainRestrictions = arrayOf(RESTRICTION_MAIL, RESTRICTION_COMPANY)
                        )

                        testLoadGroup(
                            user = userE_post,
                            canLoad = true,
                            groupsInfo = groupsInfo,
                            entryModes = arrayOf(GroupsApi.PUBLIC, GroupsApi.CLOSED, GroupsApi.PRIVATE),
                            domainRestrictions = arrayOf(RESTRICTION_EMPTY)
                        )

                        testLoadGroup(
                            user = userE_post,
                            canLoad = false,
                            groupsInfo = groupsInfo,
                            entryModes = arrayOf(GroupsApi.PUBLIC, GroupsApi.CLOSED, GroupsApi.PRIVATE),
                            domainRestrictions = arrayOf(RESTRICTION_MAIL, RESTRICTION_EMAIL, RESTRICTION_MAIL_EMAIL)
                        )


                    }

                }
            }

            describe("group with members") {
                val groupsInfo = mutableMapOf<GroupParams, GroupInfo>()

                describe("initialization") {
                    describe("clear database") {
                        entityStorage.clearAll()
                        it("OK") {}
                    }

                    describe("init users") {
                        registerUsers(userA, userB_mail, userC_email, userE_post)
                    }

                    describe("create groups") {
                        arrayOf(GroupsApi.PUBLIC, GroupsApi.CLOSED, GroupsApi.PRIVATE).forEach {entryMode ->
                            allRestrictions.forEach {domainRestrictions ->
                                val groupParams = GroupParams(entryMode, domainRestrictions)
                                val groupInfo = createGroup(
                                    groupAdmin = userA,
                                    members = emptySet(),
                                    entryMode = entryMode,
                                    domainRestrictions = domainRestrictions
                                )

                                groupsInfo[groupParams] = groupInfo
                            }
                        }
                    }

                    arrayOf(GroupsApi.PUBLIC, GroupsApi.CLOSED, GroupsApi.PRIVATE).forEach {entryMode ->
                        addUserToGroup(
                            groupsInfo = groupsInfo, admin = userA,
                            user = userB_mail,
                            groupParams = GroupParams(entryMode, RESTRICTION_MAIL)
                        )

                        addUserToGroup(
                            groupsInfo = groupsInfo, admin = userA,
                            user = userC_email,
                            groupParams = GroupParams(entryMode, RESTRICTION_MAIL_EMAIL),

                            )

                        addUserToGroup(
                            groupsInfo = groupsInfo, admin = userA,
                            user = userE_post,
                            groupParams = GroupParams(entryMode, RESTRICTION_EMPTY),
                        )
                    }
                }

                testAvailableGroup(
                    user = userB_mail,
                    groupsInfo = groupsInfo,
                    entryModes = arrayOf(GroupsApi.PUBLIC, GroupsApi.CLOSED),
                    domainRestrictions = arrayOf(RESTRICTION_MAIL_EMAIL, RESTRICTION_EMPTY)
                )

                testAvailableGroup(
                    user = userC_email,
                    groupsInfo = groupsInfo,
                    entryModes = arrayOf(GroupsApi.PUBLIC, GroupsApi.CLOSED),
                    domainRestrictions = arrayOf(RESTRICTION_EMAIL, RESTRICTION_EMPTY)
                )

                testAvailableGroup(
                    user = userE_post,
                    groupsInfo = groupsInfo,
                    entryModes = arrayOf(GroupsApi.PUBLIC, GroupsApi.CLOSED),
                    domainRestrictions = emptyArray()
                )
            }
        }
    }

    describe("attached joinrequests and invites") {
        /*
         *   |        | userB        | userC        |
         *   |--------|--------------|--------------|
         *   | group1 | join request | join request |
         *   | group2 | invite       |              |
         *   | group3 |              | invite       |
         *   | group4 | join request | invite       |
         */

        val userA = User("userA", "admin")
        val userB = User("userB")
        val userC = User("userC")

        lateinit var g1_id: String
        lateinit var g1_key: String
        lateinit var g2_id: String
        lateinit var g3_id: String

        lateinit var g4_id: String
        lateinit var g4_key: String

        lateinit var userB_g1_jr: String
        lateinit var userB_g2_invite: String
        lateinit var userB_g4_jr: String

        lateinit var userC_g1_jr: String
        lateinit var userC_g3_invite: String
        lateinit var userC_g4_invite: String

        describe("initialization") {
            describe("clear storage") {
                EntityStorage().clearAll()
            }

            registerUsers(userA, userB, userC)

            var r = createGroup(userA, members = emptySet(), name = "group 1", entryMode = GroupsApi.CLOSED)
            g1_id = r.groupId
            g1_key = r.joiningKey

            r = createGroup(userA, members = emptySet(), name = "group 2", entryMode = GroupsApi.CLOSED)
            g2_id = r.groupId

            r = createGroup(userA, members = emptySet(), name = "group 3", entryMode = GroupsApi.CLOSED)
            g3_id = r.groupId

            r = createGroup(userA, members = emptySet(), name = "group 4", entryMode = GroupsApi.CLOSED)
            g4_id = r.groupId
            g4_key = r.joiningKey

            describe("$userB creates joinRequests to group[$g1_id]") {
                userB_g1_jr = userB.joinRequests.create(g1_key).shouldBeOk().extractId("joinRequest")
            }

            describe("$userB invited to group[$g2_id]") {
                val response =
                    userA.invites.create(g2_id, registeredUsers = arrayOf(userB), newUsersEmails = emptyArray())
                        .shouldBeOk()
                userB_g2_invite = extractInviteId(userB, g2_id, response)
            }

            describe("$userC creates joinRequests to group[$g2_id]") {
                userC_g1_jr = userC.joinRequests.create(g1_key).shouldBeOk().extractId("joinRequest")
            }

            describe("$userC invited to group[$g3_id]") {
                val response =
                    userA.invites.create(g3_id, registeredUsers = arrayOf(userC), newUsersEmails = emptyArray())
                        .shouldBeOk()
                userC_g3_invite = extractInviteId(userC, g3_id, response)
            }

            describe("$userB creates joinRequest to group [$g4_id]") {
                userB_g4_jr = userB.joinRequests.create(g4_key).shouldBeOk().extractId("joinRequest")
            }

            describe("$userC invited to group [$g4_id]") {
                val response =
                    userA.invites.create(g4_id, registeredUsers = arrayOf(userC), newUsersEmails = emptyArray())
                        .shouldBeOk()
                userC_g4_invite = extractInviteId(userC, g4_id, response)
            }
        }

        describe("$userB load available") {
            val response = userB.groups.loadAvailable()
            val body = response.body()
            checkIsOk(
                response,
                includeGroup(g1_id, "group 1"),
                includeGroup(g2_id, "group 2"),
                includeGroup(g3_id, "group 3"),
                includeGroup(g4_id, "group 4"),
                hasJoinRequestsCount(2),
                hasInvitesCount(1)
            )

            describe("include a joinrequest for group 1 [$g1_id]") {
                includeJoinRequest(userB_g1_jr).check(body)
            }

            describe("include invite for group 2 [$g2_id]") {
                includeInvite(userB_g2_invite).check(body)
            }

            describe("include a join request for group 4 [$g4_id]") {
                includeJoinRequest(userB_g4_jr)
            }
        }

       describe("$userC load available") {
           val response = userC.groups.loadAvailable()
           val body = response.body()
           checkIsOk(response,
               includeGroup(g1_id, "group 1"),
               includeGroup(g2_id, "group 2"),
               includeGroup(g3_id, "group 3"),
               includeGroup(g4_id, "group 4"),
               hasJoinRequestsCount(1),
               hasInvitesCount(2)
           )

           describe("include a joinrequest for group 1 [$g1_id]") {
               includeJoinRequest(userC_g1_jr).check(body)
           }

           describe("include an invite for group 3 [$g3_id]") {
               includeInvite(userC_g3_invite).check(body)
           }

           describe("include an invite for group 4[$g4_id]") {
               includeInvite(userC_g4_invite).check(body)
           }


       }
    }
})

data class GroupParams(val entryMode: String, val domainsRestriction: Array<String>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GroupParams

        if (entryMode != other.entryMode) return false
        if (!domainsRestriction.contentEquals(other.domainsRestriction)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = entryMode.hashCode()
        result = 31 * result + domainsRestriction.contentHashCode()
        return result
    }

    fun msgString() = "entryMode = [$entryMode], restrictions = ${domainsRestriction.joinToString()}"
}

fun formatRestrictions(domainRestrictions: Array<out Array<String>>): String {
    return domainRestrictions.joinToString(prefix = "[", postfix = "]") {restriction ->
        restriction.joinToString(prefix = "[", postfix = "]")
    }
}

suspend fun DescribeSpecContainerScope.testAvailableGroup(
    user: User,
    groupsInfo: Map<GroupParams, GroupInfo>,
    entryModes: Array<String>,
    vararg domainRestrictions: Array<String>
) {
    val fmtRestrictions = formatRestrictions(domainRestrictions)

    describe(
        "$user should load available group with entryMode = [${GroupsApi.PUBLIC}, ${GroupsApi.CLOSED}], " +
                "restrictions: $fmtRestrictions} "
    ) {
        val loadGroupResponse = user.groups.loadAvailable()

        val totalGroupsExpected = entryModes.size * domainRestrictions.size

        if (totalGroupsExpected == 0) {
            checkIsOk(loadGroupResponse, noGroups())
        } else {
            checkIsOk(loadGroupResponse, hasGroupsCount(totalGroupsExpected))
            val responseBody = loadGroupResponse.body()
            entryModes.forEach {entryMode ->
                domainRestrictions.forEach {restriction ->
                    val groupParams = GroupParams(entryMode, restriction)
                    val groupId = groupsInfo[groupParams]!!.groupId

                    describe("includes groups $groupId, entryMode = ${entryMode}, restrictions = ${restriction.joinToString()}}") {
                        includeGroup(groupId).check(responseBody)
                    }
                }
            }
        }
    }
}

/**
 * Private group's is not available by id. Method check it.
 */
suspend fun DescribeSpecContainerScope.testLoadGroup(
    user: User,
    canLoad: Boolean,
    isCreator : Boolean = false,
    groupsInfo: Map<GroupParams, GroupInfo>,
    entryModes: Array<String>,
    vararg domainRestrictions: Array<String>
) {
    if (isCreator) require(canLoad) {"creator can load any group which he is created"}

    val fmtAccess = if (canLoad) {
        "can load"
    } else {
        "can't load"
    }

    entryModes.forEach {entryMode ->
        domainRestrictions.forEach {restrictions ->
            val groupParams = GroupParams(entryMode, restrictions)
            val groupInfo = groupsInfo[groupParams]!!

            describe("$user $fmtAccess group by id, ${groupParams.msgString()}") {
                val loadResponse = user.groups.load(groupInfo.groupId)
                if ((canLoad && (entryMode != GroupsApi.PRIVATE)) || isCreator) {
                    checkIsOk(loadResponse)
                } else {
                    checkIsNotFound(loadResponse)
                }
            }

            describe("$user $fmtAccess group by joiningKey, ${groupParams.msgString()}") {
                val loadResponse = user.groups.loadByLinkToJoin(groupInfo.joiningKey)
                if (canLoad) {
                    checkIsOk(loadResponse)
                } else {
                    checkIsNotFound(loadResponse)
                }
            }
        }
    }

}

suspend fun DescribeSpecContainerScope.addUserToGroup(
    groupsInfo: Map<GroupParams, GroupInfo>,
    groupParams: GroupParams,
    admin: User,
    user: User
) {
    describe("add $user to $groupParams") {
        val joiningKey = groupsInfo[groupParams]!!.joiningKey
        val joinRequestResponse = user.joinRequests.create(joiningKey)
        checkIsOk(joinRequestResponse)
        if (groupParams.entryMode != GroupsApi.PUBLIC) {
            val joinRequestId = extractJoinRequestId(joinRequestResponse)
            val changeStatusResponse = admin.joinRequests.changeStatus(joinRequestId, JoinRequestsApi.APPROVED)
            checkIsOk(changeStatusResponse, joinRequestIsApproved)
        }
    }
}