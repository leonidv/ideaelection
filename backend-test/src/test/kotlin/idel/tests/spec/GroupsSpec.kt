package idel.tests.spec

import idel.tests.*
import idel.tests.apiobject.*
import idel.tests.infrastructure.*

import io.kotest.assertions.asClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table

class GroupsSpec : DescribeSpec({

    val couchbase = Couchbase()
    beforeSpec {
        couchbase.clearAll()
    }

    val userAdmin = User("userA", "creator")
    val userB = User("userB", "admin")
    val userC = User("userC", "admin")
    val userE = User("userE", "member")
    val userD = User("userD", "not member")

    context("userA, userB, userC is registered in the app") {
        describe("initialization") {
            registryUsers(userAdmin, userB, userC, userE, userD)
        }

        describe("positive scenarios") {
            describe("create a new group") {
                describe("add new group") {
                    val response = userAdmin.groups.create(
                        name = "123",
                        entryMode = GroupsApi.PUBLIC,
                        description = "234",
                        admins = setOf(userB.id, userC.id)
                    )

                    checkIsOk(
                        response,
                        groupHasName("123"),
                        groupHasEntryMode(GroupsApi.PUBLIC),
                        groupHasDescription("234"),
                        groupHasAdmin(userAdmin),
                        groupHasAdmin(userB),
                        groupHasAdmin(userC)
                    )
                }

                table(
                    headers("entry mode"),
                    row(GroupsApi.PUBLIC),
                    row(GroupsApi.CLOSED),
                    row(GroupsApi.PRIVATE)
                ).forAll {entryMode ->
                    describe("group with entry mode $entryMode") {
                        val response = userAdmin.groups.create(
                            name = "test $entryMode",
                            entryMode = entryMode,
                        )

                        checkIsOk(
                            response,
                            groupHasEntryMode(entryMode)
                        )
                    }
                }
            }

        }
    }
})

