package idel.domain

import io.konform.validation.*
import java.net.URL
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

/**
 * Represent exception which are thrown on the validation fail.
 */
class ValidationException(msg: String, val errors: Collection<ValidationError>)
    : RuntimeException("$msg, errors = $errors") {
}

/**
 * Validate that string is URL
 */
fun ValidationBuilder<String>.isUrl(allowEmptyValue : Boolean): Constraint<String> {
    return addConstraint("must be URL") {value ->
         if (allowEmptyValue && value.isBlank()) {
             true
         } else {
             try {
                 URL(value).toURI()
                 true
             } catch (e: Exception) {
                 false
             }
         }
    }
}
