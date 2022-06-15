@file:Suppress("JoinDeclarationAndAssignment")

package idel.tests.spec

import idel.tests.apiobject.*
import idel.tests.infrastructure.*
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import java.util.*

class JoinRequestsSpec : DescribeSpec({

    beforeSpec {
        EntityStorage().clearAll()
    }

    val userA = User("userA", "group creator")
    val userB = User("userB")

    describe("register users [userA, userB]") {
        registerUsers(userA, userB)
    }

    describe("negative scenarios") {
        describe("joining key is not exists") {
            val response = userA.joinRequests.create(UUID.randomUUID().toString())

            it("should be 102 error (entity not exists)") {
                response.shouldBeError(102)
            }
        }
    }


    describe("positive scenarios") {
        describe("creating join requests to public group") {
            describe("basic checks") {
                lateinit var groupId: String
                lateinit var joiningKey: String
                val groupInfo = createGroup(userA, entryMode = GroupsApi.PUBLIC, members = setOf())
                groupId = groupInfo.groupId
                joiningKey = groupInfo.joiningKey

                val response = userB.joinRequests.create(joiningKey = joiningKey, message = "msg")

                checkIsOk(
                    response,
                    joinRequestHasGroupId(groupId),
                    joinRequestHasUserId(userB.id),
                    joinRequestIsApproved,
                    joinRequestHasMessage("msg")
                )

            }
        }

        describe("join requests status depends on groups entry mode") {
            table(
                headers("entryMode", "joinRequestStatus"),
                row(GroupsApi.PUBLIC, JoinRequestsApi.APPROVED),
                row(GroupsApi.CLOSED, JoinRequestsApi.UNRESOLVED),
                row(GroupsApi.PRIVATE, JoinRequestsApi.UNRESOLVED)
            ).forAll {entryMode: String, status: String ->
                describe("for $entryMode group status should be $status") {
                    lateinit var joiningKey: String
                    joiningKey = createGroup(userA, entryMode = entryMode, members = setOf()).joiningKey

                    val response = userB.joinRequests.create(joiningKey)
                    checkIsOk(
                        response,
                        joinRequestHasStatus(status)
                    )
                }
            }
        }

        describe("delete a joinRequest") {
            listOf(GroupsApi.CLOSED, GroupsApi.PRIVATE) .forEach {entryMode ->
                describe("from a $entryMode group") {

                    lateinit var joinRequestId: String

                    describe("create a joinRequest") {
                        val joiningKey = createGroup(userA, entryMode = entryMode, members = setOf()).joiningKey
                        val response = userB.joinRequests.create(joiningKey)
                        checkIsOk(response)
                        joinRequestId = extractJoinRequestId(response)
                    }

                    describe("$userB see joinRequest in his joinRequests") {
                        val response = userB.joinRequests.loadForUser()
                        checkIsOk(response, includeJoinRequest(joinRequestId))
                    }

                    describe("$userB delete him joinRequest") {
                        val response = userB.joinRequests.delete(joinRequestId)
                        checkIsOk(response)
                    }

                    describe("$userB don't see joinRequest in his joinRequests") {
                        val response = userB.joinRequests.loadForUser()
                        checkIsOk(
                            response,
                            notIncludeJoinRequest(joinRequestId)
                        )
                    }
                }
            }
        }

        xdescribe("user already in group") {
            // should ignore request and sent approved
        }
    }



    describe("security") {
        describe("$userB can't create join request for group with domain = [company]") {
            arrayOf(GroupsApi.PUBLIC, GroupsApi.CLOSED, GroupsApi.PRIVATE).forEach {entryMode ->
                val groupInfo = createGroup(
                    groupAdmin = userA,
                    entryMode = entryMode,
                    members = setOf(),
                    domainRestrictions = arrayOf("company")
                )

                describe("$userB can't create join request to $entryMode") {
                    val joinRequestResponse = userB.joinRequests.create(groupInfo.joiningKey)
                    checkIsNotFound(joinRequestResponse)
                }
            }
        }
    }
})