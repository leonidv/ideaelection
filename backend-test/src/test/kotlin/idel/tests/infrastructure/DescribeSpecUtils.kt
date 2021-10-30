package idel.tests.infrastructure

import idel.tests.apiobject.*
import io.kotest.core.spec.style.scopes.DescribeScope
import io.kotest.core.spec.style.scopes.DescribeSpecContainerContext
import java.time.LocalDateTime


suspend fun DescribeSpecContainerContext.registryUsers(vararg users: User) {
    describe("register users") {
        users.forEach {user ->
            it("register user [${user.name}]") {
                User.instanceAdmin.users.register(user, email = user.email).shouldBeOk()
            }
        }
    }
}

data class GroupInfo(
    val groupId: String,
    val joiningKey: String
)

suspend fun DescribeSpecContainerContext.createGroup(
    groupAdmin: User,
    members: Set<User>,
    entryMode: String = GroupsApi.PUBLIC,
    domainRestrictions: Array<String> = emptyArray(),
    name: String? = null
): GroupInfo {

    lateinit var groupId: String
    lateinit var joiningKey: String

    val domainRestrictionLabel = if (domainRestrictions.isNotEmpty()) {
        " domainRestrictions = [${domainRestrictions.joinToString()}]"
    } else {
        ""
    }

    val nameFmt = name?.let {"[$name]"}

    describe("$groupAdmin creates group${nameFmt?:""}. entryMode = [$entryMode]$domainRestrictionLabel") {
        val createGroupResponse = groupAdmin.groups.create(
            name = name?:"created from test ${LocalDateTime.now()}",
            entryMode = entryMode,
            domainRestrictions = domainRestrictions
        )

        checkIsOk(createGroupResponse)

        groupId = createGroupResponse.extractId()
        joiningKey = createGroupResponse.extractField(GroupsApi.Fields.JOINING_KEY)
    }

    members.forEach {user ->
        describe("$user join to group") {
            val joinRequestResponse = user.joinRequests.create(joiningKey)

            if (entryMode == GroupsApi.PRIVATE || entryMode == GroupsApi.CLOSED) {
                describe("join request is unresolved") {
                    checkIsOk(joinRequestResponse, joinRequestIsUnresolved)
                }

                describe("admin approve join request") {
                    val joinRequestId = extractJoinRequestId(joinRequestResponse)
                    val approveRequest = groupAdmin.joinRequests.changeStatus(joinRequestId, JoinRequestsApi.APPROVED)
                    checkIsOk(approveRequest, joinRequestIsApproved)
                }
            } else {
                describe("join request is automatically approved") {
                    checkIsOk(joinRequestResponse, joinRequestIsApproved)
                }
            }
        }
    }

    return GroupInfo(groupId, joiningKey)
}


