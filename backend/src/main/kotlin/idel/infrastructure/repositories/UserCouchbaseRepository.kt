package idel.infrastructure.repositories

import arrow.core.Either
import arrow.core.Option
import com.couchbase.client.core.error.DocumentNotFoundException
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Collection
import com.couchbase.client.java.codec.JsonTranscoder
import com.couchbase.client.java.json.JsonObject
import com.couchbase.client.java.kv.GetOptions
import com.couchbase.client.java.kv.InsertOptions
import idel.domain.Idea
import idel.domain.IdeaSorting
import idel.domain.User
import idel.domain.UserRepository
import mu.KLogger
import mu.KotlinLogging
import org.springframework.stereotype.Repository
import java.time.Duration
import java.util.*

data class PersistsUser(val id: String,
                   override val email: String,
                   override val displayName: String,
                   override val avatar: String,
                   override val roles: Set<String>) : User {
    override fun id() = id;

    companion object {
        fun of(user: User): PersistsUser {
            return PersistsUser(
                    id = user.id(),
                    email = user.email,
                    displayName = user.displayName,
                    avatar = user.avatar,
                    roles = user.roles
            )
        }
    }
}

@Repository
class UserCouchbaseRepository(
        cluster: Cluster,
        collection: Collection) :
        AbstractTypedCouchbaseRepository<PersistsUser>(cluster, collection, "user", PersistsUser::class.java),
        UserRepository {

    val log = KotlinLogging.logger {}

    override fun load(id: String): Option<PersistsUser> {
        val result = try {
            val getResult = collection.get(id, getOptions())
            Option.fromNullable(getResult.contentAs(PersistsUser::class.java))
        } catch (e: DocumentNotFoundException) {
            return Option.empty()
        }

        return result;
    }

    override fun add(user: User) {
        val persistsUser = PersistsUser.of(user)
        collection.insert(persistsUser.id, persistsUser, insertOptions())
    }


    override fun update(user: User): Either<Exception, User> {
        return safelyReplace(user.id()) { PersistsUser.of(user) }
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
}