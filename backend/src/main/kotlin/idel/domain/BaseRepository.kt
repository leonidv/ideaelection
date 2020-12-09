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
    data class Pagination(val first: Int = 0, val last: Int = 10) {
        val limit = last - first
    }


    /**
     * Simple conversion that based on enum value name: <FIELD>_<ASC|DESC>.
     * For example, "CTIME_ASC" will be converted to "ctime asc"
     */
    fun <E : Enum<E>> enumAsOrdering(e: E): String = e.name.toLowerCase().replace('_', ' ')

}

