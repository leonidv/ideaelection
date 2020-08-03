package idel.infrastructure.security

import idel.domain.Roles
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.*

class TestUsersDetailsServiceSpec : DescribeSpec({
    val userDetailsService = TestUsersDetailsService()
    describe("roles applying") {
        describe("without role suffix should make role ${Roles.USER}") {
           val user = userDetailsService.loadUserByUsername("bob")

            it("has authority ${IdelAuthorities.USER_AUTHORITY.authority}") {
                user.authorities.shouldBe(setOf(IdelAuthorities.USER_AUTHORITY))
            }
        }

        describe("with suffix [__super_user] should make role ${Roles.SUPER_USER}") {
            val user = userDetailsService.loadUserByUsername("alice__super_user")
            it("has authority ${IdelAuthorities.SUPER_USER_AUTHORITY.authority}") {
                user.authorities.shouldBe(setOf(IdelAuthorities.SUPER_USER_AUTHORITY))
            }
        }
    }
})
