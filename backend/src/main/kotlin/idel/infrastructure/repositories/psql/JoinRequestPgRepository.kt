@file:Suppress("DuplicatedCode")

package idel.infrastructure.repositories.psql

import arrow.core.Either
import arrow.core.continuations.either
import idel.domain.*
import idel.infrastructure.repositories.psql.exposed.firstOrNotFound
import idel.infrastructure.repositories.psql.exposed.limit
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import java.time.LocalDateTime
import java.util.*

object JoinRequestsTable : UUIDTable(name = "joinrequests", columnName = "id") {
    val ctime = datetime("ctime")
    val mtime = datetime("mtime")
    val userId = uuid("user_id").references(UsersTable.id)
    val groupId = uuid("group_id").references(GroupsTable.id)
    val status = enumerationByName("status", CaseConversion.TO_LOWER, AcceptStatus::class)
    val message = varchar("message", 200)


    fun mapResult(resultRow: ResultRow): JoinRequest {
        return JoinRequest(
            id = resultRow[id].value,
            ctime = resultRow[ctime],
            mtime = resultRow[mtime],
            userId = resultRow[userId],
            groupId = resultRow[groupId],
            status = resultRow[status],
            message = resultRow[message]
        )
    }

    fun <T> mapEntity(builder: UpdateBuilder<T>, entity: JoinRequest) {
        builder[id] = entity.id
        builder[ctime] = entity.ctime
        builder[mtime] = LocalDateTime.now()
        builder[userId] = entity.userId
        builder[groupId] = entity.groupId
        builder[status] = entity.status
        builder[message] = entity.message
    }
}

class JoinRequestPgRepository : JoinRequestRepository, HasUnimplemented {
    private fun Query.orderBy(ordering: GroupMembershipRequestOrdering): Query = when (ordering) {
        GroupMembershipRequestOrdering.CTIME_ASC -> this.orderBy(JoinRequestsTable.ctime, SortOrder.ASC)
        GroupMembershipRequestOrdering.CTIME_DESC -> this.orderBy(JoinRequestsTable.ctime, SortOrder.DESC)
        GroupMembershipRequestOrdering.MTIME_ASC -> this.orderBy(JoinRequestsTable.mtime, SortOrder.ASC)
        GroupMembershipRequestOrdering.MTIME_DESC -> this.orderBy(JoinRequestsTable.mtime, SortOrder.DESC)
    }

    private fun Query.withStatus(status: AcceptStatus?) = if (status != null) {
        this.andWhere {JoinRequestsTable.status eq status}
    } else {
        this
    }

    private fun Query.withStatuses(statuses: Set<AcceptStatus>) = if (statuses.isNotEmpty()) {
        this.andWhere {JoinRequestsTable.status inList statuses}
    } else {
        this
    }

    override fun loadUnresolved(user: User, group: Group): Either<DomainError, JoinRequest> {
        return wrappedSQLStatementFlatten {
            JoinRequestsTable
                .select {(JoinRequestsTable.userId eq user.id) and (JoinRequestsTable.groupId eq group.id)}
                .withStatus(AcceptStatus.UNRESOLVED)
                .firstOrNotFound(
                    "JoinRequest (unresolved)",
                    "user.id=${user.id}, group.id=${group.id}",
                    JoinRequestsTable::mapResult
                )
        }
    }

    override fun load(id: UUID): Either<DomainError, JoinRequest> {
        return wrappedSQLStatementFlatten {
            JoinRequestsTable
                .select {JoinRequestsTable.id eq id}
                .firstOrNotFound("JoinRequest", id, JoinRequestsTable::mapResult)
        }
    }

    override fun loadByUser(
        userId: UserId,
        statuses: Set<AcceptStatus>,
        ordering: GroupMembershipRequestOrdering,
        pagination: Repository.Pagination
    ): Either<DomainError, List<JoinRequest>> {
        return wrappedSQLStatement {
            JoinRequestsTable
                .select {JoinRequestsTable.userId eq userId}
                .withStatuses(statuses)
                .orderBy(ordering)
                .limit(pagination)
                .map(JoinRequestsTable::mapResult)
        }
    }

    override fun loadByGroup(
        groupId: GroupId,
        statuses: Set<AcceptStatus>,
        ordering: GroupMembershipRequestOrdering,
        pagination: Repository.Pagination
    ): Either<DomainError, List<JoinRequest>> {
        return wrappedSQLStatement {
            JoinRequestsTable
                .select {JoinRequestsTable.groupId eq groupId}
                .orderBy(ordering)
                .limit(pagination)
                .map(JoinRequestsTable::mapResult)
        }
    }


    override fun add(entity: JoinRequest): Either<DomainError, JoinRequest> {
        return wrappedSQLStatement {
            JoinRequestsTable.insert {
                mapEntity(it, entity)
            }
            entity
        }
    }

    override fun possibleMutate(
        id: UUID,
        mutation: (entity: JoinRequest) -> Either<DomainError, JoinRequest>
    ): Either<DomainError, JoinRequest> {
        return wrappedSQLStatementFlatten {
            either.eager {
                val joinRequest = load(id).bind()
                val nextJoinRequest = mutation(joinRequest).bind()
                JoinRequestsTable.update(
                    where = {JoinRequestsTable.id eq id},
                    limit = null,
                    body = {mapEntity(it, nextJoinRequest)}
                )
                nextJoinRequest
            }
        }
    }
}