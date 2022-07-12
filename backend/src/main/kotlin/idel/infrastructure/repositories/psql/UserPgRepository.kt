package idel.infrastructure.repositories.psql

import arrow.core.Either
import idel.domain.*
import idel.infrastructure.repositories.PersistsUser
import idel.infrastructure.repositories.psql.exposed.firstOrNotFound
import idel.infrastructure.repositories.psql.exposed.ilike
import idel.infrastructure.repositories.psql.exposed.limit
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*

object UsersTable : UUIDTable(name = "users") {
    val email = varchar("email", 100)
    val externalId = varchar("external_id", 100)
    val displayName = varchar("display_name", 100)
    val avatar = varchar("avatar", 100)
    val superAdmin = bool("super_admin")

    val subscriptionPlan = enumerationByName("subscription_plan", CaseConversion.TO_LOWER, SubscriptionPlan::class)

    fun mapResult(resultRow: ResultRow): User {
        fun calcRoles(isSuperAdmin: Boolean): Set<String> =
            if (isSuperAdmin) {
                setOf(Roles.SUPER_USER)
            } else {
                setOf(Roles.USER)
            }

        return PersistsUser(
            id = resultRow[id].value,
            externalId = resultRow[externalId],
            email = resultRow[email],
            displayName = resultRow[displayName],
            avatar = resultRow[avatar],
            roles = calcRoles(resultRow[superAdmin]),
            subscriptionPlan = resultRow[subscriptionPlan]
        )
    }
}

class UserPgRepository : UserRepository, HasUnimplemented {

    override fun add(user: User): Either<DomainError, User> {
        return wrappedSQLStatement {
            UsersTable.insert {
                it[id] = user.id
                it[email] = user.email
                it[externalId] = user.externalId
                it[displayName] = user.displayName
                it[avatar] = user.avatar
                it[superAdmin] = user.roles.contains(Roles.SUPER_USER)
                it[subscriptionPlan] = user.subscriptionPlan
            }
            user
        }
    }

    override fun update(user: User): Either<DomainError, User> {
        return wrappedSQLStatementFlatten {
            val updated = UsersTable.update({UsersTable.id eq user.id}) {
                it[displayName] = user.displayName
                it[avatar] = user.avatar
                it[superAdmin] = user.roles.contains(Roles.SUPER_USER)
                it[subscriptionPlan] = user.subscriptionPlan
            }

            zeroAsNotFound(updated, user, "user", user.id)
        }
    }

    override fun load(id: UserId): Either<DomainError, User> {
        return wrappedSQLStatementFlatten {
            UsersTable
                .select {UsersTable.id eq id}
                .firstOrNotFound("User", id, UsersTable::mapResult)
        }
    }

    override fun list(
        usernameFilter: String?,
        pagination: Repository.Pagination
    ): Either<DomainError, List<User>> {
        return wrappedSQLStatement {
            with(UsersTable) {
                val query: Query = selectAll()
                usernameFilter?.let {
                    query.andWhere {(displayName ilike "%$it%") or (email ilike "%$it%")}
                }
                query.orderBy(displayName to SortOrder.ASC)
                query.limit(pagination).map(::mapResult)
            }
        }
    }

    override fun loadByExternalId(externalId: String): Either<DomainError, User> {
        return wrappedSQLStatementFlatten {
            UsersTable
                .select {UsersTable.externalId eq externalId}
                .firstOrNotFound("User", "externalId = $externalId", UsersTable::mapResult)
        }
    }


    override fun listById(ids: Set<UserId>): Either<DomainError, Iterable<User>> {
        if (ids.size > 999) {
            return Either.Left(InvalidArgument("can load only 999 user's per one request"))
        }

        return wrappedSQLStatement {
            UsersTable
                .select {UsersTable.id inList ids }
                .map(UsersTable::mapResult)
        }
    }


}