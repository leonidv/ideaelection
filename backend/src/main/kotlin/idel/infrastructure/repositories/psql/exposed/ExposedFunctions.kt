package idel.infrastructure.repositories.psql.exposed

import arrow.core.Either
import idel.domain.DomainError
import idel.domain.EntityNotFound
import idel.domain.Repository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager

fun Query.limit(pagination: Repository.Pagination) = this.limit(
    n = pagination.count, offset = pagination.skip.toLong()
)

fun <T>Query.firstOrNotFound(type: String, id : Any, mapping: (resultRow: ResultRow) -> T) : Either<DomainError,T> {
     val entity = this.firstNotNullOfOrNull(mapping)
     return if (entity == null) {
         Either.Left(EntityNotFound(type,id))
     } else {
         Either.Right(entity)
     }
}

class ILikeOp(expr1 : Expression<*>, expr2 : Expression<*>) : ComparisonOp(expr1, expr2, "ILIKE")

infix fun <T: String?> Expression<T>.ilike(pattern: String): ILikeOp = ILikeOp(this, stringParam(pattern))

class SubQueryExpression<T>(private val aliasQuery : QueryAlias) : Expression<T>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        aliasQuery.describe(TransactionManager.current(), queryBuilder)
    }
}

fun <T> QueryAlias.asExpression() = SubQueryExpression<T>(this)
