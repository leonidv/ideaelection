package idel.tests.infrastructure

import arrow.core.Some
import idel.tests.*
import idel.tests.apiobject.GroupsApi
import idel.tests.apiobject.User
import idel.tests.apiobject.joinRequestIsApproved
import idel.tests.infrastructure.JsonNodeExtensions.dataId
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

suspend fun DescribeScope.initGroup(groupAdmin: User, members: Set<User>, entryMode : String = GroupsApi.PUBLIC): String {

    lateinit var groupId: String

    describe("$groupAdmin creates the public group") {
        val response = groupAdmin.groups.create("assignee spec group", entryMode)

        checkIsOk(response)

        groupId = (response.body().dataId() as Some).t
    }

    members.forEach {user ->
        describe("$user join to group") {
            checkIsOk(user.joinRequests.create(groupId), joinRequestIsApproved)
        }
    }

    return groupId
}

