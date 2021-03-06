package idel.infrastructure.repositories

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.flatten
import com.couchbase.client.core.error.CasMismatchException
import com.couchbase.client.core.error.DocumentExistsException
import com.couchbase.client.core.error.DocumentNotFoundException
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Collection
import com.couchbase.client.java.codec.JsonTranscoder
import com.couchbase.client.java.json.JsonObject
import com.couchbase.client.java.kv.GetOptions
import com.couchbase.client.java.kv.InsertOptions
import com.couchbase.client.java.kv.ReplaceOptions
import com.couchbase.client.java.query.QueryOptions
import com.couchbase.client.java.query.QueryScanConsistency
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import idel.domain.*
import mu.KLogger
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


fun Repository.Pagination.queryPart(): String {
    return """offset ${this.first} limit ${this.limit}"""
}

/**
 * Contains support function for using [TypedJsonSerializer]
 */
abstract class AbstractTypedCouchbaseRepository<T : Identifiable>(
    protected val cluster: Cluster,
    protected val collection: Collection,
    protected val type: String,
    protected val typedClass: Class<T>
) : BaseRepository<T> {

    abstract val log: KLogger

    protected val bucketName : String = collection.bucketName()

    protected val mapper = initMapper();


    protected val jsonSerializer = TypedJsonSerializer(
        mapper = mapper,
        rootName = "ie",
        type = type,
        typedClass = typedClass
    )

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
            .setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE)


    }

    protected fun traceRawQuery(query: String, params: JsonObject) {
        log.trace {"\nquery:  [$query], \nparams:  [$params]"}
    }

    protected fun <X>
            safelyKeyOperation(id: String, action: () -> X): Either<Exception, X> {
        return try {
            Either.Right(action())
        } catch (e: DocumentNotFoundException) {
            Either.Left(EntityNotFound(type, id))
        } catch (e: DocumentExistsException) {
            Either.Left(EntityAlreadyExists(type, id))
        } catch (e: Exception) {
            Either.Left(e)
        }
    }

    protected fun <X> safely(action: () -> Either<Exception, X>): Either<Exception, X> {
        return try {
            return action();

        } catch (e: Exception) {
            Either.Left(e)
        }
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
            .scanConsistency(QueryScanConsistency.REQUEST_PLUS)
            .serializer(jsonSerializer)

    /**
     * Return [InsertOptions] with [transcoder]
     */
    protected fun insertOptions(): InsertOptions =
        InsertOptions
            .insertOptions()
            .transcoder(transcoder)

    /**
     * Return [ReplaceOptions] with [transcoder]
     */
    protected fun replaceOptions(): ReplaceOptions = ReplaceOptions.replaceOptions().transcoder(transcoder)


    /**
     * Replace entity by id with mutation.
     */
    override fun mutate(id: String, maxAttempts: Int, mutation: (entity: T) -> T): Either<Exception, T> {
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
                        Either.Right(newEntity)
                    } catch (e: CasMismatchException) {
                        Either.Left(e)
                    }
            } catch (e: Exception) {
                return Either.Left(e)
            }

            attempts++
        } while (attempts >= maxAttempts && canUpdate.isLeft())
        return canUpdate
    }

    override fun possibleMutate(
        id: String,
        maxAttempts: Int,
        mutation: (entity: T) -> Either<Exception, T>
    ): Either<Exception, T> {
        lateinit var eUpdatedEntity: Either<Exception, T>
        var attempts = 0

        do {
            eUpdatedEntity = safelyKeyOperation(id) {
                val getResult = collection.get(id, getOptions())
                val originEntity = getResult.contentAs(typedClass)
                val replaceOptions = replaceOptions().cas(getResult.cas())

                mutation(originEntity).flatMap {newEntity: T ->
                    try {
                        collection.replace(id, newEntity, replaceOptions)
                        Either.Right(newEntity)
                    } catch (e: CasMismatchException) {
                        Either.Left(e)
                    }
                }
            }.flatten()

            val casError = (eUpdatedEntity is Either.Left) && (eUpdatedEntity.value is CasMismatchException)

        } while (attempts >= maxAttempts && casError)
        return eUpdatedEntity
    }

    /**
     * Add entity to collection
     */
    override fun add(entity: T): Either<Exception, T> {
        return safelyKeyOperation(entity.id) {
            collection.insert(entity.id, entity, insertOptions())
            entity
        }
    }

    /**
     * Load entity by id.
     */
    override fun load(id: String): Either<Exception, T> {
        return safelyKeyOperation(id) {
            val result = collection.get(id, getOptions())
            val entity = result.contentAs(typedClass)
            entity
        }
    }

    /**
     * Check exists entity or not without loading full document.
     */
    override fun exists(id: String): Either<Exception, Boolean> {
        return safelyKeyOperation(id) {
            collection.exists(id).exists()
        }
    }

    /**
     * Remove entity from collection.
     */
    open fun remove(id: String): Either<Exception, Unit> {
        return safelyKeyOperation(id) {
            collection.remove(id)
            Unit
        }

    }

    /**
     * Replace object without CAS checking.
     */
    open fun replace(entity: T): Either<Exception, T> {
        return safelyKeyOperation(entity.id) {
            collection.replace(entity.id, entity, replaceOptions())
            entity
        }
    }

    protected fun load(
        filterQueryParts: List<String>,
        ordering: String, params: JsonObject,
        pagination: Repository.Pagination,
        useFulltextSearch: Boolean = false
    ): Either<Exception, List<T>> {
        return rawLoad(
            basePart = """select * from `$bucketName` as ie where _type="${this.type}" """,
            filterQueryParts = filterQueryParts,
            ordering = ordering,
            params = params,
            pagination = pagination,
            useFulltextSearch
        )
    }


    /**
     * Low level form of [load] function. Use this if you need customize ```select * from ... where _type = ...```.
     */
    protected fun rawLoad(
        basePart: String,
        filterQueryParts: List<String>,
        ordering: String,
        params: JsonObject,
        pagination: Repository.Pagination,
        useFulltextSearch: Boolean = false
    ): Either<Exception, List<T>> {
        return try {
            val filterQuery =
                if (filterQueryParts.isNotEmpty()) {
                    filterQueryParts.joinToString(prefix = " and ", separator = " and ", postfix = " ")
                } else {
                    " "
                }

            val options = queryOptions(params).readonly(true)

            if (useFulltextSearch) {
                options.scanConsistency(QueryScanConsistency.NOT_BOUNDED)
                log.trace {"use fts index, QueryScanConsistency is ${QueryScanConsistency.NOT_BOUNDED}"}
            }

            val orderingPart = if (ordering.isNotEmpty()) {
                " order by $ordering "
            } else {
                ""
            }

            val queryString = "$basePart \n $filterQuery \n $orderingPart \n ${pagination.queryPart()}"
            traceRawQuery(queryString, params)

            Either.Right(cluster.query(queryString, options).rowsAs(typedClass))
        } catch (e: Exception) {
            Either.Left(e)
        }
    }

}