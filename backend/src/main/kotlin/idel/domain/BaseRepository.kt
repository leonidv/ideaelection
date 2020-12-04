package idel.domain


/**
 * Commons abstractions for repositories
 */
object Repository {
    data class Pagination(val first : Int = 0, val last : Int = 10) {
        val limit = last - first
    }


    /**
     * Simple conversion that based on enum value name: <FIELD>_<ASC|DESC>.
     * For example, "CTIME_ASC" will be converted to "ctime asc"
     */
    fun <E : Enum<E>> enumAsOrdering(e : E) : String = e.name.toLowerCase().replace('_',' ')

}

