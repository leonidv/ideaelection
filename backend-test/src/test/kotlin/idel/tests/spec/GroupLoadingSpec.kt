package idel.tests.spec

import arrow.core.Some
import idel.tests.apiobject.*
import idel.tests.infrastructure.*
import idel.tests.infrastructure.GroupInfo
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.spec.style.scopes.DescribeSpecContainerContext
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

            describe("init users") {
                registryUsers(userA, userB, userC)
            }

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
                    dataListSize(1),
                    includeGroup(groupsInfo[groupName_ArchitectsReports]!!.groupId)
                )
            }

            listOf("pub", "PUB", "pUb").forEach {filter ->
                describe("$userB name=[$filter] should return groups [$groupName_ReportsInPub, $groupName_ThuesdayPub]") {
                    val loadGroupResponse = userB.groups.loadForUser(name = Some("pub"))
                    checkIsOk(
                        loadGroupResponse,
                        dataListSize(2),
                        includeGroup(groupsInfo[groupName_ReportsInPub]!!.groupId),
                        includeGroup(groupsInfo[groupName_ThuesdayPub]!!.groupId),
                    )
                }
            }

            val randomName = UUID.randomUUID().toString();
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
                    dataListSize(1),
                    includeGroup(groupsInfo[groupName_ArchitectsBestPractices]!!.groupId)
                )
            }

            listOf("pub", "PUB", "pUb").forEach {filter ->
                describe("$userC name=[$filter] should return groups [$groupName_ArchitectsBestPractices, $groupName_PubOnFriday]") {
                    val loadGroupsResponse = userB.groups.loadAvailable(name = Some("pub"))
                    checkIsOk(
                        loadGroupsResponse,
                        dataListSize(2),
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

        val allRestrictions = listOf(
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

                    describe("init users") {
                        registryUsers(userA, userB_mail, userC_email, userE_post)
                    }


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

                    describe("load by joining key") {
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
                        registryUsers(userA, userB_mail, userC_email, userE_post)
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
        restriction.joinToString(prefix = "[", postfix = "]");
    }
}

suspend fun DescribeSpecContainerContext.testAvailableGroup(
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
            checkIsOk(loadGroupResponse, dataListSize(totalGroupsExpected))

            entryModes.forEach {entryMode ->
                domainRestrictions.forEach {restriction ->
                    val groupParams = GroupParams(entryMode, restriction)
                    val groupId = groupsInfo[groupParams]!!.groupId

                    val checker = BodyContainsObject(
                        testName = "includes groups $groupId, entryMode = ${entryMode}, restrictions = ${restriction.joinToString()}}",
                        objectPath = "$.data",
                        fields = arrayOf(Pair("id", groupId))
                    )

                    it(checker.testName) {
                        checker.check(loadGroupResponse.body())
                    }
                }
            }
        }
    }
}

/**
 * Private group's is not available by id. Method check it.
 */
suspend fun DescribeSpecContainerContext.testLoadGroup(
    user: User,
    canLoad: Boolean,
    groupsInfo: Map<GroupParams, GroupInfo>,
    entryModes: Array<String>,
    vararg domainRestrictions: Array<String>
) {
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
                if (canLoad && (entryMode != GroupsApi.PRIVATE)) {
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

suspend fun DescribeSpecContainerContext.addUserToGroup(
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