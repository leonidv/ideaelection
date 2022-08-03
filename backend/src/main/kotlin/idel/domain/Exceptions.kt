package idel.domain

import arrow.core.Either
import io.konform.validation.ValidationError

open class NoStacktraceRuntimeException(msg: String) : java.lang.RuntimeException(msg, null, false, false)


/**
 * Represent exception which are thrown on the validation fail.
 */
class ValidationException(msg: String, val errors: Collection<ValidationError>) :
    NoStacktraceRuntimeException("$msg, errors = $errors") {
}

/**
 * Usually indicate that required entity (or value object) is not exists.
 */
class EntityNotFoundExp(entityType: String, id: String) :
    IllegalArgumentException("Entity is not exists, type=[$entityType], id = [$id] ")

/**
 * Usually indicate that creation of new entity (or value object) is failed, because id is not unique.
 */
class EntityAlreadyExistsExp(entityType: String, id: String) :
    IllegalArgumentException("Entity already exists, type=[$entityType], id = [$id]")

/**
 *  Required operation is invalid because some of the precondition is failed checks.
 */
class InvalidOperationExp(msg: String) : NoStacktraceRuntimeException(msg)

sealed class EntityReadOnlyExp(msg: String) : NoStacktraceRuntimeException(msg)
class EntityLogicallyDeletedExp : EntityReadOnlyExp("Entity is deleted")
class EntityArchivedExp : EntityReadOnlyExp("Entity is archived")