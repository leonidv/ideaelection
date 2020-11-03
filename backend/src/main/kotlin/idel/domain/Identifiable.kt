package idel.domain

import java.util.*

interface Identifiable {
    val id : String
}

/**
 * Generate id from UUID without dashes.
 */
fun generateId() : String = UUID.randomUUID().toString().replace("-","")