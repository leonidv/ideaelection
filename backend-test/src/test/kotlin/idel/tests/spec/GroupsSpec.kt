package idel.tests.spec

import idel.tests.*
import idel.tests.apiobject.GroupsApi
import idel.tests.infrastructure.JsonNodeExtensions.querySet
import idel.tests.infrastructure.asUserId
import idel.tests.infrastructure.extractData
import io.kotest.assertions.arrow.option.shouldBeSome

import io.kotest.assertions.asClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table

class GroupsSpec : DescribeSpec({

    val groupApi = GroupsApi("userA")

    describe("positive scenarios") {
        describe("create a new group") {
            describe("add new group") {
                val response = groupApi.create(
                        title = "123",
                        entryMode = GroupsApi.PUBLIC,
                        description = "234",
                        admins = setOf("userB".asUserId(), "userC".asUserId())
                )

                val data = extractData(response)
                data.toPrettyString().asClue {
                    it("has id") {
                        data.shouldHasPath("$.id")
                    }

                    it("has title from request") {
                        data.shouldContains("$.title", "123")
                    }

                    it("has description from request") {
                        data.shouldContains("$.description", "234")
                    }

                    it("has entry mode from request") {
                        data.shouldContains("$.entryMode", GroupsApi.PUBLIC)
                    }

                    it("has admins from request and creator") {
                        data.querySet("$.administrators")
                            .shouldBeSome(setOf("userA".asUserId(), "userB".asUserId(), "userC".asUserId()))
                    }
                }
            }

            table(
                    headers("entry mode"),
                    row(GroupsApi.PUBLIC),
                    row(GroupsApi.CLOSED),
                    row(GroupsApi.PRIVATE)
            ).forAll {entryMode ->
                describe("group with entry mode $entryMode") {
                    val response = groupApi.create(
                            title = "test $entryMode",
                            entryMode = entryMode,
                    )

                    val data = extractData(response)

                    data.toPrettyString().asClue {
                        it("has entry mode $entryMode") {
                            data.shouldContains("$.entryMode", entryMode)
                        }
                    }
                }
            }
        }
    }
})

