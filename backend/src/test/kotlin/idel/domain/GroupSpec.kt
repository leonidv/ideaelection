package idel.domain

import arrow.core.Either
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.assertions.konform.shouldContainError
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

class GroupSpec : DescribeSpec({
    val factory = GroupFactory()
    describe("validation") {

        describe("create with valid properties") {

            val props = GroupEditableProperties(
                    title = "title",
                    description = "description",
                    usersRestrictions = listOf("user@httpbasic"))

            val eitherGroup = factory.createGroup("user@testuser", props)

            it("should be created") {
                eitherGroup.shouldBeRight()
            }

            if (eitherGroup is Either.Right ) {
                val group = eitherGroup.b
                it("has title [title]", ) {
                    group.title.shouldBe("title")
                }

                it("has creator [user@testuser]") {
                    group.creator.shouldBe("user@testuser")
                }

                it("id is initialized") {
                    group.id.isNotBlank()
                }
                it("user restrictions is [user@httpbasic]") {
                    group.usersRestrictions.shouldContainAll("user@httpbasic")
                }

            }
        }

        describe("create with invalid properties") {
            val badProperties = GroupEditableProperties(
                    title = "",
                    description = "",
                    usersRestrictions = listOf("(]")
            )

            val eitherGroup = factory.createGroup("user@test", badProperties)

            it("group is not created") {
                eitherGroup.shouldBeLeft()
            }

            if (eitherGroup is Either.Left ) {
                val invalid = eitherGroup.a

                it("should contain the error about title") {
                    invalid.shouldContainError(IGroupEditableProperties::title,"must have at least 3 characters")
                }

                it("should contains the error about description") {
                    invalid.shouldContainError(IGroupEditableProperties::description, "must have at least 1 characters")
                }

                it("should contains the error about restrictions") {
                    val error = invalid[IGroupEditableProperties::usersRestrictions,0]!![0]
                   // error.shouldNotBeNull()
                    error.shouldBe("Should be valid regexp")
                }

            }
        }
    }
})