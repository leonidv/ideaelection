package idel.tests.spec.ideas

import com.fasterxml.jackson.databind.JsonNode
import idel.tests.apiobject.*
import idel.tests.infrastructure.*
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.spec.style.scopes.DescribeSpecContainerContext
import java.net.http.HttpResponse
import java.util.*


fun newVersion() = UUID.randomUUID().toString().subSequence(0, 8)

class IdeaEditSpec : DescribeSpec({
    val couchbase = EntityStorage()
    beforeSpec {
        couchbase.clearAll()
    }

    val userAdmin = User("userA", "group admin")
    val userB = User("userB", "member")
    val userC = User("userC", "member")
    val userD = User("userD", "member")
    val userX = User("userX", "not member")

    lateinit var groupId: String


    context("$userAdmin creates group, userB, userC, userD are members") {
        describe("initialization") {
            registerUsers(userAdmin, userB, userC, userD, userX)

            groupId = createGroup(groupAdmin = userAdmin, members = setOf(userB, userC, userD)).groupId
        }

        describe("security checks") {
            lateinit var ideaId: String

            describe("$userB add ideas") {
                val addResponse = userB.ideas.add(
                    groupId = groupId,
                    summary = "summary v1",
                    description = "description [b]v1[/b]",
                    descriptionPlainText = "description v1",
                    link = "http://test.io/v1"
                )
                checkIsOk(
                    addResponse,
                    ideaHasSummary("summary v1"),
                    ideaHasDescription("description [b]v1[/b]"),
                    ideaHasDescriptionPlainText("description v1"),
                    ideaHasLink("http://test.io/v1")
                )
                ideaId = addResponse.extractId("idea")
                userB.role = "author"
            }

            describe("unassigned idea") {
                describe("$userAdmin can edit") {
                    val version = newVersion()
                    val response = userAdmin.ideas.quickEdit(ideaId, version)
                    checkUpdateToVersion(response, version)
                }

                describe("$userB can edit") {
                    val version = newVersion()
                    val response = userB.ideas.quickEdit(ideaId, version)
                    checkUpdateToVersion(response, version)
                }

                checkCanNotEdit(ideaId, userC, userD, userX)
            }

            describe("$userC is assignee") {
                describe("$userC assign idea him self") {
                    checkIsOk(
                        userC.ideas.assign(ideaId, userC),
                        ideaAssigneeIs(userC)
                    )
                    userC.role = "assignee"
                }

                describe("$userAdmin can edit") {
                    val version = newVersion()
                    val response = userAdmin.ideas.quickEdit(ideaId, version)
                    checkUpdateToVersion(response, version)
                }

                describe("$userC can edit") {
                    val version = newVersion()
                    val response = userC.ideas.quickEdit(ideaId, version)
                    checkUpdateToVersion(response, version)
                }

                checkCanNotEdit(ideaId, userB, userD, userX)
            }

            describe("idea is implemented") {
                describe("implement idea") {
                    checkIsOk(userAdmin.ideas.implemented(ideaId), ideaIsImplemented)
                }

                describe("$userAdmin can edit idea") {
                    val version = newVersion()
                    val response = userAdmin.ideas.quickEdit(ideaId, version)
                    checkUpdateToVersion(response, version)
                }

                checkCanNotEdit(ideaId, userB, userC, userD, userX)
            }
        }

        describe("validation checks") {
            lateinit var ideaId: String
            userB.role = "member"
            describe("$userB adds idea") {
                val response = userB.ideas.add(groupId)
                checkIsOk(response)
                ideaId = response.extractId("idea")
                userB.role = "author"
            }


            describe("minimal length of field and invalid URL") {
                val response = userB.ideas.edit(
                    ideaId,
                    summary = "s",
                    description = "d",
                    descriptionPlainText = "d",
                    link = "u"
                )

                checkValidationErrors(
                    response,
                    ValidationError.leastCharacters(".summary", 3),
                    ValidationError.leastCharacters(".description", 3),
                    ValidationError.leastCharacters(".descriptionPlainText", 3),
                    ValidationError.mustBeUrl(".link")
                )
            }

            describe("maximal length of field (link is valid URL)") {
                val response = userB.ideas.edit(
                    ideaId,
                    summary = "s".repeat(256),
                    description = "d".repeat(10001),
                    descriptionPlainText = "d".repeat(2001),
                    link = "http://test.io/123"
                )

                checkValidationErrors(
                    response,
                    ValidationError.mostCharacters(".summary", 255),
                    ValidationError.mostCharacters(".description", 10000),
                    ValidationError.mostCharacters(".descriptionPlainText", 2000),
                )
            }

            describe("fields has maximum length and link is empty") {
                val response = userB.ideas.edit(
                    ideaId,
                    summary = "s".repeat(255),
                    description = "d".repeat(10000),
                    descriptionPlainText = "d".repeat(2000),
                    link = ""
                )
                checkIsOk(response)
            }
        }
    }
})

/**
 * Check idea is update. See [IdeasApi.quickEdit] for details.
 */
suspend fun DescribeSpecContainerContext.checkUpdateToVersion(response: HttpResponse<JsonNode>, version: CharSequence) {
    checkIsOk(
        response,
        ideaHasSummary("summary $version"),
        ideaHasDescription("description [b]$version[/b]"),
        ideaHasDescriptionPlainText("description $version"),
        ideaHasLink("http://somelink.io/$version")
    )
}

/**
 * Check that users can't update idea.
 */
suspend fun DescribeSpecContainerContext.checkCanNotEdit(ideaId: String, vararg users: User) {
    users.forEach {user ->
        describe("$user can't edit") {
            checkIsForbidden(user.ideas.quickEdit(ideaId, newVersion()))
        }
    }
}