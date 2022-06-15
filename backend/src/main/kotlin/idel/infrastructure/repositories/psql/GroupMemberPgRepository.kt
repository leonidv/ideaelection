package idel.infrastructure.repositories.psql

import arrow.core.Either
import idel.domain.*
import idel.infrastructure.repositories.psql.exposed.firstOrNotFound
import idel.infrastructure.repositories.psql.exposed.ilike
import idel.infrastructure.repositories.psql.exposed.limit
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.datetime

object GroupMembersTable : Table("group_members") {
    val userId = reference("user_id", UsersTable)
    val groupId = reference("group_id", GroupsTable)
    val ctime = datetime("ctime")
    val roleInGroup = enumerationByName("role_in_group", CaseConversion.TO_LOWER, GroupMemberRole::class)

    val userJoin =
        GroupMembersTable
            .join(UsersTable, JoinType.INNER, GroupMembersTable.userId, UsersTable.id)

    fun keyIs(userId: UserId, groupId: GroupId) =
        (GroupMembersTable.groupId eq groupId) and (GroupMembersTable.userId eq userId)

    fun mapResult(resultRow: ResultRow): GroupMember {
        val user = UsersTable.mapResult(resultRow)
        return GroupMember(
            userId = resultRow[userId].value,
            groupId = resultRow[groupId].value,
            ctime = resultRow[ctime],
            roleInGroup = resultRow[roleInGroup],
            externalId = user.externalId,
            avatar = user.avatar,
            displayName = user.displayName,
            email = user.email,
            roles = user.roles,
            subscriptionPlan = user.subscriptionPlan
        )
    }
}

class GroupMemberPgRepository : GroupMemberRepository {
    private fun notImplemented(method: String) = Either.Left(NotImplemented("GroupMemberPgRepository", method))

    private fun formatId(userId: UserId, groupId: GroupId) = "groupId = $groupId && userId = $userId"


    override fun add(groupMember: GroupMember): Either<DomainError, GroupMember> {
        return wrappedSQLStatement {
            GroupMembersTable.insert {
                it[userId] = groupMember.userId
                it[groupId] = groupMember.groupId
                it[ctime] = groupMember.ctime
                it[roleInGroup] = groupMember.roleInGroup
            }
            groupMember
        }
    }

    override fun isMember(groupId: GroupId, userId: UserId): Either<DomainError, Boolean> {
        val gm = load(
            groupId = groupId,
            userId = userId
        )
        return when (gm) {
            is Either.Right<GroupMember> -> Either.Right(true)
            is Either.Left<DomainError> -> if (gm.value is EntityNotFound) {
                Either.Right(false)
            } else {
                gm
            }
        }
    }

    override fun load(groupId: GroupId, userId: UserId): Either<DomainError, GroupMember> {
        return wrappedSQLStatementFlatten {
            GroupMembersTable
                .userJoin
                .select {GroupMembersTable.keyIs(userId = userId, groupId = groupId)}
                .firstOrNotFound(
                    type = "GroupMember",
                    id = formatId(userId = userId, groupId = groupId),
                    mapping = GroupMembersTable::mapResult
                )
        }
    }

    override fun removeFromGroup(groupId: GroupId, userId: UserId): Either<DomainError, Unit> {
        return wrappedSQLStatementFlatten {
            val deleted = GroupMembersTable
                .deleteWhere {GroupMembersTable.keyIs(userId = userId, groupId = groupId)}

            when (deleted) {
                0 -> Either.Left(EntityNotFound("GroupMember", formatId(userId = userId, groupId = groupId)))
                else -> Either.Right(Unit)
            }
        }
    }

    override fun update(groupMember: GroupMember): Either<DomainError, GroupMember> {
        val userId = groupMember.userId
        val groupId = groupMember.groupId
        return wrappedSQLStatementFlatten {
            val updated = GroupMembersTable
                .update(where = {GroupMembersTable.keyIs(userId = userId, groupId = groupId)}) {
                    it[roleInGroup] = groupMember.roleInGroup
                }
            zeroAsNotFound(updated, groupMember, "GroupMember", formatId(userId = userId, groupId = groupId))
        }
    }

    override fun listByGroup(
        groupId: GroupId,
        pagination: Repository.Pagination,
        usernameFilter: String?,
        roleFilter: GroupMemberRole?
    ): Either<DomainError, List<GroupMember>> {
        return wrappedSQLStatement {
            val query =
                GroupMembersTable
                    .userJoin
                    .select {GroupMembersTable.groupId eq groupId}
                    .limit(pagination)
                    .orderBy(GroupMembersTable.ctime, SortOrder.DESC)

            if (usernameFilter != null) {
                query.andWhere {
                    (UsersTable.displayName ilike "%$usernameFilter%") or
                            (UsersTable.email ilike "%$usernameFilter%")
                }
            }

            if (roleFilter != null) {
                query.andWhere {GroupMembersTable.roleInGroup eq roleFilter}
            }

            query.map(GroupMembersTable::mapResult)

        }
    }
}