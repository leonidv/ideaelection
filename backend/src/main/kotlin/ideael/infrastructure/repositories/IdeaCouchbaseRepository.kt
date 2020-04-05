package ideael.infrastructure.repositories

import arrow.core.Either
import com.couchbase.client.core.error.CasMismatchException
import com.couchbase.client.core.error.DecodingFailureException
import com.couchbase.client.core.error.DocumentNotFoundException
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Collection
import com.couchbase.client.java.codec.*
import com.couchbase.client.java.json.JsonObject
import com.couchbase.client.java.kv.GetOptions
import com.couchbase.client.java.kv.InsertOptions
import com.couchbase.client.java.kv.ReplaceOptions
import com.couchbase.client.java.query.QueryOptions
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import ideael.domain.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * A little sugar. Use it when call [Cluster.query].
 */
fun JsonObject.asQueryOptions(readonly: Boolean = true): QueryOptions {
    return QueryOptions.queryOptions()
        .parameters(this)
        .readonly(readonly)
}

@Repository
class IdeaCouchbaseRepository(
    private val cluster: Cluster,
    private val collection: Collection
) : IdeaRepository {

    private val log: Logger = LoggerFactory.getLogger(IdeaCouchbaseRepository::class.java)

    val mapper = initMapper();

    val jsonSerializer = TypedJsonSerializer(
        mapper =  mapper,
        rootName = "ie",
        type = "idea",
        typedClass =  Idea::class.java)

    private val transcoder: JsonTranscoder = JsonTranscoder.create(jsonSerializer)

    /**
     * Configure mapper to user custom Voter serializer
     */
    private fun initMapper(): ObjectMapper {
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


    override fun add(idea: Idea): Idea {
        val options = InsertOptions.insertOptions().transcoder(transcoder)
        collection.insert(idea.id.toString(), idea, options)
        return idea
    }


    /**
     * Update idea's information.
     *
     * Return Left(Nothing) if 3 attempts of updating is not working.
     */
    override fun updateInfo(id: String, info: IdeaInfo): Either<Unit, Idea> {
        lateinit var idea: Idea
        var canUpdate: Boolean
        var attempts = 0

        do {
            val maybeOriginIdea = this.load(id)
            if (maybeOriginIdea.isEmpty) {
                log.warn("Can't load document updateInfo(id=[$id])")
                return Either.left(Unit)
            }


            val originIdea = maybeOriginIdea.get().idea
            val originVersion = maybeOriginIdea.get().version

            val options = ReplaceOptions.replaceOptions()
                .transcoder(transcoder)
                .cas(originVersion)

            idea = originIdea.copy(
                title = info.title,
                description = info.description,
                link = info.link
            )

            canUpdate =
                try {
                    collection.replace(idea.id, idea, options)
                    true
                } catch (ex: CasMismatchException) {
                    false
                }

            attempts++
        } while (attempts >= 3 && !canUpdate)

        return if (canUpdate) {
            Either.right(idea)
        } else {
            Either.left(Unit)
        }
    }

    override fun replace(ideaWithVersion: IdeaWithVersion): Either<IdeaWithVersion, Unit> {
        val (idea, version) = ideaWithVersion

        val options =
            ReplaceOptions
                .replaceOptions()
                .transcoder(transcoder)
                .cas(version)
        return try {
            collection.replace(idea.id, idea, options)
            Either.right(Unit)
        } catch (ex: CasMismatchException) {
            val currentIdeaWithVersion = load(idea.id)
            Either.left(currentIdeaWithVersion.get())
        }
    }

    override fun load(
        first: Int,
        last: Int,
        sorting: IdeaSorting,
        filtering: IdeaFiltering
    ): List<Idea> {
        val limit = last - first
        val params = JsonObject.create()
            .put("offset", first)
            .put("limit", limit)

        val ordering = when (sorting) {
            IdeaSorting.CTIME_ASC -> "ctime asc"
            IdeaSorting.CTIME_DESC -> "ctime desc"
            IdeaSorting.VOTES_DESC -> "ARRAY_COUNT(voters) desc"
        }

        val filters = listOf(
            filtering.assignee.map { "assignee" to it },
            filtering.offeredBy.map { "offeredBy" to it },
            filtering.implemented.map { "implemented" to it }
        )
            .filter { it.isPresent }
            .map { it.get() }

        filters.forEach { (field, value) ->
            params.put(field, value)
        }


        var filterQueryParts = filters.map { (field, value) ->
            "$field = \$${field}"  //should get [name == \$name], for example
        }

        if (filtering.text.isPresent) {
            val filterValue = filtering.text.get()
            params.put("text", filterValue)
            filterQueryParts = filterQueryParts + """SEARCH(ie, ${'$'}text, {"index":"idea_fts"})"""
        }


        val filterQuery =
            if (filterQueryParts.isNotEmpty()) {
                filterQueryParts.joinToString(prefix = " and ", separator = " and ")
            } else {
                ""
            }

        val options = params.asQueryOptions()
            .serializer(jsonSerializer)
            .timeout(Duration.ofSeconds(2))

        val queryString = "select * from `ideaelection` as ie " +
                "where _type = \"idea\" $filterQuery " +
                "order by $ordering offset \$offset limit \$limit"

        if (log.isTraceEnabled) {
            log.trace("query: [$queryString], params: [$params]")
        }

        val q = cluster.query(
            queryString,
            options
        )



        return q.rowsAs(Idea::class.java)
    }

    override fun load(id: String): Optional<IdeaWithVersion> {
        val transcoder = JsonTranscoder.create(jsonSerializer)
        val getOptions = GetOptions.getOptions().transcoder(transcoder)

        val result = try {
            collection.get(id, getOptions)
        } catch (e: DocumentNotFoundException) {
            return Optional.empty()
        }

        return try {
            val idea = result.contentAs(Idea::class.java)
            val cas = result.cas()
            if (null != idea) {
                Optional.of(IdeaWithVersion(idea, cas))
            } else {
                Optional.empty()
            }
        } catch (e: DecodingFailureException) {
            log.warn("Can't decode idea, id=[$id], result = [${result.toString()}]")
            throw e
        }
    }
}

