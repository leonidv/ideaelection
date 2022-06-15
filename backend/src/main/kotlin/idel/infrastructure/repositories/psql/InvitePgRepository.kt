package idel.infrastructure.repositories.psql

import arrow.core.Either
import idel.domain.*
import idel.infrastructure.repositories.psql.exposed.firstOrNotFound
import idel.infrastructure.repositories.psql.exposed.limit
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.*

object InvitesTable : UUIDTable(name = "invites", columnName = "id") {
    val ctime = datetime("ctime")
    val mtime = datetime("mtime")
    val userId = uuid("user_id").references(UsersTable.id).nullable()
    val groupId = uuid("group_id").references(GroupsTable.id)
    val status = enumerationByName("status", CaseConversion.TO_LOWER, AcceptStatus::class)
    val author = uuid("author").references(UsersTable.id)
    val userEmail = varchar("user_email", 200)
    val emailStatus = enumerationByName("email_status", CaseConversion.TO_LOWER, InviteEmailStatus::class)
    val message = varchar("message", 200)

    fun mapResult(resultRow: ResultRow): Invite {
        return Invite(
            id = resultRow[id].value,
            ctime = resultRow[ctime],
            mtime = resultRow[mtime],
            userId = resultRow[userId],
            groupId = resultRow[groupId],
            status = resultRow[status],
            author = resultRow[author],
            userEmail = resultRow[userEmail],
            emailStatus = resultRow[emailStatus],
            message = resultRow[message]
        )
    }
}


class InvitePgRepository : InviteRepository, HasUnimplemented {
    private fun Query.withStatus(status: AcceptStatus?) = if (status != null) {
        this.andWhere {InvitesTable.status eq status}
    } else {
        this
    }

    private fun Query.orderBy(ordering: GroupMembershipRequestOrdering): Query {
        return when (ordering) {
            GroupMembershipRequestOrdering.CTIME_ASC -> this.orderBy(InvitesTable.ctime, SortOrder.ASC)
            GroupMembershipRequestOrdering.CTIME_DESC -> this.orderBy(InvitesTable.ctime, SortOrder.DESC)
            GroupMembershipRequestOrdering.MTIME_ASC -> this.orderBy(InvitesTable.mtime, SortOrder.ASC)
            GroupMembershipRequestOrdering.MTIME_DESC -> this.orderBy(InvitesTable.mtime, SortOrder.DESC)
        }
    }

    override fun loadUnresolved(user: User, group: Group): Either<DomainError, Invite> {
        return wrappedSQLStatementFlatten {
            InvitesTable
                .select {(InvitesTable.userId eq user.id) and (InvitesTable.groupId eq group.id)}
                .withStatus(AcceptStatus.UNRESOLVED)
                .firstOrNotFound(
                    "Invite (unresolved)",
                    "user.id = ${user.id}, group.id = ${group.id}",
                    InvitesTable::mapResult
                )
        }
    }

    override fun load(id: UUID): Either<DomainError, Invite> {
        return wrappedSQLStatementFlatten {
            InvitesTable
                .select {InvitesTable.id eq id}
                .firstOrNotFound("Invite", id, InvitesTable::mapResult)
        }
    }

    override fun loadByUser(
        userId: UserId,
        statuses: Set<AcceptStatus>,
        order: GroupMembershipRequestOrdering,
        pagination: Repository.Pagination
    ): Either<DomainError, List<Invite>> {
        return wrappedSQLStatement {
            InvitesTable
                .select {(InvitesTable.userId eq userId) and (InvitesTable.status inList (statuses))}
                .orderBy(order)
                .limit(pagination)
                .map(InvitesTable::mapResult)
        }
    }

    override fun loadByEmail(email: String, pagination: Repository.Pagination): Either<DomainError, List<Invite>> {
        return wrappedSQLStatement {
            InvitesTable
                .select {InvitesTable.userEmail eq UserInfo.normalizeEmail(email)}
                .limit(pagination)
                .orderBy(GroupMembershipRequestOrdering.CTIME_ASC)
                .map(InvitesTable::mapResult)
        }
    }

    override fun loadByGroup(
        groupId: GroupId,
        statuses: Set<AcceptStatus>,
        order: GroupMembershipRequestOrdering,
        pagination: Repository.Pagination
    ): Either<DomainError, List<Invite>> {
        return wrappedSQLStatement {
            InvitesTable
                .select{(InvitesTable.groupId eq groupId) and (InvitesTable.status inList statuses) }
                .orderBy(order)
                .limit(pagination)
                .map(InvitesTable::mapResult)
        }

    }

    override fun add(invite: Invite): Either<DomainError, Invite> {
        return wrappedSQLStatement {
            InvitesTable.insert {
                it[id] = invite.id
                it[ctime] = invite.ctime
                it[mtime] = invite.mtime
                it[userId] = invite.userId
                it[groupId] = invite.groupId
                it[status] = invite.status
                it[author] = invite.author
                it[userEmail] = UserInfo.normalizeEmail(invite.userEmail)
                it[emailStatus] = invite.emailStatus
            }
            invite
        }
    }

    override fun update(invite: Invite): Either<DomainError, Invite> {
        return wrappedSQLStatementFlatten {
            val updated = InvitesTable.update(
                where = {InvitesTable.id eq invite.id},
                body = {
                    it[mtime] = LocalDateTime.now()
                    it[status] = invite.status
                    it[userId] = invite.userId
                    it[emailStatus] = invite.emailStatus
                }
            )

            zeroAsNotFound(updated, invite, "Invite", invite.id)
        }
    }

}