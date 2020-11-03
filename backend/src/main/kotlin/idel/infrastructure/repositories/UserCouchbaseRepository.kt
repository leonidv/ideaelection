package idel.infrastructure.repositories

import arrow.core.Either
import arrow.core.Left
import arrow.core.Some
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Collection
import com.couchbase.client.java.json.JsonObject
import idel.domain.*
import mu.KotlinLogging

data class PersistsUser(
        override val id: String,
        override val email: String,
        override val displayName: String,
        override val avatar: String,
        override val roles: Set<String>) : User {


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
        collection: Collection) :
        AbstractTypedCouchbaseRepository<PersistsUser>(cluster, collection, "user", PersistsUser::class.java),
        UserRepository {

    override val log = KotlinLogging.logger {}

    override fun add(user: User) {
        val persistsUser = PersistsUser.of(user)
        collection.insert(persistsUser.id, persistsUser, insertOptions())
    }


    override fun update(user: User): Either<Exception, User> {
        return safelyReplace(user.id) {PersistsUser.of(user)}
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

    override fun loadUserInfo(ids: List<UserId>): Either<Exception, List<UserInfo>> {
        val users = mutableListOf<UserInfo>()
        for (id in ids) {
            when (val eUser = load(id)) {
                is Either.Left -> return eUser
                is Either.Right -> {
                    val oUser = eUser.b
                    if (oUser is Some) {
                       users.add(UserInfo.ofUser(oUser.t))
                    }
                }
            }
        }
        return Either.right(users)
    }
}