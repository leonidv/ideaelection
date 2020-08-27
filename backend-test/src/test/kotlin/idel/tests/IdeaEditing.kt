package idel.tests

import io.kotest.core.spec.style.DescribeSpec
import assertk.assertThat
import assertk.assertions.*

import io.restassured.module.kotlin.extensions.*
import io.restassured.response.Response
import org.apache.http.HttpStatus

class IdeaEditing : DescribeSpec({
    describe("Working with votes as user") {
        val r =
            Given {
                initRequest(this, "userA")
                body("""{ "title": "new title", "description": "new description", "link":"new link" }""")
            } When {
                post("$idelUrl/ideas")
            }

        val data = checkEntityData(this, r, 201)

        it("has title [new title]") {
              assertThat(data["title"]).isEqualTo("new title")
        }

        it("offeredBy [userA]") {
            assertThat(data["offeredBy"]).isUser("userA")
        }

        it("description is [new description]") {
            assertThat(data["description"]).isEqualTo("new description")
        }

        it("link is [new link]") {
            assertThat(data["link"]).isEqualTo("new link")
        }
    }

    describe("load idea by id (idea created by userB)") {
        val ideaId = addIdea("userB", "title 2", "description 2", "link 2")

        describe("userB loads idea [id=$ideaId]") {
            val r = Given {
                initRequest(this, "userB")
            } When {
                get("$idelUrl/ideas/$ideaId")
            }

            val data = checkEntityData(this, r)

            it("has required id") {
                assertThat(data.get("id")).isEqualTo(ideaId)
            }

            it("has title [title 2]") {
                assertThat(data.get("title")).isEqualTo("title 2")
            }

            it("has description [description 2]") {
                assertThat(data.get("description")).isEqualTo("description 2")
            }

            it("has link [link 2]") {
                assertThat(data["link"]).isEqualTo("link 2")
            }

            it("offeredBy [userB]") {
                assertThat(data.get("offeredBy")).isUser("userB")
            }
        }


        describe("userA loads idea [id=$ideaId]") {

            val r = Given {
                initRequest(this, "userA")
            } When {
                get("$idelUrl/ideas/$ideaId")
            }

            val data = checkEntityData(this, r)

            it("has required id") {
                assertThat(data.get("id")).isEqualTo(ideaId)
            }

            it("has title [title 2]") {
                assertThat(data.get("title")).isEqualTo("title 2")
            }

            it("has description [description 2]") {
                assertThat(data.get("description")).isEqualTo("description 2")
            }

            it("offeredBy [userB]") {
                assertThat(data.get("offeredBy")).isUser("userB")
            }
        }
    }

    describe("updating ideas info") {
        val newInfo = """{
                |"title" : "t2",
                |"description" : "d2",
                |"link":"l2"
                |}
            """.trimMargin()

        fun sendUpdate(user: String = "userA", id: String, info: String = newInfo): Response {
            return Given {
                initRequest(this, user)
                body(info)
            } When {
                put("$idelUrl/ideas/$id")
            }
        }

        val originIdeaId = addIdea("userA", "title", "description", "link")


        describe("negative scenarios") {
            describe("idea not found") {

                val r = sendUpdate("userA", "3fe4f81c943243a2808ea1c8af752dcc")
                checkError(this, r, 102, HttpStatus.SC_NOT_FOUND)

            }

            describe("user which is not offered try to changes Idea") {
                val r = sendUpdate("userB", originIdeaId)
                checkError(this, r, 103, HttpStatus.SC_FORBIDDEN)
            }
        }

        describe("positive scenario") {
            describe("update all fields, title=[t2], description=[d2], link=[l2]") {

                val r = sendUpdate("userA", originIdeaId)
                val data = checkEntityData(this, r)

                it("title is [t2]") {
                    assertThat(data["title"]).isEqualTo("t2")
                }

                it("description is [d2]") {
                    assertThat(data["description"]).isEqualTo("d2")
                }

                it("link is [l2]") {
                    assertThat(data["link"]).isEqualTo("l2")
                }
            }

        }
    }

    describe("voting for election") {
        describe("negative scenarios") {
            describe("voting for idea which is not exists") {

                val r = Given {
                    initRequest(this, "userA")
                } When {
                    post("$idelUrl/ideas/3fe4f81c943243a2808ea1c8af752dcc/voters")
                }

                checkError(this, r, 102, HttpStatus.SC_NOT_FOUND)
            }


            describe("devoting for idea which is not exists") {

                val r = Given {
                    initRequest(this, "userA")
                } When {
                    delete("$idelUrl/ideas/3fe4f81c943243a2808ea1c8af752dcc/voters")
                }

                checkError(this, r, 102, HttpStatus.SC_NOT_FOUND)
            }

        }

        describe("positive scenarios") {
            describe("userB votes for an idea offered userA") {

                val ideaId = addIdea("userA", "some idea title", "some description", "some link")
                val r = Given {
                    initRequest(this, "userB")
                } When {
                    post("$idelUrl/ideas/$ideaId/voters")
                }

                val data = checkEntityData(this, r, 200)

                it("has userB as voter") {
                    assertThat(data["voters"]).isEqualTo(listOf("userB@httpbasic"))
                }
            }


            describe("userA votes for his own idea") {

                val ideaId = addIdea("userA", "some idea title", "some description", "some link")

                val r = Given {
                    initRequest(this, "userA")
                } When {
                    post("$idelUrl/ideas/$ideaId/voters")
                }

                val data = checkEntityData(this, r, 200)

                it("userA is not in voters, because he offered this idea") {
                    assertThat(data["voters"]).isEqualTo(listOf<String>())
                }

            }

            describe("userB devotes for an Idea") {

                val ideaId = addIdea("userA", "some idea title", "some description", "some link")

                Given {
                    initRequest(this, "userB")
                } When {
                    post("$idelUrl/ideas/$ideaId/voters")
                }

                val r = Given {
                    initRequest(this, "userB")
                } When {
                    delete("$idelUrl/ideas/$ideaId/voters")
                }


                val data = checkEntityData(this, r, 200)

                it("userB is not in voters (list of voters is empty)") {
                    assertThat(data["voters"]).isEqualTo(listOf<String>())
                }

            }
        }
    }

    describe("change assignee") {
        describe("negative scenarios") {
            describe("assign to userA and try to remove assignee by userB") {

                val ideaId = addIdea("userA", "t", "d", "l")
                changeAssignee("userA", ideaId, "userA")
                val r = Given {
                    initRequest(this, "userB")
                } When {
                    delete("/ideas/$ideaId/assignee/")
                }

                checkError(this, r, 103, HttpStatus.SC_FORBIDDEN)
            }

        }

        describe("positive scenarios") {
            describe("userA adds idea and assign it to self") {

                val ideaId = addIdea("userA", "t", "d", "l")

                val r = Given {
                    initRequest(this, "userA")
                } When {
                    post("/ideas/$ideaId/assignee/userA@httpbasic")
                }

                val data = checkEntityData(this, r)

                it("response contains assignee as userA") {
                    assertThat(data["assignee"]).isUser("userA")
                }

                it("idea loaded by userA contains assignee as userA") {
                    val data = loadIdea("userA", ideaId)
                    assertThat(data["assignee"]).isUser("userA")
                }

                it("idea loaded by userB contains assignee as userA") {
                    val data = loadIdea("userB", ideaId)
                    assertThat(data["assignee"]).isUser("userA")
                }
            }


            describe("userA assigns idea and removes self as assignee") {

                val ideaId = addIdea("userA", "t", "d", "l")
                changeAssignee("userA", ideaId, "userA")

                val r = Given {
                    initRequest(this, "userA")
                } When {
                    delete("$idelUrl/ideas/$ideaId/assignee")
                }

                val data = checkEntityData(this, r)

                it("response contains assignee as empty string") {
                    assertThat(data["assignee"]).isEqualTo("")
                }

                it("idea loaded by userA contains assignee as empty string") {
                    val data = loadIdea("userA", ideaId)
                    assertThat(data["assignee"]).isEqualTo("")
                }

                it("idea loaded by userB contains assignee as empty string") {
                    val data = loadIdea("userB", ideaId)
                    assertThat(data["assignee"]).isEqualTo("")
                }

            }

            describe("userA adds idea, assigns to self. userB assigns idea to self") {

                val ideaId = addIdea("userA", "t", "d", "l")
                changeAssignee("userA", ideaId, "userA")
                val r = Given {
                    initRequest(this, "userB")
                } When {
                    post("$idelUrl/ideas/$ideaId/assignee/userB@httpbasic")
                }

                val data = checkEntityData(this, r)

                it("response contains assignee as userB") {
                    assertThat(data["assignee"]).isUser("userB")
                }

                it("idea loaded by userA contains assignee as userB") {
                    val data = loadIdea("userA", ideaId)
                    assertThat(data["assignee"]).isUser("userB")
                }

                it("idea loaded by userB contains assignee as userB") {
                    val data = loadIdea("userB", ideaId)
                    assertThat(data["assignee"]).isUser("userB")
                }
            }

        }


    }

    describe("change implemented") {
        describe("negative scenarios") {
            describe("userA add an idea, assigns it to self. UserB mark the idea as implemented") {

                val ideaId = addIdea("userA")
                changeAssignee("userA", ideaId, "userA")
                val r = markImplemented("userB", ideaId, true, HttpStatus.SC_FORBIDDEN)

                checkError(this, r, 103, HttpStatus.SC_FORBIDDEN)
            }


            describe("userA add an idea, assigns it to self, mark as implemented. UserB mark the idea as unimplemented") {

                val ideaId = addIdea("userA")
                changeAssignee("userA", ideaId, "userA")
                markImplemented("userA", ideaId, true)
                val r = markImplemented("userB", ideaId, false, HttpStatus.SC_FORBIDDEN)

                checkError(this, r, 103, HttpStatus.SC_FORBIDDEN)

            }
        }

        describe("positive scenarios") {
            describe("userA add an idea, assignes is to self and then marks it as implemented") {

                val ideaId = addIdea("userA")
                changeAssignee("userA", ideaId, "userA")
                val r = markImplemented("userA", ideaId, true)
                var data = checkEntityData(this, r)

                it("response has implemented as [true]") {
                    assertThat(data["implemented"]).isEqualTo(true)
                }


                it("loaded as userA has implemented as [true]") {
                    data = loadIdea("userA", ideaId)
                    assertThat(data["implemented"]).isEqualTo(true)
                }

                it("loaded as userB has implemented as [true]") {
                    data = loadIdea("userB", ideaId)
                    assertThat(data["implemented"]).isEqualTo(true)
                }


            }

            describe("userA add an idea, userB assigns is to self and then marks it as implemented") {

                val ideaId = addIdea("userA")
                changeAssignee("userB", ideaId, "userB")
                val r = markImplemented("userB", ideaId, true)

                val data = checkEntityData(this, r)
                it("response has implemented as [true]") {
                    assertThat(data["implemented"]).isEqualTo(true)
                }


                it("loaded as userA has implemented as [true]") {
                    val data = loadIdea("userA", ideaId)
                    assertThat(data["implemented"]).isEqualTo(true)
                }

                it("loaded as userB has implemented as [true]") {
                    val data = loadIdea("userB", ideaId)
                    assertThat(data["implemented"]).isEqualTo(true)
                }


            }

            describe("userA add an idea, assigns it self, mark implemented and mark as unimplemented") {

                val ideaId = addIdea("userA")
                changeAssignee("userA", ideaId, "userA")
                markImplemented("userA", ideaId, true)
                val r = markImplemented("userA", ideaId, false)
                var data = checkEntityData(this, r)

                it("response has implemented as [false]") {
                    assertThat(data["implemented"]).isEqualTo(false)
                }


                it("loaded as userA has implemented as [false]") {
                    data = loadIdea("userA", ideaId)
                    assertThat(data["implemented"]).isEqualTo(false)
                }

                it("loaded as userB has implemented as [false]") {
                    data = loadIdea("userB", ideaId)
                    assertThat(data["implemented"]).isEqualTo(false)
                }

            }
        }
    }
})