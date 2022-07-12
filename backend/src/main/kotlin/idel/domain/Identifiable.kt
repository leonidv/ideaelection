package idel.domain

import org.springframework.util.DigestUtils
import java.util.*

interface Identifiable {
    val id: String
}

/**
 * Generate id from UUID without dashes.
 */
fun generateId() : String = UUID.randomUUID().toString()

/**
 * Make id based on field values. May be used for value object or similar to them.
 *
 * @return md5 of concatenation of values. MD5 used because it's need speed, not security.
 */
fun compositeId(key : String,  vararg values: Any) : String {
    val valueString = key + "$" + values.joinToString(separator = "|")
    return key + DigestUtils.md5DigestAsHex(valueString.toByteArray())
}