package idel.infrastructure.repositories

import arrow.core.Either
import com.couchbase.client.core.error.CasMismatchException
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Collection
import com.couchbase.client.java.codec.JsonTranscoder
import com.couchbase.client.java.json.JsonObject
import com.couchbase.client.java.kv.GetOptions
import com.couchbase.client.java.kv.InsertOptions
import com.couchbase.client.java.kv.ReplaceOptions
import com.couchbase.client.java.query.QueryOptions
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import mu.KotlinLogging
import java.lang.Exception
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Contains support function for using [TypedJsonSerializer]
 */
abstract class AbstractTypedCouchbaseRepository<T>(
        protected val cluster: Cluster,
        protected val collection: Collection,
        protected val type: String,
        protected val typedClass: Class<T>

) {

    private val log = KotlinLogging.logger {}

    protected val mapper = initMapper();

    protected val jsonSerializer = TypedJsonSerializer(
            mapper = mapper,
            rootName = "ie",
            type = type,
            typedClass = typedClass)

    protected val transcoder: JsonTranscoder = JsonTranscoder.create(jsonSerializer)

    /**
     * Configure mapper to user custom Voter serializer
     */
    protected fun initMapper(): ObjectMapper {
        val timeModule = JavaTimeModule()
        timeModule.addSerializer(
                LocalDateTime::class.java,
                LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
        timeModule.addDeserializer(
                LocalDateTime::class.java,
                LocalDateTimeDeserializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )

        return jacksonObjectMapper()
            .registerModule(timeModule)
            .registerModule(Jdk8Module())
            .registerModule(ParameterNamesModule())

    }

    /**
     * Return [GetOptions] with [transcoder]
     */
    protected fun getOptions(): GetOptions = GetOptions.getOptions().transcoder(transcoder)

    /**
     * Return QueryOptions with [[params]], 2-seconds timeout and [transcoder]
     */
    protected fun queryOptions(params: JsonObject): QueryOptions =
            QueryOptions
                .queryOptions()
                .parameters(params)
                .timeout(Duration.ofSeconds(2))
                .serializer(jsonSerializer)
    /**
     * Return [InsertOptions] with [transcoder]
     */
    protected fun insertOptions(): InsertOptions = InsertOptions.insertOptions().transcoder(transcoder)

    /**
     * Return [ReplaceOptions] with [transcoder]
     */
    protected fun replaceOptions(): ReplaceOptions = ReplaceOptions.replaceOptions().transcoder(transcoder)

    /**
     * Replace entity by id with mutation.
     */
    protected fun safelyReplace(id: String, maxAttempts: Int = 3, mutation: (entity: T) -> T): Either<Exception, T> {
        lateinit var canUpdate: Either<Exception, T>
        var attempts = 0

        do {
            try {
                val getResult = collection.get(id, getOptions())
                val originEntity = getResult.contentAs(typedClass)
                val replaceOptions = replaceOptions().cas(getResult.cas())
                canUpdate =
                        try {
                            val newEntity = mutation(originEntity)
                            collection.replace(id, newEntity, replaceOptions)
                            Either.right(newEntity)
                        } catch (e: CasMismatchException) {
                            Either.left(e)
                        }
            } catch (e: Exception) {
                return Either.left(e)
            }

            attempts++
        } while (attempts >= maxAttempts && canUpdate.isLeft())
        return canUpdate
    }

    protected fun load(filterQueryParts : List<String>, ordering : String, params : JsonObject, first : Int, last : Int) : List<T> {
        val limit = last - first
        params
            .put("offset", first)
            .put("limit", limit)

        val filterQuery =
                if (filterQueryParts.isNotEmpty()) {
                    filterQueryParts.joinToString(prefix = " and ", separator = " and ")
                } else {
                    ""
                }

        val options = queryOptions(params)
        val queryString = "select * from `ideaelection` as ie " +
                "where _type = \"${this.type}\" $filterQuery " +
                "order by $ordering offset \$offset limit \$limit"

        log.trace {"query: [$queryString], params: [$params]"}

        return cluster.query(queryString, options).rowsAs(typedClass)
    }
}