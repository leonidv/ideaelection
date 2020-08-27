package idel.domain

class Roles {
    companion object {
        /*
         * Ordinary user
         */
        const val USER = "ROLE_USER"

        /**
         * User can admin application. Works with users from all groups, manage any group, etc.
         */
        const val SUPER_USER = "ROLE_SUPER_USER"

        /**
         * Set of all possible roles.
         */
        val all = setOf(USER, SUPER_USER)

        /**
         * Find roles which are incorrect.
         */
        fun findIncorrect(roles : Set<String>) : Set<String> = roles - all
    }


}