package idel.infrastructure.repositories

import idel.domain.*
import java.util.*

data class PersistsUser(
    override val id: UUID,
    override val externalId: String,
    override val email: String,
    override val displayName: String,
    override val avatar: String,
    override val roles: Set<String>,
    override val subscriptionPlan: SubscriptionPlan
) : User {


    companion object {
        fun of(user: User): PersistsUser {
            return PersistsUser(
                id = user.id,
                externalId = user.externalId,
                email = UserInfo.normalizeEmail(user.email),
                displayName = user.displayName,
                avatar = user.avatar,
                roles = user.roles,
                subscriptionPlan = user.subscriptionPlan
            )
        }
    }
}

/*
class UserCouchbaseRepository(
    cluster: Cluster,
    collection: Collection
) :
    AbstractTypedCouchbaseRepository<User>(
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

    override fun load(id: UUID): Either<Exception, User> {
        TODO("Only in PG Repository")
    }

    override fun add(user: User): Either<Exception, User> {
       TODO("migrated to PG")
    }


    override fun update(user: User): Either<Exception, User> {
        return mutate(user.id.toString()) {PersistsUser.of(user)}
    }

    override fun load(usernameFilter: Option<String>, pagination: Repository.Pagination): Either<Exception, List<User>> {
        val filterQueryParts = mutableListOf<String>()
        val params = JsonObject.create()

        if (usernameFilter is Some) {
            filterQueryParts.add("""
                (CONTAINS(UPPER(ie.displayName), UPPER(${'$'}username)) or 
                 CONTAINS(UPPER(ie.email), UPPER(${'$'}username)))
            """.trimIndent())
            params.put("username", usernameFilter.value)
        }

        return super.load(
            filterQueryParts = filterQueryParts,
            ordering = "ie.displayName ASC",
            params = params,
            pagination = pagination,
            useFulltextSearch = false
        )
    }


    override fun enrichIdeas(ideas: List<Idea>, maxVoters: Int): Either<Exception, Set<User>> {
        val usersIds: Set<UserId> = ideas.flatMap {idea ->
            val votersCount = min(idea.voters.size, maxVoters)
            setOf(idea.assignee, idea.author) + idea.voters.subList(0, votersCount)
        }
            .filterNotNull()
            .toSet()

        val users = mutableSetOf<User>()
        for (userId in usersIds) {
            when (val eUser = load(userId)) {
                is Either.Left -> when (val ex = eUser.value) {
                    is EntityNotFoundExp -> {
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
*/