package idel.domain

/**
 * Commons abstractions for repositories
 */
object Repository {
    data class Pagination(val skip: Int = 0, val count: Int = 10) {
        init {
            require(count < 100) {"Max elements count is 99 per request"}
        }
    }
    val ONE_ELEMENT = Pagination(0, 1)

}

