package idel.tests.spec

import com.typesafe.config.ConfigFactory
import idel.tests.checkEntityData
import idel.tests.checkError
import idel.tests.initRequest
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import io.kotest.matchers.maps.shouldContain
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.When
import io.restassured.response.Response
import java.util.*

class UserManagementApi(val usersEndpointUrl: String = ConfigFactory.load().getString("idel.url") + "/users") {
    private fun rolesAsJsonArray(roles: List<String>): String {
        return roles.joinToString(prefix = "[", postfix = "]") {
            '"' + it + '"'
        }

    }

    fun registerUser(login: String, roles: List<String> = listOf("ROLE_SUPER_USER")): Response {
        val rolesJsonArray = rolesAsJsonArray(roles)

        return Given {
            initRequest(this, user = login)
            body("""
                {
                    "id": "$login@httpbasic",
                    "email": "user@mail.fake",
                    "displayName": "$login registered from tests",
                    "avatar": "",
                    "roles": $rolesJsonArray
                }
            """.trimIndent())
        } When {
            post(usersEndpointUrl)
        }
    }

    fun changeRoles(loginHowChangesRole: String, loginWhichIsChanged: String, roles: List<String> = listOf()): Response {
        val rolesJsonArray = rolesAsJsonArray(roles)

        return Given {
            initRequest(this, user = loginHowChangesRole)
            body(rolesJsonArray)
        } When {
            put("$usersEndpointUrl/$loginWhichIsChanged@httpbasic/roles")
        }
    }

    fun load(loginHowLoaded : String, loadedLogin : String) : Response {
        return Given {
            initRequest(this, user = loginHowLoaded)
        } When {
            get("$usersEndpointUrl/$loadedLogin@httpbasic")
        }
    }

}

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