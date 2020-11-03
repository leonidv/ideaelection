package idel.tests.spec

import assertk.assertThat
import assertk.assertions.*
import idel.tests.*
import idel.tests.apiobject.Couchbase
import io.kotest.core.spec.style.DescribeSpec
import io.restassured.module.kotlin.extensions.*
import io.restassured.response.Response
import java.time.LocalDateTime

data class UsersIdeas(
    val userId: String,
    val offeredByUser: List<String>,
    val assignedToUser: List<String>,
    val implemented: List<String>,
    val assignedToAnother: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UsersIdeas

        if (userId != other.userId) return false

        return true
    }

    override fun hashCode(): Int {
        return userId.hashCode()
    }
}

fun addIdeas(user: String): UsersIdeas {
    val all = (1..10).map { i ->
        val now = LocalDateTime.now()
        val uuid = addIdea(
            user = user,
            title = "idea #$i by $user",
            description = "This genius idea created at $now",
            link = "link"
        )
        uuid
    }

    val assignedToUser = all.subList(0, 4)

    assignedToUser.forEach { ideaId ->
        changeAssignee(user, ideaId, user)
    }

    val implemented = assignedToUser.subList(0, 2)
    implemented.forEach { ideaId ->
        markImplemented(user, ideaId, true)
    }

    return UsersIdeas(
        userId = user,
        offeredByUser = all,
        assignedToUser = assignedToUser,
        implemented = implemented,
        assignedToAnother = ""
    )
}

fun voteForIdea(
    ideaId: String,
    anotherUsers: Collection<String>
) {
    anotherUsers.forEach { vote(it, ideaId) }
}

class IdeaLoading : DescribeSpec({

    lateinit var allIdeas: List<String>
    lateinit var userAIdeas: UsersIdeas
    lateinit var userBIdeas: UsersIdeas
    lateinit var votedIdeas: List<String>

    beforeSpec {
        Couchbase().clearIdeas()

        userAIdeas = addIdeas("userA")
        userBIdeas = addIdeas("userB")


        val lastByUserA = userAIdeas.offeredByUser.last()
        changeAssignee("userB", lastByUserA, "userB")

        val lastByUserB = userBIdeas.offeredByUser.last()
        changeAssignee("userA", lastByUserB, "userA")

        userAIdeas = userAIdeas.copy(
            assignedToUser = userAIdeas.assignedToUser + lastByUserB,
            assignedToAnother = lastByUserA
        )
        // save order of ideas sorted by ctime
        userBIdeas = userBIdeas.copy(
            assignedToUser = listOf(lastByUserA) + userBIdeas.assignedToUser,
            assignedToAnother = lastByUserB
        )

        allIdeas = userAIdeas.offeredByUser + userBIdeas.offeredByUser
        votedIdeas = allIdeas.shuffled().subList(0, 5)
        voteForIdea(votedIdeas[0], listOf("user1", "user2", "user3", "user4", "user5"))
        voteForIdea(votedIdeas[1], listOf("user1", "user2", "user3", "user4"))
        voteForIdea(votedIdeas[2], listOf("user1", "user2", "user3"))
        voteForIdea(votedIdeas[3], listOf("user1", "user2"))
        voteForIdea(votedIdeas[4], listOf("user1"))

    }

    describe("Loading Ideas") {
        describe("negative scenarios") {
            describe("request negative count of records [first=10, last=0]") {
                val r: Response = Given {
                    initRequest(this, "userA")
                    param("first", 10)
                    param("last", 0)
                } When {
                    get("$idelUrl/ideas")
                }

                checkError(this, r, 100)

            }

            describe("request to many records [first=0, last = 101") {
                val r = Given {
                    initRequest(this, "userA")
                    param("first", 0)
                    param("last", 101)
                } When {
                    get("$idelUrl/ideas")
                }

                checkError(this, r, 101)
            }
        }

        describe("positive scenarios") {


            describe("without filtering") {
                describe("with default values (latest 10 records without filtering)") {

                    val r = Given {
                        initRequest(this, "userA")
                    } When {
                        get("$idelUrl/ideas")
                    }

                    val data = checkListData(this, r)

                    it("loaded 10 latest records") {
                        val latestIds = allIdeas.subList(10, 20).reversed()
                        val actualIds = data.map { it["id"] }
                        assertThat(actualIds).isEqualTo(latestIds)
                    }

                }

                describe("latest five records (first=0&last=5)") {

                    var r = Given {
                        initRequest(this, "userA")
                        param("first", 0)
                        param("last", 5)
                    } When {
                        get("$idelUrl/ideas")
                    }

                    val data = checkListData(this, r)

                    it("loaded 5 latest records") {
                        val latestIds = allIdeas.subList(15, 20).reversed()
                        val actualIds = data.map { it["id"] }
                        assertThat(actualIds).isEqualTo(latestIds)
                    }


                }

                describe("earliest five records (first=0&last=5&sorting=ctime_asc)") {

                    var r = Given {
                        initRequest(this, "userA")
                        param("first", 0)
                        param("last", 5)
                        param("sorting", "ctime_asc")
                    } When {
                        get("$idelUrl/ideas")
                    }

                    val data = checkListData(this, r)

                    it("loaded 5 earliest records") {
                        val earliestIds = allIdeas.subList(0, 5)
                        val actualIds = data.map { it["id"] }
                        assertThat(actualIds).isEqualTo(earliestIds)
                    }

                }

                describe("latest six records (first=0&last=6&sorting=ctime_desc)") {

                    var r =
                        Given {
                            initRequest(this, "userB")
                            param("first", 0)
                            param("last", 6)
                            param("sorting", "ctime_desc")
                        } When {
                            get("$idelUrl/ideas")
                        }

                    val data = checkListData(this, r)

                    it("loaded 6 latest") {
                        val earliestIds = allIdeas.reversed().subList(0, 6)
                        val actualIds = data.map { it["id"] }
                        assertThat(actualIds).isEqualTo(earliestIds)
                    }

                }

                describe("load 5,6,7 records (first=4&last=7)") {

                    val r = Given {
                        initRequest(this, "userC")
                        param("first", 4)
                        param("last", 7)
                    } When {
                        get("$idelUrl/ideas")
                    }

                    val data = checkListData(this, r)
                    it("load 5,6,7 records") {
                        val expected = allIdeas.reversed().subList(4, 7)
                        val actualsIds = data.map { it["id"] }
                        assertThat(actualsIds).isEqualTo(expected)
                    }

                }

                describe("load more then exists (first=0&last=50") {

                    val r = Given {
                        initRequest(this, "userA")
                        param("first", 0)
                        param("last", 50)
                    } When {
                        get("$idelUrl/ideas")
                    }

                    val data = checkListData(this, r)
                    it("load all 20 records") {
                        assertThat(data.map { it["id"] }).isEqualTo(allIdeas.reversed())
                    }

                }

                describe("most voted ideas") {

                    val r = Given {
                        initRequest(this, "userA")
                        param("sorting", "votes_desc")
                    } When {
                        get("$idelUrl/ideas")
                    }

                    val data = checkListData(this, r)
                    it("load in order by votes count") {
                        val mostVoted = data.subList(0, 5).map { it["id"] }
                        assertThat(votedIdeas).isEqualTo(mostVoted)
                    }

                }
            }

            describe("with filtering") {
                describe("offeredBy") {
                    describe("userC loads earliest 5 records which are offered by userA (first=0&last=5&sorting=ctime_asc&offeredBy=userA)") {

                        val r = Given {
                            initRequest(this, "userC")
                            param("first", 0)
                            param("last", 5)
                            param("sorting", "ctime_asc")
                            param("offeredBy", "userA@httpbasic")
                        } When {
                            get("$idelUrl/ideas")
                        }

                        val data = checkListData(this, r)

                        it("loaded 5 earliest records offered by userA ") {
                            val latestIds = userAIdeas.offeredByUser.subList(0, 5)
                            val actualIds = data.map { it["id"] }
                            assertThat(actualIds).isEqualTo(latestIds)
                        }


                    }

                    describe("userC loads latest 5 records which are offered by userB (first=0&last=5&sorting=ctime_desc&offeredBy=userB)") {

                        val r = Given {
                            initRequest(this, "userC")
                            param("first", 0)
                            param("last", 5)
                            param("sorting", "ctime_desc")
                            param("offeredBy", "userB@httpbasic")
                        } When {
                            get("$idelUrl/ideas")
                        }

                        val data = checkListData(this, r)

                        it("loaded earliest 5 records which are offered by userB") {
                            val latestIds = userBIdeas.offeredByUser.reversed().subList(0, 5)
                            val actualIds = data.map { it["id"] }
                            assertThat(actualIds).isEqualTo(latestIds)
                        }

                    }
                }

                describe("assignee") {
                    describe("userA loads records which are assigned to userA (assignee=userA)") {

                        val r = Given {
                            initRequest(this, "userA")
                            param("assignee", "userA@httpbasic")
                        } When {
                            get("$idelUrl/ideas")
                        }

                        val data = checkListData(this, r)

                        it("loaded records which are assigned to userA") {
                            val actualIds = data.map { it["id"] }
                            assertThat(actualIds).isEqualTo(userAIdeas.assignedToUser.reversed())
                        }

                    }

                    describe("userC loads records which are assigned to userB (assignee=userB)") {

                        val r = Given {
                            initRequest(this, "userC")
                            param("assignee", "userB@httpbasic")
                        } When {
                            get("$idelUrl/ideas")
                        }

                        val data = checkListData(this, r)

                        it("loaded records which are assigned to userB") {
                            val expectedIds = userBIdeas.assignedToUser.reversed()
                            val actualIds = data.map { it["id"] }
                            assertThat(actualIds).isEqualTo(expectedIds)
                        }

                    }
                }

                describe("implemented") {
                    describe("userA loads all implemented ideas") {

                        val r = Given {
                            initRequest(this, "userA")
                            param("implemented", "true")
                        } When {
                            get("$idelUrl/ideas")
                        }

                        val data = checkListData(this, r)

                        it("loaded records which are implemented") {
                            val expected = userAIdeas.implemented + userBIdeas.implemented
                            val actual = data.map { it["id"] }
                            assertThat(actual).isEqualTo(expected.reversed())
                        }

                    }

                    describe("userB loads all unimplemented ideas") {

                        val r = Given {
                            initRequest(this, "userB")
                            param("implemented", "false")
                            param("first", 0)
                            param("last", 50)
                        } When {
                            get("$idelUrl/ideas")
                        }

                        val data = checkListData(this, r)

                        it("loaded all unimplemented records") {
                            val expected =
                                (userAIdeas.offeredByUser + userBIdeas.offeredByUser) -
                                        (userAIdeas.implemented + userBIdeas.implemented)
                            val actual = data.map { it["id"] }
                            assertThat(actual).isEqualTo(expected.reversed())
                        }

                    }
                }

                describe("by text [userA] (text=userA)") {

                    val r = Given {
                        initRequest(this, "userC")
                        param("text", "userA")
                    } When {
                        get("$idelUrl/ideas")
                    }

                    val data = checkListData(this, r)


                    it("loaded all records contains text [userA]") {
                        assertThat(data.map { it["id"] }).isEqualTo(userAIdeas.offeredByUser.reversed())
                    }

                }

                describe("complex request") {
                    describe("userC loads first earliest idea which are offered by userA, assigned to userB and not implemented") {

                        val r = Given {
                            initRequest(this, "userC")
                            param("offset", 0)
                            param("last", 1)
                            param("offeredBy", "userA@httpbasic")
                            param("assignee", "userB@httpbasic")
                            param("implemented", false)
                        } When {
                            get("$idelUrl/ideas")
                        }

                        val data = checkListData(this, r)
                        it("found idea which is correct") {
                            assertThat(data.map { it["id"] }).isEqualTo(listOf(userAIdeas.assignedToAnother))
                        }
                    }
                }
            }
        }
    }
})