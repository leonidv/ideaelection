package idel.domain

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.data.*
import io.kotest.matchers.shouldBe

class RolesSpec : DescribeSpec({
   describe("A roles") {
       describe("findIncorrect") {
           table(
                   headers("checked roles", "incorrect roles"),
                   row(Roles.all, setOf()),
                   row(setOf("GOD_ROLE"),setOf("GOD_ROLE")),
                   row(setOf(Roles.SUPER_USER,"GOD_ROLE"), setOf("GOD_ROLE"))
           ).forAll {roles, expectedIncorrect ->
               Roles.findIncorrect(roles).shouldBe(expectedIncorrect)
           }
       }
   }
})