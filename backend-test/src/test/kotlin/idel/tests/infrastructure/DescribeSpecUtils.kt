package idel.tests.infrastructure

import idel.tests.apiobject.*
import io.kotest.core.spec.style.scopes.DescribeScope


suspend fun DescribeScope.registryUsers(vararg users: User) {
    describe("register users") {
        users.forEach {user ->
            it("register user [${user.name}]") {
                User.instanceAdmin.users.register(user.name).shouldBeOk()
            }
        }
    }
}

data class GroupInfo(
    val groupId : String,
    val joiningKey : String
)

suspend fun DescribeScope.createGroup(
    groupAdmin: User,
    members: Set<User>,
    entryMode: String = GroupsApi.PUBLIC
): GroupInfo {

    lateinit var groupId: String
    lateinit var joiningKey: String


    describe("$groupAdmin creates group with entryMode = [$entryMode]") {
        val createGroupResponse = groupAdmin.groups.create("assignee spec group", entryMode)

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
                    var joinRequestId = joinRequestResponse.extractId()
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


