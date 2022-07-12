package idel.domain

import arrow.core.Either
import mu.KLogger
import mu.KotlinLogging
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction

private val log = KotlinLogging.logger("saedi.ftransation")

/**
 * Functional transaction. Rollback transaction if result of statement is [Either.Left]
 * Always reuse current transaction if it present.
 */
fun <T> fTransaction(
    db: Database? = null,
    statement: Transaction.() -> Either<DomainError, T>
): Either<DomainError, T> {
    try {
        val alreadyInTransaction = TransactionManager.currentOrNull() != null
        return if (alreadyInTransaction) {
            statement(TransactionManager.current())
        } else {
            return transaction(db) {
                val result = statement()
                result.tapLeft {rollback()}
            }
        }
    } catch (e: Exception) {
        log.error(e) {"Can't execute transaction, probably developer error (statement should wrap any exception and convert into Either.Left). ex.msg = ${e.message} "}
        throw e
    }
}