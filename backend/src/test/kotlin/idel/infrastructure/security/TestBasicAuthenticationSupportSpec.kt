package idel.infrastructure.security

import arrow.core.Either
import idel.domain.EntityNotFound
import idel.domain.GroupMemberRepository
import idel.domain.Roles
import idel.domain.UserRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.*
import io.mockk.every
import io.mockk.mockk

class TestUsersDetailsServiceSpec : DescribeSpec({
    val userRepository = mockk<UserRepository>()

    every {userRepository.load(any())} returns Either.Left(EntityNotFound("",""))

    val userDetailsService = TestUsersDetailsService(userRepository)
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
