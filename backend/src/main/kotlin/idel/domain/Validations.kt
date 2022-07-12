package idel.domain

import arrow.core.Either
import arrow.core.flatten
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

    fun <X> ifValid(properties: T, action: () -> X) :Either<ValidationError,X> {
        val validationResult = validation.validate(properties)
        return when (validationResult) {
            is Invalid -> {
                val errors = validationResult.errors
                Either.Left(ValidationError("properties is invalid", errors))
            }
            is Valid -> Either.Right(action())
        }
    }

    fun <X> ifValidEither(properties: T, action: () -> Either<DomainError,X>) : Either<DomainError,X> {
        return ifValid(properties, action).flatten()
    }

    fun <X> ifValidExp(properties: T, action: () -> X): Either<ValidationException, X> {
        val validationResult = validation.validate(properties)
        return when (validationResult) {
            is Invalid -> {
                val errors = validationResult.errors
                Either.Left(ValidationException("properties is invalid", errors))
            }
            is Valid -> Either.Right(action())
        }
    }

    fun <X> ifValidEitherExp(properties: T, action: () -> Either<Exception,X>): Either<Exception,X> {
        val x: Either<Exception, Either<Exception, X>> = ifValidExp(properties,action)
        return when (x) {
            is Either.Left -> x
            is Either.Right -> x.value
        }
    }
}