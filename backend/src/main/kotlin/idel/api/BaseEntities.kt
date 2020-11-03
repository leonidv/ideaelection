package idel.api


/**
 * Commons abstractions for repositories
 */
object Repository {
    data class Pagination(val first : Int, val last : Int) {
        val limit = last - first
    }
}

