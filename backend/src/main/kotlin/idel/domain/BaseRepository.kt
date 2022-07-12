package idel.domain

import arrow.core.Either

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
 * Commons abstractions for repositories
 */
object Repository {
    data class Pagination(val skip: Int = 0, val count: Int = 10)

    val ONE_ELEMENT = Pagination(0, 1)

}

