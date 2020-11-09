package idel.domain

import java.lang.RuntimeException
import java.util.*

interface Identifiable {
    val id : String
}

class EntityNotFound(entityType : String, id : String) : RuntimeException("Entity type=[$entityType], id = [$id] is not exists")
/**
 * Generate id from UUID without dashes.
 */
fun generateId() : String = UUID.randomUUID().toString().replace("-","")