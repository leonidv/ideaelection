package idel.domain

import assertk.assertThat
import assertk.assertions.*
import io.kotest.core.spec.style.DescribeSpec

import java.time.LocalDateTime

data class IdeaInfoValues(
        override val groupId: String,
        override val title: String,
        override val description: String,
        override val link: String,
) : IIdeaEditableProperties

class TestUser(
               override val id : String,
               override val email: String,
               override val displayName: String,
               override val avatar: String,
               override val roles: Set<String>
) : User {

    companion object {

        val DUMMY_USER = TestUser(
                id = "user1",
                email = "dummy@mail",
                displayName =  "dummy dummy",
                avatar =  "none",
                roles = setOf(Roles.USER)
        );
    }

}

fun ideaInfo(title: String = "t", groupId: String = "g", description: String = "d", link: String = "l"): IIdeaEditableProperties {
    return IdeaInfoValues(
            groupId = groupId,
            title = title,
            description = description,
            link = link
    )
}

class IdeaSpec : DescribeSpec({
    describe("An IdeaFactory") {
        val factory = IdeaFactory()

        describe("a new idea with title = [t], description = [d], link = [l], offeredBy = [v]") {
            val idea = factory.createIdea(ideaInfo(),"v")

            it("has title [t]") {
                assertThat(idea.title).isEqualTo("t")
            }

            it("has description [d]") {
                assertThat(idea.description).isEqualTo("d")
            }

            it("has voterId = [v]") {
                assertThat(idea.author).isEqualTo("v")
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

        val voterA = TestUser("ua", "a@email", "a", "", setOf(Roles.USER));
        val voterB = TestUser("ub", "b@email", "b", "", setOf(Roles.USER));

        describe("copy") {
            val originIdea = Idea(
                id = "1",
                groupId = "g",
                title = "t",
                description = "d",
                link = "l",
                assignee = "userA",
                implemented = false,
                author = "userA",
                voters = setOf("userB"),
                ctime = LocalDateTime.of(1984,7,1,20,18)
            )

            describe("copy with changes") {
                val newIdea = originIdea.update(
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
                    assertThat(newIdea.author).isEqualTo("userA")
                }

                it("new idea voters is equals to origin idea voters (voters=[{userB}]") {
                    assertThat(newIdea.voters).isEqualTo(setOf("userB"))
                }

                it("new idea ctime is equals to origin idea ctime (ctime=[1984-07-01 20:18])") {
                    assertThat(newIdea.ctime).isEqualTo(LocalDateTime.of(1984,7,1,20,18))
                }
            }

            describe("copy without any changes") {
                val newIdea = originIdea.update()

                it("new idea is equals to origin idea") {
                    assertThat(newIdea).isEqualTo(originIdea)
                }
            }

        }

        describe("voting") {
            describe("user can vote for an idea") {
                val idea = originIdea.addVote(TestUser.DUMMY_USER.id)

                it("id is not changed") {
                    assertThat(idea.id).isEqualTo(originIdea.id)
                }

                it("has voter [dummy@mail]") {
                    assertThat(idea.voters).isEqualTo(setOf(TestUser.DUMMY_USER.id))
                }

                it("return 1 vote") {
                    assertThat(idea.votesCount()).isEqualTo(1)
                }
            }

            describe("many users can vote for an idea") {
                val idea = originIdea
                    .addVote(voterA.id)
                    .addVote(voterB.id)

                it("has voter [a@email, b@email]") {
                    assertThat(idea.voters).isEqualTo(setOf(voterA.id, voterB.id))
                }

                it("has 2 votes") {
                    assertThat(idea.votesCount()).isEqualTo(2)
                }
            }

            describe("user can't vote twice for same idea") {
                val idea = originIdea
                    .addVote(TestUser.DUMMY_USER.id)
                    .addVote(TestUser.DUMMY_USER.id)

                it("has only one voter [dummy@email]") {
                    assertThat(idea.voters).isEqualTo(setOf(TestUser.DUMMY_USER.id))
                }

                it("has 1 vote") {
                    assertThat(idea.votesCount()).isEqualTo(1)
                }
            }

            describe("user, which has offered idea, can't vote for him") {
                val offeredById = originIdea.author
                val idea = originIdea.addVote(offeredById)

                it("idea is not changed") {
                    assertThat(idea).isEqualTo(originIdea)
                }
            }

        }

        describe("removing vote") {
            describe("user can remove his vote (only he have voted)") {
                val idea = originIdea
                    .addVote(TestUser.DUMMY_USER.id)
                    .removeVote(TestUser.DUMMY_USER.id)

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
                val idea = originIdea.removeVote(TestUser.DUMMY_USER.id)

                it("has not any voters") {
                    assertThat(idea.voters).isEmpty()
                }

                it("has 0 votes") {
                    assertThat(idea.votesCount()).isEqualTo(0)
                }
            }

            describe("user can't remove vote, if he doesn't vote for an idea") {
                val idea = originIdea
                    .addVote(voterA.id)
                    .removeVote(voterB.id)

                it("has voter [a@email] ") {
                    assertThat(idea.voters).isEqualTo(setOf(voterA.id))
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