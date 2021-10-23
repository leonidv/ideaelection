package idel.infrastructure.repositories

import arrow.core.Either
import arrow.core.Some
import arrow.core.getOrElse
import arrow.core.some
import com.couchbase.client.core.error.DecodingFailureException
import com.couchbase.client.core.error.DocumentNotFoundException
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Collection
import com.couchbase.client.java.codec.JsonTranscoder
import com.couchbase.client.java.json.JsonObject
import com.couchbase.client.java.kv.GetOptions
import idel.domain.*
import mu.KotlinLogging
import java.util.*

class IdeaCouchbaseRepository(
    cluster: Cluster,
    collection: Collection
) : AbstractTypedCouchbaseRepository<Idea>(cluster, collection, TYPE, Idea::class.java), IdeaRepository {

    companion object {
        const val TYPE = "idea"
    }

    override val log = KotlinLogging.logger {}

    override fun load(id: String): Either<Exception, Idea> {
        val idea = super.load(id)
        return when (idea) {
            is Either.Left -> idea
            is Either.Right -> {
                if (!idea.value.deleted) {
                    idea
                } else {
                    Either.Left(EntityNotFound(TYPE, id))
                }
            }
        }
    }

    override fun exists(id: String): Either<Exception, Boolean> {
        return load(id).map {true}
    }

    override fun load(
        groupId: String,
        ordering: IdeaOrdering,
        filtering: IdeaFiltering,
        pagination: Repository.Pagination
    ): Either<Exception, List<Idea>> {
        val orderingValue = when (ordering) {
            IdeaOrdering.CTIME_ASC -> "ctime asc"
            IdeaOrdering.CTIME_DESC -> "ctime desc"
            IdeaOrdering.VOTES_DESC -> "ARRAY_COUNT(voters) desc"
        }

        val params = JsonObject.create();

        val filters = listOf(
            Some("groupId" to groupId),
            filtering.assignee.map {"assignee" to it},
            filtering.offeredBy.map {"offeredBy" to it},
            filtering.implemented.map {"implemented" to it},
            Some("deleted" to filtering.deleted),
            Some("archived" to filtering.archived)
        )
            .filter {it.isDefined()}
            .map {(it as Some).value}

        filters.forEach {(field, value) ->
            params.put(field, value)
        }


        var filterQueryParts = filters.map {(field, _) ->
            "$field = \$${field}"  //should get [name == \$name], for example
        }

        if (filtering.text is Some) {
            val filterValue = filtering.text.value
            params.put("text", filterValue)
            filterQueryParts = filterQueryParts + """SEARCH(ie, ${'$'}text, {"index":"idea_fts"})"""
        }

        return load(
            filterQueryParts = filterQueryParts,
            ordering = orderingValue,
            params = params,
            pagination = pagination,
            useFulltextSearch = filtering.text.map {true}.getOrElse {false}
        )
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

