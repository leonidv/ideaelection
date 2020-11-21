package idel.tests.scenario

import idel.tests.apiobject.Couchbase
import idel.tests.apiobject.GroupsApi
import idel.tests.apiobject.User
import idel.tests.isOk
import idel.tests.statusIs
import io.kotest.core.spec.style.DescribeSpec

class JoinToGroupScenarios : DescribeSpec({
    beforeSpec {
        Couchbase().clear()
    }

    context("empty installation without any groups and users") {
        val userAdmin = User("userAdmin")
        val userA = User("userA")
        val userB = User("userB")
        val userC = User("userC")

        describe("register all users") {
            listOf(userA, userB,userC).forEach{user ->
                it("register user ${user.name}") {
                    userAdmin.users.register(user.name).isOk()
                }
            }
        }

        describe("join to public group") {
            it("userA adds new public group without admins and members") {
                userA.groups.create("public group", GroupsApi.PUBLIC).isOk()
            }

            it("userB see group is list of available") {
                //userB.groups.
            }
        }

    }
})