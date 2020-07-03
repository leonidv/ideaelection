package idel.domain

import assertk.assertThat
import assertk.assertions.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDateTime

data class IdeaInfoValues(
    override val title: String,
    override val description: String,
    override val link: String
) : IdeaInfo

class TestUser(override val email: String, override val profileName: String, override val profilePhoto: String) : User {
    companion object {
        val DUMMY = TestUser("dummy@mail", "dummy dummy","none")
    }
    override fun id(): String = email;
}

fun ideaInfo(title: String = "t", description: String = "d", link: String = "l"): IdeaInfo {
    return IdeaInfoValues(title, description, link)
}

class IdeaSpec : Spek({
    describe("An IdeaFactory") {
        val factory by memoized { IdeaFactory() }

        describe("a new idea with title = [t], description = [d], link = [l], offeredBy = [v]") {
            lateinit var idea: Idea
            beforeEachTest {
                idea = factory.createIdea(ideaInfo(), "v")
            }

            it("has title [t]") {
                assertThat(idea.title).isEqualTo("t")
            }

            it("has description [d]") {
                assertThat(idea.description).isEqualTo("d")
            }

            it("has voterId = [v]") {
                assertThat(idea.offeredBy).isEqualTo("v")
            }


            it("not implemented") {
                assertThat(idea.implemented).isFalse()
            }

            it("link is [l]]") {
                assertThat(idea.link).isEqualTo("l")
            }

            it("don't have any voters") {
                assertThat(idea.voters).isEmpty()
            }
        }
    }
    describe("An Idea") {

        val originIdea = IdeaFactory().createIdea(ideaInfo(), "u@email")

        val voterA = TestUser("a@email", "a", "")
        val voterB = TestUser("b@email", "b", "")

        describe("copy") {
            val originIdea = Idea(
                id = "1",
                title = "t",
                description = "d",
                link = "l",
                assignee = "userA",
                implemented = false,
                offeredBy = "userA",
                voters = setOf("userB"),
                ctime = LocalDateTime.of(1984,7,1,20,18)
            )

            describe("copy with changes") {
                val newIdea = originIdea.copy(
                    title = "new title",
                    description = "new description",
                    link = "new link",
                    implemented = true
                )

                it("new idea title is [new title]") {
                    assertThat(newIdea.title).isEqualTo("new title")
                }

                it("origin idea title is not changed (title is [t])") {
                    assertThat(originIdea.title).isEqualTo("t")
                }

                it("new idea description is [new description]") {
                    assertThat(newIdea.description).isEqualTo("new description")
                }

                it("origin idea description is not changes (description is [d])") {
                    assertThat(originIdea.description).isEqualTo("d")
                }

                it("new idea link is [new link]") {
                    assertThat(newIdea.link).isEqualTo("new link")
                }

                it("origin idea link is not changes (link is [l])") {
                    assertThat(originIdea.link).isEqualTo("l")
                }

                it("new implemented is true ") {
                    assertThat(newIdea.implemented).isTrue()
                }

                it("new idea id is equals to origin idea id (id = [1])") {
                    assertThat(newIdea.id).isEqualTo("1")
                }

                it("new idea offeredBy is equals to origin idea offeredBy (offeredBy=[userA])") {
                    assertThat(newIdea.offeredBy).isEqualTo("userA")
                }

                it("new idea voters is equals to origin idea voters (voters=[{userB}]") {
                    assertThat(newIdea.voters).isEqualTo(setOf("userB"))
                }

                it("new idea ctime is equals to origin idea ctime (ctime=[1984-07-01 20:18])") {
                    assertThat(newIdea.ctime).isEqualTo(LocalDateTime.of(1984,7,1,20,18))
                }
            }

            describe("copy without any changes") {
                val newIdea = originIdea.copy()

                it("new idea is equals to origin idea") {
                    assertThat(newIdea).isEqualTo(originIdea)
                }
            }

        }

        describe("voting") {
            describe("user can vote for an idea") {
                val idea = originIdea.addVote(TestUser.DUMMY.id())

                it("id is not changed") {
                    assertThat(idea.id).isEqualTo(originIdea.id)
                }

                it("has voter [dummy@mail]") {
                    assertThat(idea.voters).isEqualTo(setOf(TestUser.DUMMY.id()))
                }

                it("return 1 vote") {
                    assertThat(idea.votesCount()).isEqualTo(1)
                }
            }

            describe("many users can vote for an idea") {
                val idea = originIdea
                    .addVote(voterA.id())
                    .addVote(voterB.id())

                it("has voter [a@email, b@email]") {
                    assertThat(idea.voters).isEqualTo(setOf(voterA.id(), voterB.id()))
                }

                it("has 2 votes") {
                    assertThat(idea.votesCount()).isEqualTo(2)
                }
            }

            describe("user can't vote twice for same idea") {
                val idea = originIdea
                    .addVote(TestUser.DUMMY.id())
                    .addVote(TestUser.DUMMY.id())

                it("has only one voter [dummy@email]") {
                    assertThat(idea.voters).isEqualTo(setOf(TestUser.DUMMY.id()))
                }

                it("has 1 vote") {
                    assertThat(idea.votesCount()).isEqualTo(1)
                }
            }

            describe("user, which has offered idea, can't vote for him") {
                val offeredById = originIdea.offeredBy
                val idea = originIdea.addVote(offeredById)

                it("idea is not changed") {
                    assertThat(idea).isEqualTo(originIdea)
                }
            }

        }

        describe("removing vote") {
            describe("user can remove his vote (only he have voted)") {
                val idea = originIdea
                    .addVote(TestUser.DUMMY.id())
                    .removeVote(TestUser.DUMMY.id())

                it("has not any voters") {
                    assertThat(idea.voters).isEmpty()
                }

                it("has 0 votes") {
                    assertThat(idea.votesCount()).isEqualTo(0)
                }
            }

            describe("user can remove his vote (other users have voted for an idea)") {
                val idea = originIdea
                    .addVote("a")
                    .addVote("b")
                    .removeVote("b")

                it("has only voter [a]") {
                    assertThat(idea.voters).isEqualTo(setOf("a"))
                }

                it("has one vote") {
                    assertThat(idea.votesCount()).isEqualTo(1)
                }
            }

            describe("user can't remove vote for an idea without any vote") {
                val idea = originIdea.removeVote(TestUser.DUMMY.id())

                it("has not any voters") {
                    assertThat(idea.voters).isEmpty()
                }

                it("has 0 votes") {
                    assertThat(idea.votesCount()).isEqualTo(0)
                }
            }

            describe("user can't remove vote, if he doesn't vote for an idea") {
                val idea = originIdea
                    .addVote(voterA.id())
                    .removeVote(voterB.id())

                it("has voter [a@email] ") {
                    assertThat(idea.voters).isEqualTo(setOf("a@email"))
                }

                it("has 1 vote") {
                    assertThat(idea.votesCount()).isEqualTo(1)
                }
            }
        }

        describe("assignee") {

            describe("change assignee to user A") {
                val newIdea = originIdea.assign("userA")

                it("new idea has assignee = [userA]") {
                    assertThat(newIdea.assignee).isEqualTo("userA")
                }
            }

            describe("assign to userA, then to userB") {
                val newIdea = originIdea
                    .assign("userA")
                    .assign("userB")

                it("new idea has assignee = [userB]") {
                    assertThat(newIdea.assignee).isEqualTo("userB")
                }
            }

            describe("assign to userA and then remove assignee") {
                val newIdea = originIdea
                    .assign("userA")
                    .removeAssign()

                it("new idea has assignee = NOT_ASSIGNED") {
                    assertThat(newIdea.assignee).isEqualTo(NOT_ASSIGNED)
                }
            }
        }
    }
})