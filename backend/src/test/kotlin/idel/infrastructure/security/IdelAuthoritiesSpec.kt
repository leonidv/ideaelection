package idel.infrastructure.security

import idel.domain.Roles
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.types.shouldBeSameInstanceAs

class IdelAuthoritiesSpec : DescribeSpec({
    describe("positive scenarios") {
        describe("from role to authority") {
            table(
                    headers("role","authority"),
                    row(Roles.USER,IdelAuthorities.USER_AUTHORITY),
                    row(Roles.SUPER_USER, IdelAuthorities.SUPER_USER_AUTHORITY)
            ).forAll {role, authority ->
                describe("from $role to authority ${authority.authority}") {
                    it("correct instance of IdelAuthority") {
                        IdelAuthorities.from(role).shouldBeSameInstanceAs(authority)
                    }
                }
            }
        }

        describe("from all roles to all authorities") {
            val actual = IdelAuthorities.from(Roles.all)
            val expected = IdelAuthorities.all

            it("return all authorities") {
                actual.shouldHaveSize(expected.size)
                actual.shouldContainAll(expected)
            }
        }

        describe("authority to role") {
            table(
                    headers("authority","role"),
                    row(IdelAuthorities.USER_AUTHORITY, Roles.USER),
                    row(IdelAuthorities.SUPER_USER_AUTHORITY, Roles.SUPER_USER)
            ).forAll {authority, role ->
                describe("from authority ${authority.authority} to $role") {
                    it("correct role") {
                       IdelAuthorities.asRole(authority).shouldBeSameInstanceAs(role)
                    }
                }
            }
        }

        describe("all authorities as roles") {
            val actual = IdelAuthorities.asRoles(IdelAuthorities.all)
            val expected = Roles.all

            it("return all roles") {
                actual.shouldHaveSize(expected.size)
                actual.shouldContainAll(expected)
            }
        }
    }
})