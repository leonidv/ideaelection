package idel.domain

import io.konform.validation.Invalid
import io.konform.validation.ValidationError
import io.konform.validation.ValidationErrors

/**
 * Represent exception which are thrown on the validation fail.
 */
class ValidationException(msg: String, val errors: Collection<ValidationError>)
    : RuntimeException("$msg, errors = $errors") {
}