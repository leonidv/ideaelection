package idel.tests.scenario

import arrow.core.Some
import idel.tests.apiobject.*
import idel.tests.infrastructure.*
import idel.tests.infrastructure.JsonNodeExtensions.dataId
import io.kotest.core.spec.style.DescribeSpec
import java.net.HttpURLConnection
import kotlin.time.ExperimentalTime

@ExperimentalTime
class JoinToPublicGroupScenario : DescribeSpec({
    val couchbase = EntityStorage()
    beforeSpec {
        couchbase.clearAll()
    }

    context("empty installation without any groups and users") {
        val userA = User("userA")
        val userB = User("userB")
        val userC = User("userC")
        val userD = User("userD", "not member")

        registryUsers(userA, userB, userC, userD)

        lateinit var groupId: String
        lateinit var joiningKey: String

        describe("$userA adds new PUBLIC group without admins") {
            val groupInfo = createGroup(groupAdmin = userA, entryMode = GroupsApi.PUBLIC, members = setOf())
            groupId = groupInfo.groupId
            joiningKey = groupInfo.joiningKey
        }

        describe("$userB sees group in the list of available") {

            val availableResponse = userB.groups.loadAvailable()

            checkIsOk(
                availableResponse,
                includeGroup(groupId)
            )
        }

        describe("$userB can't add an idea to the group") {
            val ideaResponse = userB.ideas.add(groupId)

            it("can't do it 403 and error [OperationNotPermitted]") {
                ideaResponse.shouldHasStatus(HttpURLConnection.HTTP_FORBIDDEN)
                ideaResponse.shouldBeError(103)
            }
        }

        describe("$userB joins to the group") {
            val joinRequestResponse = userB.joinRequests.create(joiningKey)

            checkIsOk(
                joinRequestResponse,
                joinRequestIsApproved
            )
        }

        describe("list of the group's members include $userB") {
            val response = userA.groups.loadMembers(groupId)
            checkIsOk(response, groupHasMember(userB))
        }

        describe("$userC joins to the group") {
            val response = userC.joinRequests.create(joiningKey)

            checkIsOk(
                response,
                joinRequestIsApproved
            )

        }

        describe("list of the group's members include $userB and $userC") {
            val response = userA.groups.loadMembers(groupId)
            checkIsOk(
                response,
                groupHasMember(userB),
                groupHasMember(userC))
        }

        describe("$userB adds an idea to the group") {
            val addIdeaResponse = userB.ideas.add(groupId)

            checkIsOk(addIdeaResponse)

            val ideaId = addIdeaResponse.extractId("idea")


            context("load idea") {
                describe("$userB (author) can load idea") {
                    checkIsOk(userB.ideas.load(ideaId))
                }

                describe("$userC (member) can load idea") {
                    checkIsOk(userC.ideas.load(ideaId))
                }

                describe("$userA (admin) can load idea") {
                    checkIsOk(userA.ideas.load(ideaId))
                }

                describe("$userD  can't load idea") {
                    checkIsForbidden(userD.ideas.load(ideaId))
                }
            }
        }
    }
})

