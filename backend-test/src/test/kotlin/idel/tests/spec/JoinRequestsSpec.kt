@file:Suppress("JoinDeclarationAndAssignment")

package idel.tests.spec

import idel.tests.apiobject.*
import idel.tests.infrastructure.*
import idel.tests.shouldHasPath
import idel.tests.shouldContains
import idel.tests.shouldBeError
import io.kotest.assertions.asClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import java.util.*

class JoinRequestsSpec : DescribeSpec({

    beforeSpec {
        Couchbase().clearAll()
    }

    val userA = User("userA", "group creator")
    val userB = User("userB")

    describe("register users [userA, userB]") {
        registryUsers(userA, userB)
    }

    describe("negative scenarios") {
        describe("group id is not exists") {
            val response = userA.joinRequests.create(UUID.randomUUID().toString())

            it("should be 102 error (entity not exists)") {
                response.shouldBeError(102)
            }
        }
    }


    describe("positive scenarios") {
        describe("creating join requests to public group") {
            describe("basic checks") {
                lateinit var groupId : String
                groupId = initGroup(userA, entryMode = GroupsApi.PUBLIC, members = setOf())

                val response  = userB.joinRequests.create(groupId = groupId, message = "msg")

                checkIsOk(
                    response,
                    hasId,
                    joinRequestHasGroupId(groupId),
                    joinRequestHasUserId(userB.id),
                    joinRequestIsApproved,
                    joinRequestHasMessage("msg")
                )

            }

            describe("join requests status depends on groups entry mode") {
                table(
                    headers("entryMode", "joinRequestStatus"),
                    row(GroupsApi.PUBLIC, JoinRequestsApi.APPROVED),
                    row(GroupsApi.CLOSED, JoinRequestsApi.UNRESOLVED),
                    row(GroupsApi.PRIVATE, JoinRequestsApi.UNRESOLVED)
                ).forAll {entryMode: String, status: String ->
                    describe("for $entryMode group status should be $status") {
                        lateinit var groupId : String
                        groupId = initGroup(userA, entryMode = entryMode, members = setOf())

                        val response = userB.joinRequests.create(groupId)
                        checkIsOk(
                            response,
                            joinRequestHasStatus(status)
                        )
                    }
                }
            }

            xdescribe("user already in group") {
                // should ignore request and sent approved
            }
        }
    }
})