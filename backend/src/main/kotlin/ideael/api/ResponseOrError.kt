package ideael.api

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.LocalDateTime
import java.util.*

data class ErrorDescription(val code: Int, val message: String) {
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

        fun internal(msg : String) : ErrorDescription {
            return ErrorDescription(105, msg)
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
        fun <T> badRequest(
            description: ErrorDescription,
            code: HttpStatus = HttpStatus.BAD_REQUEST
        ): ResponseEntity<ResponseOrError<T>> {
            val x = error<T>(description)
            return ResponseEntity(x, code)
        }

        /**
         * Make [ResponseEntity] with [ErrorDescription.notAllowed] and code [HttpStatus.FORBIDDEN]
         */
        fun <T> forbidden(reason: String): ResponseEntity<ResponseOrError<T>> {
            return badRequest(ErrorDescription.notAllowed(reason), HttpStatus.FORBIDDEN)
        }

        /**
         * Make [ResponseEntity] with [ErrorDescription.versionIsOutdated] and code [HttpStatus.CONFLICT]
         */
        fun <T> versionIsOutdated(): ResponseEntity<ResponseOrError<T>> {
            return badRequest(ErrorDescription.versionIsOutdated(), HttpStatus.CONFLICT)
        }

        /**
         * Make [ResponseEntity] with [ErrorDescription.ideaNotFound] and code [HttpStatus.NOT_FOUND]
         */
        fun <T> notFound(id: String): ResponseEntity<ResponseOrError<T>> {
            return badRequest(ErrorDescription.ideaNotFound(id), HttpStatus.NOT_FOUND)
        }


        fun <T>internal(msg : String) : ResponseEntity<ResponseOrError<T>> {
            return badRequest(ErrorDescription.internal(msg), HttpStatus.INTERNAL_SERVER_ERROR)
        }

        fun <T> data(body: T, code : HttpStatus = HttpStatus.OK): ResponseEntity<ResponseOrError<T>> {
            return ResponseEntity.status(code).body(response(body))
        }

    }

    init {
        assert(data.isPresent && error.isEmpty)
        assert(data.isEmpty && error.isPresent)
    }

}

