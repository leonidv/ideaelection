package idel.domain

import org.springframework.util.DigestUtils
import java.lang.RuntimeException
import java.util.*

interface Identifiable {
    val id : String
}

/**
 * Usually indicate that required entity (or value object) is not exists.
 */
class EntityNotFound(entityType : String, id : String) : IllegalArgumentException("Entity is not exists, type=[$entityType], id = [$id] ")

/**
 * Usually indicate that creation of new entity (or value object) is failed, because id is not unique.
 */
class EntityAlreadyExists(entityType: String, id : String) : IllegalArgumentException("Entity already exists, type=[$entityType], id = [$id]")
/**
 * Generate id from UUID without dashes.
 */
fun generateId() : String = UUID.randomUUID().toString().replace("-","")

/**
 * Make id based on field values. May be used for value object or similar to them.
 *
 * @return md5 of concatenation of values. MD5 used because it's need speed, not security.
 */
fun compositeId(vararg values: Any) : String {
    val valueString = values.joinToString(separator = "|")
    return DigestUtils.md5DigestAsHex(valueString.toByteArray())
}