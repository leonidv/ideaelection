package idel.tests.spec

import idel.tests.apiobject.UserManagementApi
import idel.tests.checkEntityData
import idel.tests.checkError
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import io.kotest.matchers.maps.shouldContain
import java.util.*

class UsersManagement : DescribeSpec({
    val api = UserManagementApi()

    fun genName(): String = UUID.randomUUID().toString().replace("-", "")

    xdescribe("register user") {
        describe("positive scenarios") {
            describe("may create with all roles") {
                table(
                        headers("roles"),
                        row(listOf("ROLE_USER")),
                        row(listOf("ROLE_SUPER_USER")),
                        row(listOf("ROLE_USER", "ROLE_SUPER_USER"))
                ).forAll {roles ->
                    val name = genName()
                    describe("roles is $roles") {

                        val r = api.registerUser(name, roles)

                        val rLoad = api.load("admin__super_user", name)

                        context("registration response") {
                            val data = checkEntityData(this, r)
                            it("new user id is [$name@httpbasic]") {
                                data.shouldContain("id", "$name@httpbasic")
                            }

                            it("new user roles is $roles") {
                                data.shouldContain("roles", roles)
                            }
                        }

                        context("loading response") {
                            val data = checkEntityData(this, rLoad)
                            it("new user id is [$name@httpbasic]") {
                                data.shouldContain("id", "$name@httpbasic")
                            }

                            it("new user roles is $roles") {
                                data.shouldContain("roles", roles)
                            }
                        }
                    }
                }
            }
        }

        xdescribe("negative scenarios") {
            describe("id already exists") {
                val name = genName()

                val rOk = api.registerUser(name)
                context("first registration is ok") {
                    checkEntityData(this, rOk)
                }

                val rFail = api.registerUser(name)
                context("second registration is failed with code 100") {
                    checkError(this, rFail, 100)
                }

            }

            describe("incorrect role") {
                val name = genName()
                val r = api.registerUser(name, roles = listOf("ROLE_GOD"))

                checkError(this, r, 100)
            }
        }


    }

    describe("changes user roles") {
        describe("positive scenarios") {
            table(
                    headers("current roles", "next roles"),
                    row(listOf("ROLE_USER"), listOf("ROLE_SUPER_USER")),
                    row(listOf("ROLE_USER"), listOf()),
                    row(listOf("ROLE_SUPER_USER"), listOf("ROLE_USER")),
                    row(listOf("ROLE_SUPER_USER"), listOf()),
                    row(listOf("ROLE_USER", "ROLE_SUPER_USER"), listOf("ROLE_USER")),
                    row(listOf("ROLE_USER", "ROLE_SUPER_USER"), listOf("ROLE_SUPER_USER")),
                    row(listOf("ROLE_USER", "ROLE_SUPER_USER"), listOf())
            ).forAll {currentRoles, newRoles ->
                describe("from $currentRoles tp $newRoles") {

                    val name = genName()
                    val rRegistration = api.registerUser(name, currentRoles)
                    val rChangeRoles = api.changeRoles("admin__super_user", name, newRoles)
                    val rLoad = api.load("admin__super_user", name)

                    context("registration response") {
                        val data = checkEntityData(this, rRegistration)

                        it("has roles $currentRoles") {
                            data.shouldContain("roles", currentRoles)
                        }
                    }
                    
                    context("changes roles response") {
                        val data = checkEntityData(this, rChangeRoles)
                        it("has roles $newRoles") {
                            data.shouldContain("roles", newRoles)
                        }
                    }

                    context("load user response") {
                        val data = checkEntityData(this, rLoad)
                        it("has roles $newRoles") {
                            data.shouldContain("roles", newRoles)
                        }
                    }

                }
            }

        }
    }
})