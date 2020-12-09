package idel.tests.spec

import idel.tests.apiobject.GroupsApi
import idel.tests.apiobject.JoinRequestsApi
import idel.tests.apiobject.User
import idel.tests.shouldHasPath
import idel.tests.shouldContains
import idel.tests.infrastructure.extractData
import idel.tests.infrastructure.extractId
import idel.tests.shouldBeError
import io.kotest.assertions.asClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import java.util.*

class JoinRequestsSpec : DescribeSpec({
    val userA = User("userA")

    describe("negative scenarios") {
        describe("group id is not exists") {
            val response = userA.joinRequests.create(UUID.randomUUID().toString())

            it("should be 105 error") {
                response.shouldBeError(105)
            }
        }
    }


    describe("positive scenarios") {
        describe("creating join requests") {
            describe("basic checks") {
                val createGroupsResponse = userA.groups.create("userA public group", entryMode = GroupsApi.PUBLIC)
                val groupId = extractId(createGroupsResponse)

                val createJoinRequestsResponse = userA.joinRequests.create(groupId)
                val data = extractData(createJoinRequestsResponse)

                data.toPrettyString().asClue {
                    it("has id") {
                        data.shouldHasPath("$.id")
                    }

                    it("has group from request") {
                        data.shouldContains("$.groupId", groupId)
                    }

                    it("has user from request") {
                        data.shouldContains("$.userId", userA.id)
                    }

                    it("has creation time") {
                        data.shouldHasPath("$.ctime")
                    }

                    it("is approved") {
                        data.shouldContains("$.status", JoinRequestsApi.APPROVED)
                    }

                }
            }

            describe("join requests status depends on groups entry mode") {
                table(
                        headers("entryMode", "joinRequestStatus"),
                        row(GroupsApi.PUBLIC, JoinRequestsApi.APPROVED),
                        row(GroupsApi.CLOSED, JoinRequestsApi.UNRESOLVED),
                        row(GroupsApi.PRIVATE, JoinRequestsApi.UNRESOLVED)
                ).forAll {entryMode : String, status : String ->
                    describe("for $entryMode group status should be $status") {

                        val createGroupsResponse = userA.groups.create("userA public group", entryMode = entryMode)
                        val groupId = extractId(createGroupsResponse)

                        val createJoinRequestsResponse = userA.joinRequests.create(groupId)
                        val data = extractData(createJoinRequestsResponse)

                        it("status is $status") {
                            data.shouldContains("$.status",status)
                        }
                    }
                }
            }

            xdescribe("user already in group"){
                // should ignore request and sent approved
            }
        }
    }
})