package idel.domain

import arrow.core.Either
import io.konform.validation.ValidationError

sealed class DomainError {
    abstract val message : String

    override fun toString(): String {
        return "${this::class.simpleName} message"
    }
}

class NotImplemented(className : String, method : String) : DomainError() {
    override val message = "$className.$method(...) is still not implemented"
}

open class InvalidOperation(override val message: String) : DomainError()

sealed class EntityCantBeChanged(msg: String) : InvalidOperation(msg)
object EntityArchived : EntityCantBeChanged("Entity is archived")
object EntityLogicallyDeleted: EntityCantBeChanged("Entity is logically deleted")
object EntityReadOnly : EntityCantBeChanged("Entity in read-only state")

class InvalidArgument(override val message: String): DomainError()

/*
================
Wrappers and convertos
*/
data class ExceptionError(val ex : Throwable) : DomainError() {
    override val message: String= "${ex.javaClass::class.java.name} (${ex.message})"
}

fun Throwable.asError() : ExceptionError = ExceptionError(this)

fun <T> Either<DomainError,T>.isEntityOrNotFound() : Boolean =
    (this is Either.Right) || ((this is Either.Left) && (this.value is EntityNotFound))

fun <T> Either<DomainError,T>.isNotEntityOrNotFound() = !this.isEntityOrNotFound()

/*
==================================
   Persistence Errors
==================================
*/

abstract class PersistenceError : DomainError()

data class EntityNotFound(val type: String, val id: Any) : PersistenceError() {
    override val message: String = "Entity [$type] with id = [$id] is not found"
}

class EntityAlreadyExists(override val message : String) : PersistenceError()

data class PersistenceUndefinedError(val cause : Exception) : PersistenceError() {
    override val message = "Can't recognize SQL cause, cause.msg = [${cause.message}]"
}

fun <T> notFoundToNull(e: Either<DomainError, T>): Either<DomainError, T?> {
    return if ((e is Either.Left) && (e.value is EntityNotFound)) {
        Either.Right(null)
    } else {
        e
    }
}


/*
==================================
   Security Errors
==================================
*/

abstract class SecurityError : DomainError()

class OperationNotPermitted : SecurityError() {
    override val message: String = "operation is not permitted"
}

/*
==================================
   Validation Errors
==================================
*/

class ValidationError(override val message: String, val errors: Collection<ValidationError>) : DomainError()

