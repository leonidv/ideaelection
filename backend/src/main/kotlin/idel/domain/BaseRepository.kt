package idel.domain

import arrow.core.Either
import com.couchbase.client.java.Collection
import com.couchbase.transactions.AttemptContext
import com.fasterxml.jackson.databind.node.ObjectNode
import mu.KLogger
import java.util.*

interface BaseRepository<T : Identifiable> {
    /**
     * Add entity to collection
     */
    fun add(entity: T): Either<Exception, T>

    /**
     * Load entity by id.
     */
    fun load(id: String): Either<Exception, T>

    /**
     * Delete entity from storage.
     */
    fun delete(id : String): Either<Exception, Unit>

    /**
     * Check exists entity or not without loading full document.
     */
    fun exists(id: String): Either<Exception, Boolean>

    /**
     * Replace entity by id with mutation.
     */
    fun mutate(id: String, maxAttempts: Int = 3, mutation: (entity: T) -> T): Either<Exception, T>

    /**
     * Replace entity if it is possible.
     */
    fun possibleMutate(id: String, maxAttempts: Int = 3, mutation: (entity: T) -> Either<Exception, T>): Either<Exception, T>
}

/**
 * Should be removed after a migration to PostgreSQL
 */
interface CouchbaseTransactionBaseRepository<T : Identifiable> {
    val log : KLogger

    fun collection() : Collection

    fun entityToJsonObject(entity: T) : ObjectNode

    fun add(entity : T, ctx: AttemptContext) : Either<Exception,Unit> {
        return try {
            val jsonObject = entityToJsonObject(entity)
            log.trace {"transaction add start: ${ctx.transactionId()}, entity: ${jsonObject.toPrettyString()}"}
            ctx.insert(collection(), entity.id, jsonObject)
            log.trace {"transaction add finished: ${ctx.transactionId()}, entity: ${jsonObject.toPrettyString()}"}
            Either.Right(Unit)
        } catch (e: Exception) {
            Either.Left(e)
        }
    }
}


/**
 * Commons abstractions for repositories
 */
object Repository {
    data class Pagination(val first: Int = 0, val last: Int = 10) {
        val limit = last - first
    }

    val ONE_ELEMENT = Pagination(0, 1)


    /**
     * Simple conversion that based on enum value name: <FIELD>_<ASC|DESC>.
     * For example, "CTIME_ASC" will be converted to "ctime asc"
     */
    fun <E : Enum<E>> enumAsOrdering(e: E): String = e.name.lowercase(Locale.getDefault()).replace('_', ' ')

}

