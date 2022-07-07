package idel.infrastructure.repositories.psql

import arrow.core.Either
import arrow.core.flatten
import idel.domain.*
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.insert
import org.postgresql.util.PSQLException

data class UniqueViolationDetails(val key: String, val value: String)

val uniqueViolationDetailsRegex = """.*?\((.*?)\)=\((.*?)\).*?""".toRegex()
fun parseUniqueViolation(message: String): Sequence<UniqueViolationDetails> {
    val matchResult = uniqueViolationDetailsRegex.findAll(message)

    return matchResult
        .map {it.groupValues}
        .map {UniqueViolationDetails(key = it[1], value = it[2])}
}



fun ExposedSQLException.asPersistenceError(): PersistenceError {
    val cause = this.cause
    return if (cause !is PSQLException) {
        PersistenceUndefinedError(this)
    } else {
        val error = when (cause.sqlState) {
            "23505" -> {
                val details = parseUniqueViolation(cause.message ?: "").joinToString {"${it.key} = ${it.value}"}
                EntityAlreadyExists("Entity is already exists, non unique values: $details")
            }
            else -> PersistenceUndefinedError(this)
        }

        error
    }
}

/**
 * Wrap possible executions of statement execution into [DomainError].
 *
 * Use this method for cases like load entity by id, when result is also mapped to [DomainError]. For example, when
 * you load entity by id, you should map ```null``` to [EntityNotFound]
 */
fun <T> wrappedSQLStatementFlatten(action: () -> Either<DomainError,T>) : Either<DomainError,T> {
    return wrappedSQLStatement(action).flatten()
}

/**
 * Wrap possible executions of statement execution into [DomainError].
 *
 */
fun <T> wrappedSQLStatement(action: () -> T): Either<DomainError, T> {
    return try {
        val result = action();
        Either.Right(result)
    } catch (e: ExposedSQLException) {
        Either.Left(e.asPersistenceError())
    } catch (e: PSQLException) {
        Either.Left(e.asError())
    } catch (e: Exception) {
        Either.Left(e.asError())
    }
}

/**
 * Use this method for strong update (when not updated any entities is error).
 */
fun <T> zeroAsNotFound(count : Int, entity: T, entityClass: String, id: Any): Either<DomainError,T> {
    return if (count > 0) {
        Either.Right(entity)
    } else {
        Either.Left(EntityNotFound(entityClass, id))
    }
}