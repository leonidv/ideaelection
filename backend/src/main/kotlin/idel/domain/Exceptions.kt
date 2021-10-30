package idel.domain

import arrow.core.Either
import io.konform.validation.ValidationError

open class NoStacktraceRuntimeException(msg: String) : java.lang.RuntimeException(msg, null, false, false)

/**
 * User can permissions for operation
 */
class OperationNotPermitted : NoStacktraceRuntimeException("")

/**
 * Represent exception which are thrown on the validation fail.
 */
class ValidationException(msg: String, val errors: Collection<ValidationError>) :
    NoStacktraceRuntimeException("$msg, errors = $errors") {
}

/**
 * Usually indicate that required entity (or value object) is not exists.
 */
class EntityNotFound(entityType: String, id: String) :
    IllegalArgumentException("Entity is not exists, type=[$entityType], id = [$id] ")

/**
 * Convert Either<EntityNotFound,E> into Either.Right(null).
 *
 * Use this method if you want obviously process  a case "entity is not found in storage".
 */
fun <T> notFoundToNull(e: Either<Exception, T>): Either<Exception, T?> {
    return if ((e is Either.Left) && (e.value is EntityNotFound)) {
        Either.Right(null)
    } else {
        e
    }
}

/**
 * Usually indicate that creation of new entity (or value object) is failed, because id is not unique.
 */
class EntityAlreadyExists(entityType: String, id: String) :
    IllegalArgumentException("Entity already exists, type=[$entityType], id = [$id]")

/**
 *  Required operation is invalid because some of the precondition is failed checks.
 */
class InvalidOperation(msg: String) : NoStacktraceRuntimeException(msg)

sealed class EntityReadOnly(msg: String) : NoStacktraceRuntimeException(msg)
class EntityLogicallyDeleted : EntityReadOnly("Entity is deleted")
class EntityArchived : EntityReadOnly("Entity is archived")