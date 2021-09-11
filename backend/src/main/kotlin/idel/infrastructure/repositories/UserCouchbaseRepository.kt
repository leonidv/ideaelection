package idel.infrastructure.repositories

import arrow.core.Either
import arrow.core.Option
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Collection
import com.couchbase.client.java.json.JsonObject
import idel.domain.*
import mu.KotlinLogging
import kotlin.math.E
import kotlin.math.max
import kotlin.math.min

data class PersistsUser(
    override val id: String,
    override val email: String,
    override val displayName: String,
    override val avatar: String,
    override val roles: Set<String>,
) : User {


    companion object {
        fun of(user: User): PersistsUser {
            return PersistsUser(
                id = user.id,
                email = user.email,
                displayName = user.displayName,
                avatar = user.avatar,
                roles = user.roles
            )
        }
    }
}

class UserCouchbaseRepository(
    cluster: Cluster,
    collection: Collection
) :
    AbstractTypedCouchbaseRepository<PersistsUser>(
        cluster,
        collection,
        TYPE,
        PersistsUser::class.java,
        ignoredFields = setOf("domain")
    ),
    UserRepository {
    companion object {
        const val TYPE = "user"
    }

    override val log = KotlinLogging.logger {}

    override fun add(user: User) {
        val persistsUser = PersistsUser.of(user)
        collection.insert(persistsUser.id, persistsUser, insertOptions())
    }


    override fun update(user: User): Either<Exception, User> {
        return mutate(user.id) {PersistsUser.of(user)}
    }

    override fun load(first: Int, last: Int): List<User> {
        val limit = last - first
        val params = JsonObject.create()
            .put("offset", first)
            .put("limit", limit)

        val queryString = "select * from `ideaelection` as ie " +
                "where _type = \"${this.type}\" " +
                "order by displayName offset \$offset limit \$limit"

        log.trace {"query: [$queryString], params: [$params]"}


        val q = cluster.query(
            queryString,
            queryOptions(params).readonly(true)
        )

        return q.rowsAs(this.typedClass)
    }

    override fun loadByGroup(
        groupId: String,
        pagination: Repository.Pagination,
        usernameFilter: Option<String>
    ): Either<Exception, List<User>> {
        throw NotImplementedError()
    }


    override fun enrichIdeas(ideas: List<Idea>, maxVoters: Int): Either<Exception, Set<User>> {
        val usersIds: Set<UserId> = ideas.flatMap {idea ->
            val votersCount = min(idea.voters.size, maxVoters)
            setOf(idea.assignee, idea.author) + idea.voters.subList(0, votersCount)
        }
            .filterNot {it.isEmpty()}
            .toSet()

        val users = mutableSetOf<User>()
        for (userId in usersIds) {
            when (val eUser = load(userId)) {
                is Either.Left -> when (val ex = eUser.value) {
                    is EntityNotFound -> {
                        log.debug {ex.message}
                    } // ignore users which are not found
                    else -> return eUser
                }
                is Either.Right -> {
                    users.add(eUser.value)
                }
            }
        }

        return Either.Right(users)
    }
}