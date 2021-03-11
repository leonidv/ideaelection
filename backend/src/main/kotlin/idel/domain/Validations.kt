package idel.domain

import arrow.core.Either
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


interface Validator<T> {
    val validation : Validation<T>

    fun <X> ifValid(properties: T, action: () -> X): Either<ValidationException, X> {
        val validationResult = validation.validate(properties)
        return when (validationResult) {
            is Invalid -> {
                val errors = validationResult.errors
                Either.left(ValidationException("properties is invalid", errors))
            }
            is Valid -> Either.right(action())
        }
    }
}