package idel.domain

import arrow.core.Either
import io.konform.validation.*
import java.net.URL

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

// https://rgxdb.com/r/1NUN74O6
val base64Regex = ("""data\:image/(png|jpg);base64,(?:[A-Za-z0-9+\/]{4})*""" +
        """(?:[A-Za-z0-9+\/]{2}==|[A-Za-z0-9+\/]{3}=|[A-Za-z0-9+\/]{4})""").toRegex()

fun ValidationBuilder<String>.isImageBase64(allowEmptyValue: Boolean): Constraint<String> {

    return addConstraint("must be PNG or JPG in base64") { value ->
        if (allowEmptyValue && value.isBlank()) {
            true
        } else {
            base64Regex.matches(value)
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
                Either.Left(ValidationException("properties is invalid", errors))
            }
            is Valid -> Either.Right(action())
        }
    }
}