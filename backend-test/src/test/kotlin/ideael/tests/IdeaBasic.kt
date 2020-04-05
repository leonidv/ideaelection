package ideael.tests

import assertk.assertThat
import assertk.assertions.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import io.restassured.module.kotlin.extensions.*
import io.restassured.response.Response
import org.apache.http.HttpStatus

class APITests : Spek({

    describe("Working with votes as user") {
        describe("Add with title =[new title], description = [new description] by user = [userA]") {
            beforeGroup {
                val r =
                    Given {
                        initRequest(this, "userA")
                        body("""{ "title": "new title", "description": "new description", "link":"new link" }""")
                    } When {
                        post("$ideaelUrl/ideas")
                    }

                val data = checkEntityData(this, r, 201)

                it("has title [new title]") {
                    assertThat(data["title"]).isEqualTo("new title")
                }

                it("offeredBy [userA]") {
                    assertThat(data["offeredBy"]).isEqualTo("userA")
                }

                it("description is [new description]") {
                    assertThat(data["description"]).isEqualTo("new description")
                }

                it("link is [new link]") {
                    assertThat(data["link"]).isEqualTo("new link")
                }

            }
        }

        describe("load idea by id (idea created by userB)") {
            beforeGroup {
                val ideaId = addIdea("userB", "title 2", "description 2", "link 2")

                describe("userB loads idea [id=$ideaId]") {
                    beforeGroup {
                        val r = Given {
                            initRequest(this, "userB")
                        } When {
                            get("$ideaelUrl/ideas/$ideaId")
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
                            assertThat(data.get("offeredBy")).isEqualTo("userB")
                        }
                    }
                }

                describe("userA loads idea [id=$ideaId]") {
                    beforeGroup {
                        val r = Given {
                            initRequest(this, "userA")
                        } When {
                            get("$ideaelUrl/ideas/$ideaId")
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
                            assertThat(data.get("offeredBy")).isEqualTo("userB")
                        }
                    }
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
                    put("$ideaelUrl/ideas/$id")
                }
            }

            lateinit var originIdeaId: String

            beforeGroup {
                originIdeaId = addIdea("userA", "title", "description", "link")
            }

            describe("negative scenarios") {
                describe("idea not found") {
                    beforeGroup {
                        val r = sendUpdate("userA", "3fe4f81c943243a2808ea1c8af752dcc")
                        checkError(this, r, 102, HttpStatus.SC_NOT_FOUND)
                    }
                }

                describe("user which is not offered try to changes Idea") {
                    beforeGroup {
                        val r = sendUpdate("userB", originIdeaId)
                        checkError(this, r, 103, HttpStatus.SC_FORBIDDEN)
                    }
                }
            }

            describe("positive scenario") {
                describe("update all fields, title=[t2], description=[d2], link=[l2]") {
                    beforeGroup {
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
        }

        describe("voting for election") {
            describe("negative scenarios") {
                describe("voting for idea which is not exists") {
                    beforeGroup {
                        val r = Given {
                            initRequest(this, "userA")
                        } When {
                            post("$ideaelUrl/ideas/3fe4f81c943243a2808ea1c8af752dcc/voters")
                        }

                        checkError(this, r, 102, HttpStatus.SC_NOT_FOUND)
                    }
                }

                describe("devoting for idea which is not exists") {
                    beforeGroup {
                        val r = Given {
                            initRequest(this, "userA")
                        } When {
                            delete("$ideaelUrl/ideas/3fe4f81c943243a2808ea1c8af752dcc/voters")
                        }

                        checkError(this, r, 102, HttpStatus.SC_NOT_FOUND)
                    }
                }
            }

            describe("positive scenarios") {
                describe("userB votes for an idea offered userA") {
                    beforeGroup {
                        val ideaId = addIdea("userA", "some idea title", "some description", "some link")
                        val r = Given {
                            initRequest(this, "userB")
                        } When {
                            post("$ideaelUrl/ideas/$ideaId/voters")
                        }

                        val data = checkEntityData(this, r, 200)

                        it("has userB as voter") {
                            assertThat(data["voters"]).isEqualTo(listOf("userB"))
                        }
                    }
                }

                describe("userA votes for his own idea") {
                    beforeGroup {
                        val ideaId = addIdea("userA", "some idea title", "some description", "some link")

                        val r = Given {
                            initRequest(this, "userA")
                        } When {
                            post("$ideaelUrl/ideas/$ideaId/voters")
                        }

                        val data = checkEntityData(this, r, 200)

                        it("userA is not in voters, because he offered this idea") {
                            assertThat(data["voters"]).isEqualTo(listOf<String>())
                        }
                    }
                }

                describe("userB devotes for an Idea") {
                    beforeGroup {
                        val ideaId = addIdea("userA", "some idea title", "some description", "some link")

                        Given {
                            initRequest(this, "userB")
                        } When {
                            post("$ideaelUrl/ideas/$ideaId/voters")
                        }

                        val r = Given {
                            initRequest(this, "userB")
                        } When {
                            delete("$ideaelUrl/ideas/$ideaId/voters")
                        }


                        val data = checkEntityData(this, r, 200)

                        it("userB is not in voters (list of voters is empty)") {
                            assertThat(data["voters"]).isEqualTo(listOf<String>())
                        }
                    }
                }
            }
        }

        describe("change assignee") {
            describe("negative scenarios") {
                describe("assign to userA and try to remove assignee by userB") {
                    beforeGroup {
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
            }

            describe("positive scenarios") {
                describe("userA adds idea and assign it to self") {
                    beforeGroup {
                        val ideaId = addIdea("userA", "t", "d", "l")

                        val r = Given {
                            initRequest(this, "userA")
                        } When {
                            post("/ideas/$ideaId/assignee/userA")
                        }

                        val data = checkEntityData(this, r)

                        it("response contains assignee as userA") {
                            assertThat(data["assignee"]).isEqualTo("userA")
                        }

                        it("idea loaded by userA contains assignee as userA") {
                            val data = loadIdea("userA", ideaId)
                            assertThat(data["assignee"]).isEqualTo("userA")
                        }

                        it("idea loaded by userB contains assignee as userA") {
                            val data = loadIdea("userB", ideaId)
                            assertThat(data["assignee"]).isEqualTo("userA")
                        }
                    }
                }

                describe("userA assigns idea and removes self as assignee") {
                    beforeGroup {
                        val ideaId = addIdea("userA", "t", "d", "l")
                        changeAssignee("userA", ideaId, "userA")

                        val r = Given {
                            initRequest(this, "userA")
                        } When {
                            delete("$ideaelUrl/ideas/$ideaId/assignee")
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
                }

                describe("userA adds idea, assigns to self. userB assigns idea to self") {
                    beforeGroup {
                        val ideaId = addIdea("userA", "t", "d", "l")
                        changeAssignee("userA", ideaId, "userA")
                        val r = Given {
                            initRequest(this, "userB")
                        } When {
                            post("$ideaelUrl/ideas/$ideaId/assignee/userB")
                        }

                        val data = checkEntityData(this, r)

                        it("response contains assignee as empty string") {
                            assertThat(data["assignee"]).isEqualTo("userB")
                        }

                        it("idea loaded by userA contains assignee as empty string") {
                            val data = loadIdea("userA", ideaId)
                            assertThat(data["assignee"]).isEqualTo("userB")
                        }

                        it("idea loaded by userB contains assignee as empty string") {
                            val data = loadIdea("userB", ideaId)
                            assertThat(data["assignee"]).isEqualTo("userB")
                        }
                    }
                }
            }


        }

        describe("change implemented") {
            describe("negative scenarios") {
                describe("userA add an idea, assigns it to self. UserB mark the idea as implemented") {
                    beforeGroup {
                        val ideaId = addIdea("userA")
                        changeAssignee("userA", ideaId, "userA")
                        val r = markImplemented("userB", ideaId, true, HttpStatus.SC_FORBIDDEN)

                        checkError(this, r, 103, HttpStatus.SC_FORBIDDEN)
                    }
                }

                describe("userA add an idea, assigns it to self, mark as implemented. UserB mark the idea as unimplemented") {
                    beforeGroup {
                        val ideaId = addIdea("userA")
                        changeAssignee("userA", ideaId, "userA")
                        markImplemented("userA", ideaId, true)
                        val r = markImplemented("userB", ideaId, false, HttpStatus.SC_FORBIDDEN)

                        checkError(this, r, 103, HttpStatus.SC_FORBIDDEN)
                    }
                }
            }

            describe("positive scenarios") {
                describe("userA add an idea, assignes is to self and then marks it as implemented") {
                    beforeGroup {
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
                }

                describe("userA add an idea, userB assigns is to self and then marks it as implemented") {
                    beforeGroup {
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
                }

                describe("userA add an idea, assigns it self, mark implemented and mark as unimplemented") {
                    beforeGroup {
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
        }
    }
})

