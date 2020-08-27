package idel.domain

import java.util.*

/**
 * Generate id from UUID without dashes.
 */
fun generateId() : String = UUID.randomUUID().toString().replace("-","")