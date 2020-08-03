package idel.infrastructure.security

import idel.domain.Roles
import org.springframework.security.access.vote.RoleVoter
import org.springframework.security.core.authority.SimpleGrantedAuthority

class IdelAuthorities {
    companion object {
        val USER_AUTHORITY = SimpleGrantedAuthority(Roles.USER)
        val SUPER_USER_AUTHORITY = SimpleGrantedAuthority(Roles.SUPER_USER)
    }
}