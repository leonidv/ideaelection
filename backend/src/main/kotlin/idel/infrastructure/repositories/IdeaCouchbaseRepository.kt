package idel.infrastructure.repositories

import arrow.core.Either
import com.couchbase.client.core.error.CasMismatchException
import com.couchbase.client.core.error.DecodingFailureException
import com.couchbase.client.core.error.DocumentNotFoundException
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Collection
import com.couchbase.client.java.codec.*
import com.couchbase.client.java.json.JsonObject
import com.couchbase.client.java.kv.GetOptions
import com.couchbase.client.java.kv.ReplaceOptions
import com.couchbase.client.java.query.QueryOptions
import idel.domain.*
import mu.KotlinLogging
import java.time.Duration
import java.util.*

/**
 * A little sugar. Use it when call [Cluster.query].
 */
fun JsonObject.asQueryOptions(readonly: Boolean = true): QueryOptions {
    return QueryOptions.queryOptions()
        .parameters(this)
        .readonly(readonly)
}


class IdeaCouchbaseRepository(
        cluster: Cluster,
        collection: Collection
) : AbstractTypedCouchbaseRepository<Idea>(cluster, collection, "idea", Idea::class.java), IdeaRepository {

    override val log = KotlinLogging.logger {}
    /**
     * Update idea's information.
     *
     * Return Left(Nothing) if 3 attempts of updating is not working.
     */
    override fun updateInfo(id: String, info: IIdeaEditableProperties): Either<Exception, Idea> {
        return safelyReplace(id) {
            it.update(
                    title = info.title,
                    description = info.description,
                    link = info.link
            )
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
            val currentIdeaWithVersion = loadWithVersion(idea.id)
            Either.left(currentIdeaWithVersion.get())
        }
    }

    override fun loadWithVersion(
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
                filtering.assignee.map {"assignee" to it},
                filtering.offeredBy.map {"offeredBy" to it},
                filtering.implemented.map {"implemented" to it}
        )
            .filter {it.isPresent}
            .map {it.get()}

        filters.forEach {(field, value) ->
            params.put(field, value)
        }


        var filterQueryParts = filters.map {(field, _) ->
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
                "where _type = \"${this.type}\" $filterQuery " +
                "order by $ordering offset \$offset limit \$limit"


        log.trace {"query: [$queryString], params: [$params]"}


        val q = cluster.query(
                queryString,
                options
        )



        return q.rowsAs(Idea::class.java)
    }

    override fun loadWithVersion(id: String): Optional<IdeaWithVersion> {
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

