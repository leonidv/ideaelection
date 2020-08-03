package idel.domain

import java.util.*

class Group (
        /**
         * Generated idenitfier.
         */
        val id : String,

        /**
         * Name of group.
         */
        val title : String,

        /**
         * Short description of group.
         */
        val description : String,

        /**
         * Regexp with users restriction. May include patterns like domains and other.
         */
        val usersRestriction : String
) {
}