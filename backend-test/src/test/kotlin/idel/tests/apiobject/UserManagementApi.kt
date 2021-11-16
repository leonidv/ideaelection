package idel.tests.apiobject

import com.typesafe.config.ConfigFactory
import idel.tests.initRequest
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.When
import io.restassured.response.Response

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
            body(
                """
                {
                    "id": "$login@httpbasic",
                    "email": "user@mail.fake",
                    "displayName": "$login registered from tests",
                    "avatar": "",
                    "roles": $rolesJsonArray,
                    "subscriptionPlan":"FREE"
                }
            """.trimIndent()
            )
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