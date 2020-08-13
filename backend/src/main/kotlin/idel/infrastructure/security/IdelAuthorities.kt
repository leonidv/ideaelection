package idel.infrastructure.security

import idel.domain.Roles
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

class IdelAuthorities {
    companion object {
        val USER_AUTHORITY = SimpleGrantedAuthority(Roles.USER)
        val SUPER_USER_AUTHORITY = SimpleGrantedAuthority(Roles.SUPER_USER)

        val all = setOf(USER_AUTHORITY, SUPER_USER_AUTHORITY)

        /**
         * Safe method for converting from [Roles] into [IdelAuthorities]
         * @throws IllegalAccessException if role is incorrect
         */
        fun from(role : String) : GrantedAuthority {
            return when(role) {
                Roles.USER -> USER_AUTHORITY
                Roles.SUPER_USER -> SUPER_USER_AUTHORITY
                else -> throw IllegalArgumentException("incorrect role: $role")
            }
        }

        /**
         * Safe method for converting from iterable of [Roles] into [IdelAuthorities]
         */
        fun from(roles : Iterable<String>) : List<GrantedAuthority> {
            return roles.map {from(it)}
        }

        /**
         * Safe method for converting one of [IdelAuthorities] into [Roles].
         * @throws IllegalArgumentException if authority is incorrect
         */
        fun asRole(grantedAuthority: GrantedAuthority) : String {
            return when(grantedAuthority.authority) {
                USER_AUTHORITY.authority -> Roles.USER
                SUPER_USER_AUTHORITY.authority -> Roles.SUPER_USER
                else -> throw IllegalArgumentException("Unknown authority: ${grantedAuthority.authority}")
            }
        }

        /**
         * Safe method for converting iterable of [IdelAuthorities] into [Roles]
         * @see asRole
         */
        fun asRoles(authority: Iterable<GrantedAuthority>) : List<String> {
            return authority.map {asRole(it)}
        }
    }
}