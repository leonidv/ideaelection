package idel.api

import arrow.core.Either
import idel.domain.*
import io.konform.validation.ValidationError
import io.konform.validation.ValidationErrors
import mu.KLogger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.LocalDateTime
import java.util.*

data class ExceptionDescription(
    val msg: String,
    val type: String,
    val stackTrace: Array<String>,
    val cause: ExceptionDescription?,
    val suppressed: List<ExceptionDescription>
) {
    companion object {
        fun of(exception: Throwable): ExceptionDescription {
            val cause = if (exception.cause != null) {
                of(exception.cause!!)
            } else {
                null
            }

            return ExceptionDescription(
                msg = exception.message ?: "",
                type = exception.javaClass.name,
                stackTrace = emptyArray(), // ExceptionUtils.getStackFrames(exception),
                cause = cause,
                suppressed = exception.suppressed.map {of(it)}
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExceptionDescription

        if (msg != other.msg) return false
        if (!stackTrace.contentEquals(other.stackTrace)) return false
        if (cause != other.cause) return false
        if (suppressed != other.suppressed) return false

        return true
    }

    override fun hashCode(): Int {
        var result = msg.hashCode()
        result = 31 * result + stackTrace.contentHashCode()
        result = 31 * result + (cause?.hashCode() ?: 0)
        result = 31 * result + suppressed.hashCode()
        return result
    }
}

data class ErrorDescription(
    val code: Int,
    val message: String,
    val validationErrors: Collection<ValidationError> = emptySet(),
    val exception: Optional<ExceptionDescription> = Optional.empty()
) {
    @Suppress("unused")
    val timestamp: LocalDateTime = LocalDateTime.now()

    companion object {
        fun incorrectArgument(argument: String, reason: String): ErrorDescription {
            return ErrorDescription(100, "Incorrect value $argument. $reason")
        }

        fun tooManyItems(value: Int, maxValue: Int): ErrorDescription {
            return ErrorDescription(101, "Required to many items. Allowed $maxValue, required : $value")
        }

        fun ideaNotFound(id: String): ErrorDescription {
            return ErrorDescription(102, "An idea with id = ${id} is not found")
        }

        fun entityNotFound(msg: String): ErrorDescription {
            return ErrorDescription(102, msg)
        }


        fun notAllowed(msg: String): ErrorDescription {
            return ErrorDescription(103, msg)
        }

        fun versionIsOutdated(): ErrorDescription {
            return ErrorDescription(104, "Version is outdated. Reload current version")
        }

        fun internal(msg: String): ErrorDescription {
            return ErrorDescription(105, msg)
        }

        fun internal(msg: String, ex: Exception): ErrorDescription {
            return ErrorDescription(105, msg, exception = Optional.of(ExceptionDescription.of(ex)))
        }

        fun validationFailed(msg: String, errors: Collection<ValidationError>): ErrorDescription {
            return ErrorDescription(106, msg, errors)
        }

        fun badRequestFormat(ex: Exception): ErrorDescription {
            return ErrorDescription(107, "bad format of request", exception = Optional.of(ExceptionDescription.of(ex)))
        }

        fun conflict(message: String): ErrorDescription {
            return ErrorDescription(108, message)
        }

        fun invalidOperation(description: String): ErrorDescription {
            return ErrorDescription(109, description)
        }

        fun logicallyDeleted(): ErrorDescription = ErrorDescription(110, "entity is not found")

        fun entityIsArchived(): ErrorDescription = ErrorDescription(111, "entity is in an archive, can't be edited")
    }
}


/**
 *  Common used in rest controllers.
 */
typealias ResponseDataOrError<T> = ResponseEntity<DataOrError<T>>


interface DataOrErrorHelper {
    val log : KLogger

    fun <T : Any> Either<DomainError, T>.asHttpResponse() : ResponseDataOrError<T> {
        return DataOrError.fromEither(this, this@DataOrErrorHelper.log)
    }
}

@Suppress("unused")
data class DataOrError<T>(val data: Optional<T>, val error: Optional<ErrorDescription>) {
    companion object {
        fun <T : Any> error(description: ErrorDescription): DataOrError<T> {
            return DataOrError(Optional.empty(), Optional.of(description))
        }

        fun <T : Any> response(body: T): DataOrError<T> {
            return DataOrError(Optional.of(body), Optional.empty())
        }

        /**
         * Make [ResponseEntity] with [HttpStatus.BAD_REQUEST]
         */
        fun <T :Any> errorResponse(
            description: ErrorDescription,
            code: HttpStatus = HttpStatus.BAD_REQUEST
        ): ResponseEntity<DataOrError<T>> {
            val x = error<T>(description)
            return ResponseEntity(x, code)
        }

        fun <T : Any> badRequest(ex: Exception): ResponseEntity<DataOrError<T>> {
            return errorResponse(ErrorDescription.badRequestFormat(ex))
        }

        fun <T : Any> internal(ex: Exception, log: KLogger): ResponseEntity<DataOrError<T>> {

            return internal("can't process operation", ex, log)
        }

        fun <T : Any> internal(msg: String): ResponseEntity<DataOrError<T>> {
            return errorResponse(ErrorDescription.internal(msg), HttpStatus.INTERNAL_SERVER_ERROR)
        }

        /**
         * Make [ResponseEntity] with [ErrorDescription.internal]
         */
        fun <T : Any> internal(msg: String, ex: Exception, log: KLogger): ResponseEntity<DataOrError<T>> {
            log.warn(ex) {"Operation is failed, msg = $msg"}
            return errorResponse(ErrorDescription.internal(msg, ex), HttpStatus.INTERNAL_SERVER_ERROR)
        }

        /**
         * Make [errorResponse] with collection of [ValidationError].
         */
        fun <T : Any> invalid(errors: ValidationErrors): ResponseEntity<DataOrError<T>> {
            return errorResponse(
                ErrorDescription.validationFailed("request has validation errors", errors),
                HttpStatus.BAD_REQUEST
            )
        }

        fun <T : Any> invalid(errors: Collection<ValidationError>): ResponseEntity<DataOrError<T>> {
            return errorResponse(
                ErrorDescription.validationFailed("request has validation errors", errors),
                HttpStatus.BAD_REQUEST
            )
        }

        fun <T : Any> invalidOperation(description: String): ResponseEntity<DataOrError<T>> {
            return errorResponse(ErrorDescription.invalidOperation(description), HttpStatus.BAD_REQUEST)
        }

        fun <T : Any> conflict(message: String): ResponseEntity<DataOrError<T>> {
            return errorResponse(ErrorDescription.conflict(message), HttpStatus.CONFLICT)
        }

        /**
         * Choose result based on [operationResult]:
         *
         * * isRight then return ok
         * * isLeft - then select error result based on exception type.
         */
        fun <T : Any> fromEitherExp(operationResult: Either<Exception, T>, log: KLogger): ResponseEntity<DataOrError<T>> {
            return when (operationResult) {
                is Either.Right -> ok(operationResult.value)
                is Either.Left -> when (val ex = operationResult.value) {
                    is EntityNotFoundExp -> notFound("${ex.message}")
                    is EntityLogicallyDeletedExp -> errorResponse(
                        ErrorDescription.logicallyDeleted(),
                        HttpStatus.NOT_FOUND
                    )
                    is EntityArchivedExp -> errorResponse(ErrorDescription.entityIsArchived(), HttpStatus.BAD_REQUEST)
                    is EntityAlreadyExistsExp -> conflict(ex.message!!)
                    is OperationNotPermittedExp -> forbidden("operation is not permitted")
                    is ValidationException -> invalid(ex.errors)
                    is InvalidOperationExp -> invalidOperation(ex.message!!)
                    else -> internal(operationResult.value, log)
                }
            }
        }

        fun <T : Any> fromEither(operationResult: Either<DomainError, T>, log: KLogger): ResponseEntity<DataOrError<T>> {
            return when (operationResult) {
                is Either.Right -> ok(operationResult.value)
                // Parent classes should be after child classes!!!
                is Either.Left -> when (val error = operationResult.value) {
                    is EntityArchived -> errorResponse(ErrorDescription.entityIsArchived(), HttpStatus.BAD_REQUEST)
                    is EntityNotFound -> notFound(error.message)
                    is EntityAlreadyExists -> conflict(error.message)
                    is ExceptionError -> internal(error.message, error.ex, log)
                    is OperationNotPermitted -> forbidden(error.message)
                    is InvalidOperation -> invalidOperation(error.message)
                    is idel.domain.ValidationError -> invalid(error.errors)
                    else -> internal(error.message)
                }
            }
        }

        /**
         * Make [ResponseEntity] with [ErrorDescription.notAllowed] and code [HttpStatus.FORBIDDEN]
         */
        fun <T : Any> forbidden(reason: String): ResponseEntity<DataOrError<T>> {
            return errorResponse(ErrorDescription.notAllowed(reason), HttpStatus.FORBIDDEN)
        }

        /**
         * Make [ResponseEntity] with [ErrorDescription.versionIsOutdated] and code [HttpStatus.CONFLICT]
         */
        fun <T : Any> versionIsOutdated(): ResponseEntity<DataOrError<T>> {
            return errorResponse(ErrorDescription.versionIsOutdated(), HttpStatus.CONFLICT)
        }

        /**
         * Make [ResponseEntity] with [ErrorDescription.ideaNotFound] and code [HttpStatus.NOT_FOUND]
         */
        fun <T : Any> ideaNotFound(id: String): ResponseEntity<DataOrError<T>> {
            return errorResponse(ErrorDescription.ideaNotFound(id), HttpStatus.NOT_FOUND)
        }

        fun <T : Any> notFound(msg: String): ResponseEntity<DataOrError<T>> {
            return errorResponse(ErrorDescription.entityNotFound(msg), HttpStatus.NOT_FOUND)
        }

        /**
         * Return [internal] error with message "not implemented". Use as [TODO] for MVC controllers.
         */
        @Suppress("unused")
        fun <T : Any> notImplemented(): ResponseEntity<DataOrError<T>> {
            return internal("not implemented yet")
        }

        /**
         * Make [ResponseEntity] with [ErrorDescription.incorrectArgument] and code [HttpStatus.BAD_REQUEST]
         */
        fun <T : Any> incorrectArgument(argument: String, reason: String): ResponseEntity<DataOrError<T>> {
            return errorResponse(ErrorDescription.incorrectArgument(argument, reason), HttpStatus.BAD_REQUEST)
        }


        fun <T : Any> data(body: T, code: HttpStatus = HttpStatus.OK): ResponseEntity<DataOrError<T>> {
            return ResponseEntity.status(code).body(response(body))
        }

        /**
         * Return data as entity with [HttpStatus.OK]
         */
        fun <T : Any> ok(data: T): ResponseEntity<DataOrError<T>> {
            return data(data)
        }
    }

    init {
        assert(data.isPresent && error.isEmpty)
        assert(data.isEmpty && error.isPresent)
    }


}

