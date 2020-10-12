package idel.api

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.couchbase.client.core.error.CouchbaseException
import idel.domain.generateId
import io.konform.validation.ValidationError
import io.konform.validation.ValidationErrors
import mu.KLogger
import org.apache.commons.lang3.exception.ExceptionUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.LocalDateTime
import java.util.*
import kotlin.Exception

data class ExceptionDescription(
        val msg: String,
        val stackTrace: Array<String>,
        val cause: ExceptionDescription?,
        val suppressed: List<ExceptionDescription>
) {
    companion object {
        fun of(exception: Throwable): ExceptionDescription {

            val msg = if (exception is CouchbaseException) {
                exception.context().toString()
            } else {
                exception.message ?: ""
            }

            val cause = if (exception.cause != null) {
                ExceptionDescription.of(exception.cause!!)
            } else {
                null
            }

            return ExceptionDescription(
                    msg = msg,
                    stackTrace = ExceptionUtils.getStackFrames(exception),
                    cause = cause,
                    suppressed = exception.suppressed.map {ExceptionDescription.of(it)}
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

data class ErrorDescription(val code: Int,
                            val message: String,
                            val validationErrors: Collection<ValidationError> = emptySet(),
                            val exception: Optional<ExceptionDescription> = Optional.empty()
) {
    val timestamp = LocalDateTime.now()

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
    }
}


class ResponseOrError<T>(val data: Optional<T>, val error: Optional<ErrorDescription>) {
    companion object {
        fun <T> error(description: ErrorDescription): ResponseOrError<T> {
            return ResponseOrError<T>(Optional.empty(), Optional.of(description))
        }

        fun <T> response(body: T): ResponseOrError<T> {
            return ResponseOrError(Optional.of(body), Optional.empty())
        }

        /**
         * Make [ResponseEntity] with [HttpStatus.BAD_REQUEST]
         */
        fun <T> errorResponse(
                description: ErrorDescription,
                code: HttpStatus = HttpStatus.BAD_REQUEST
        ): ResponseEntity<ResponseOrError<T>> {
            val x = error<T>(description)
            return ResponseEntity(x, code)
        }

        fun <T> internal(msg: String): ResponseEntity<ResponseOrError<T>> {
            return errorResponse(ErrorDescription.internal(msg), HttpStatus.INTERNAL_SERVER_ERROR)
        }

        /**
         * Make [ResponseEntity] with [ErrorDescription.internal]
         */
        fun <T> internal(msg: String, ex: Exception): ResponseEntity<ResponseOrError<T>> {
            return errorResponse(ErrorDescription.internal(msg, ex), HttpStatus.INTERNAL_SERVER_ERROR)
        }

        /**
         * Make [errorResponse] with collection of [ValidationError].
         */
        fun <T> invalid(errors: ValidationErrors): ResponseEntity<ResponseOrError<T>> {
            return errorResponse(ErrorDescription.validationFailed("Properties is not valid", errors))
        }

        /**
         * Make [ResponseEntity] [ok] if [operationResult] is [Either.Right]. Otherwise return [internal] and print
         * exception to [log].
         */
        fun <T> fromLoading(operationResult: Either<Exception, T>, log: KLogger): ResponseEntity<ResponseOrError<T>> {
            return when (operationResult) {
                is Either.Right -> ok(operationResult.b)
                is Either.Left -> {
                    val errorId = generateId();
                    val ex = operationResult.a
                    log.warn(ex) {"Can't process operation, errorId: ${errorId}"}
                    internal("${ex.message}, errorId: ${errorId}")
                }
            }
        }

        /**
         * Make [ResponseEntity] [ok] if [operationResult] is [Some], otherwise return [notFound]
         */
        fun <T> fromLoading(id: String, operationResult: Option<T>): ResponseEntity<ResponseOrError<T>> {
            return when (operationResult) {
                is Some -> ok(operationResult.t)
                is None -> notFound(id)
            }
        }

        /**
         * Make [ResponseEntity] with [ErrorDescription.notAllowed] and code [HttpStatus.FORBIDDEN]
         */
        fun <T> forbidden(reason: String): ResponseEntity<ResponseOrError<T>> {
            return errorResponse(ErrorDescription.notAllowed(reason), HttpStatus.FORBIDDEN)
        }

        /**
         * Make [ResponseEntity] with [ErrorDescription.versionIsOutdated] and code [HttpStatus.CONFLICT]
         */
        fun <T> versionIsOutdated(): ResponseEntity<ResponseOrError<T>> {
            return errorResponse(ErrorDescription.versionIsOutdated(), HttpStatus.CONFLICT)
        }

        /**
         * Make [ResponseEntity] with [ErrorDescription.ideaNotFound] and code [HttpStatus.NOT_FOUND]
         */
        fun <T> notFound(id: String): ResponseEntity<ResponseOrError<T>> {
            return errorResponse(ErrorDescription.ideaNotFound(id), HttpStatus.NOT_FOUND)
        }


        /**
         * Return [internal] error with message "not implemented". Use as [TODO] for MVC controllers.
         */
        @Suppress("unused")
        fun <T> notImplemented(): ResponseEntity<ResponseOrError<T>> {
            return internal("not implemented yet")
        }

        /**
         * Make [ResponseEntity] with [ErrorDescription.incorrectArgument] and code [HttpStatus.BAD_REQUEST]
         */
        fun <T> incorrectArgument(argument: String, reason: String): ResponseEntity<ResponseOrError<T>> {
            return errorResponse(ErrorDescription.incorrectArgument(argument, reason), HttpStatus.BAD_REQUEST)
        }


        fun <T> data(body: T, code: HttpStatus = HttpStatus.OK): ResponseEntity<ResponseOrError<T>> {
            return ResponseEntity.status(code).body(response(body))
        }

        /**
         * Return data as entity with [HttpStatus.OK]
         */
        fun <T> ok(data: T): ResponseEntity<ResponseOrError<T>> {
            return data(data)
        }
    }

    init {
        assert(data.isPresent && error.isEmpty)
        assert(data.isEmpty && error.isPresent)
    }

}

