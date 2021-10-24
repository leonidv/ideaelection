package idel.tests.spec.ideas

import idel.tests.apiobject.*
import idel.tests.infrastructure.*
import io.kotest.core.spec.style.DescribeSpec

class IdeaArchiveSpec : DescribeSpec({
    beforeSpec {
        EntityStorage().clearAll()
    }

    val userA = User("userA", "group admin")
    val userB = User("userB", "member")
    val userC = User("userC", "author")
    val userD = User("userD", "assignee")

    lateinit var groupId: String

    describe("initialization") {
        registryUsers(userA, userB, userC, userD)

        groupId = createGroup(userA, members = setOf(userB, userC, userD)).groupId
    }

    describe("archived idea can't be changed") {
        lateinit var ideaId: String

        describe("add idea") {
            val response = userA.ideas.quickAdd(groupId, "1")

            checkIsOk(response)

            ideaId = response.extractId("idea")
        }


        describe("$userA archive an idea") {
            val response = userA.ideas.changeArchived(ideaId, archived = true)
            checkIsOk(
                response,
                ideaIsArchived
            )
        }

        describe("$userA can't edit idea") {
            val response = userA.ideas.quickEdit(ideaId, "2")
            checkIsBadRequest(response, CustomErrors.ENTITY_IS_ARCHIVED)
        }

        describe("$userA can't change assignee") {
            val response = userA.ideas.assign(ideaId, userB)
            checkIsBadRequest(response, CustomErrors.ENTITY_IS_ARCHIVED)
        }

        describe("$userB can't vote for idea") {
            val response = userB.ideas.vote(ideaId)
            checkIsBadRequest(response, CustomErrors.ENTITY_IS_ARCHIVED)
        }

        describe("$userB can't devote idea") {
            val response = userB.ideas.devote(ideaId)
            checkIsBadRequest(response, CustomErrors.ENTITY_IS_ARCHIVED)
        }

        listOf(userA, userB).forEach {user ->
            describe("$user can load idea by id") {
                val response = user.ideas.load(ideaId)
                checkIsOk(response, ideaIsArchived)
            }
        }

        describe("$userA can deleted archived idea") {
            val response = userA.ideas.delete(ideaId)
            checkIsOk(response, ideaIsArchived, ideaIsDeleted)
        }
    }

    describe("archived idea can be restored from archive") {
        lateinit var ideaId: String

        describe("$userA add an idea") {
            ideaId = userA.ideas.quickAdd(groupId, "10").extractId("idea")
        }

        describe("$userA archive an idea") {
            val response = userA.ideas.changeArchived(ideaId, archived = true)
            checkIsOk(response, ideaIsArchived)
        }

        describe("$userA restore an idea from an archive") {
            val response = userA.ideas.changeArchived(ideaId, archived = false)
            checkIsOk(response, ideaIsNotArchived)
        }

        describe("$userB load restored idea") {
            val response = userB.ideas.load(ideaId)
            checkIsOk(response, ideaIsNotArchived)
        }

        describe("$userA can edit and idea") {
            val response = userA.ideas.quickEdit(ideaId, "11")
            checkIsOk(response)
        }
    }

    describe("security checks") {
        listOf(userA, userC, userD).forEach {user ->
            describe("$user can archive/restore idea") {
                lateinit var ideaId: String

                describe("$userC add idea") {
                    ideaId = userC.ideas.quickAdd(groupId, "1").extractId("idea")
                }

                if (user != userC) { // author can't edit assigned idea, so don't assign on author iteration
                    describe("$userD assigns idea to himself") {
                        checkIsOk(userD.ideas.assign(ideaId, userD))
                    }
                }



                describe("$user can archive idea") {
                    val response = user.ideas.changeArchived(ideaId, archived = true)
                    checkIsOk(
                        response,
                        ideaIsArchived
                    )
                }


                describe("$user can't edit archived idea") {
                    val response = user.ideas.quickEdit(ideaId, "2")
                    checkIsBadRequest(response, CustomErrors.ENTITY_IS_ARCHIVED)
                }

                describe("$user can restore the idea from an archive") {
                    val response = user.ideas.changeArchived(ideaId, archived = false)
                    checkIsOk(
                        response,
                        ideaIsNotArchived
                    )
                }
            }
        }

        describe("$userB (not admin, author or assignee) can't archive idea") {
            lateinit var ideaId: String

            describe("$userA adds an idea") {
                ideaId = userA.ideas.quickAdd(groupId, "1").extractId("idea")
            }

            describe("$userB can't archive an idea") {
                checkIsForbidden(userB.ideas.changeArchived(ideaId, archived = true))
            }

            describe("$userA archives an idea") {
                val response = userA.ideas.changeArchived(ideaId, archived = true)
                checkIsOk(response, ideaIsArchived)
            }

            describe("$userB can't restore an idea") {
                val response = userB.ideas.changeArchived(ideaId, archived = false)
                checkIsForbidden(response)
            }

        }
    }
})