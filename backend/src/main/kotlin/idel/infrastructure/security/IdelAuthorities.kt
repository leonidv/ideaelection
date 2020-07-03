package idel.infrastructure.security

import org.springframework.security.core.authority.SimpleGrantedAuthority

class IdelAuthorities {
    companion object {
        const val USER = "idel_user"
        val USER_AUTHORITY = SimpleGrantedAuthority(USER)

        const val GROUP_ADMIN = "idel_group_admin"
        val GROUP_ADMIN_AUTHORITY = SimpleGrantedAuthority(GROUP_ADMIN)
    }
}