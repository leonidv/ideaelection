package idel.infrastructure.repositories

import arrow.core.Either
import arrow.core.Option
import arrow.core.Right
import arrow.core.Some
import com.couchbase.client.core.error.CasMismatchException
import com.couchbase.client.core.error.DocumentNotFoundException
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
import idel.api.Repository
import idel.domain.Identifiable
import mu.KLogger
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


/**
 * Contains support function for using [TypedJsonSerializer]
 */
abstract class AbstractTypedCouchbaseRepository<T : Identifiable>(
        protected val cluster: Cluster,
        protected val collection: Collection,
        protected val type: String,
        protected val typedClass: Class<T>

) {

    abstract val log: KLogger

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

    /**
     * Add entity to collection
     */
    open fun add(entity: T) : Either<Exception, T> {
        return try {
            collection.insert(entity.id, entity, insertOptions())
            Either.Right(entity)
        } catch (e : Exception) {
            Either.Left(e)
        }
    }

    /**
     * Load entity by id.
     */
    open fun load(id : String) : Either<Exception,Option<T>> {
        return try {
            val result = collection.get(id, getOptions())
            val entity = Some(result.contentAs(typedClass))
            Either.right(entity)
        } catch (e : DocumentNotFoundException) {
            Either.right(Option.empty())
        } catch (e : Exception) {
            Either.left(e)
        }
    }

    /**
     * Check exists entity or not without loading full document.
     */
    open fun exists(groupId: String): Either<Exception, Boolean> {
       return try {
            Either.right(collection.exists(groupId).exists())
        } catch (e : Exception) {
            Either.left(e)
        }
    }


    protected fun load(filterQueryParts : List<String>, ordering : String, params : JsonObject, pagination: Repository.Pagination) : Either<Exception,List<T>> {
       return try {
            params
                .put("offset", pagination.first)
                .put("limit", pagination.limit)

            val filterQuery =
                    if (filterQueryParts.isNotEmpty()) {
                        filterQueryParts.joinToString(prefix = " and ", separator = " and ")
                    } else {
                        ""
                    }

            val options = queryOptions(params).readonly(true)
            val queryString = "select * from `${collection.bucketName()}` as ie " +
                    "where _type = \"${this.type}\" $filterQuery " +
                    "order by $ordering offset \$offset limit \$limit"

            log.trace {"query: [$queryString], params: [$params]"}

            Either.right(cluster.query(queryString, options).rowsAs(typedClass))
        } catch (e :Exception) {
            Either.left(e)
        }
    }
}