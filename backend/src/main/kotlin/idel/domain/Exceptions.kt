package idel.domain

import io.konform.validation.ValidationError

/**
 * User can permissions for operation
 */
class OperationNotPermitted : Exception()

/**
 * Represent exception which are thrown on the validation fail.
 */
class ValidationException(msg: String, val errors: Collection<ValidationError>)
    : RuntimeException("$msg, errors = $errors") {
}

/**
 * Usually indicate that required entity (or value object) is not exists.
 */
class EntityNotFound(entityType : String, id : String) : IllegalArgumentException("Entity is not exists, type=[$entityType], id = [$id] ")

/**
 * Usually indicate that creation of new entity (or value object) is failed, because id is not unique.
 */
class EntityAlreadyExists(entityType: String, id : String) : IllegalArgumentException("Entity already exists, type=[$entityType], id = [$id]")

/**
 *  Required operation is invalid because some of the precondition is failed checks.
 */
class InvalidOperation(msg : String) : Exception(msg)