@file:Suppress("DuplicatedCode")

package idel.infrastructure.repositories.psql

import arrow.core.Either
import idel.domain.*
import idel.infrastructure.repositories.psql.exposed.*
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.statements.UpdateStatement


object GroupsTable : UUIDTable(name = "groups", columnName = "id") {
    val ctime = datetime("ctime")
    val joiningKey = varchar("joining_key", 64)
    val creator = uuid("creator").references(UsersTable.id)
    val state = enumerationByName("state", CaseConversion.TO_LOWER, GroupState::class)
    val name = varchar("name", 100)
    val description = varchar("description", 300)
    val logo = text("logo")
    val entryMode = enumerationByName("entry_mode", CaseConversion.TO_LOWER, GroupEntryMode::class)
    val entryQuestion = varchar("entry_question", 200)
    val domainRestrictions: Column<List<String>> = array<String>("domain_restrictions", TextColumnType())

    val membersCount = GroupMembersTable
        .slice(GroupMembersTable.userId.count())
        .select {GroupsTable.id eq GroupMembersTable.groupId}
        .alias("members_count")
        .asExpression<Int>()

    val ideasCount = IdeasTable
        .slice(IdeasTable.id.count())
        .select(GroupsTable.id eq IdeasTable.groupId)
        .alias("ideas_count")
        .asExpression<Int>()

    val usersJoin =
        GroupsTable
            .join(UsersTable, JoinType.INNER, additionalConstraint = {creator eq UsersTable.id})

    val allFields =
        usersJoin
            .slice(usersJoin.fields + listOf(membersCount, ideasCount))


    fun mapResult(resultRow: ResultRow): Group {
        val creator = UsersTable.mapResult(resultRow)
        val group = with(GroupsTable) {
            Group(
                id = resultRow[id].value,
                ctime = resultRow[ctime],
                creator = UserInfo.ofUser(creator),
                state = resultRow[state],
                name = resultRow[name],
                description = resultRow[description],
                logo = resultRow[logo],
                entryMode = resultRow[entryMode],
                entryQuestion = resultRow[entryQuestion],
                domainRestrictions = resultRow[domainRestrictions],
                membersCount = resultRow[membersCount],
                ideasCount = resultRow[ideasCount],
                joiningKey = resultRow[joiningKey]
            )
        }
        return group
    }
}


class GroupPgRepository : GroupRepository {
    private fun Query.notDeleted(): Query = this.andWhere {GroupsTable.state neq GroupState.DELETED}

    private fun Query.withPartOfName(partOfName: String?): Query =
        if (partOfName != null) {
            this.andWhere {GroupsTable.name ilike "%$partOfName%"}
        } else {
            this
        }

    private fun Query.orderBy(ordering: GroupOrdering): Query {
        return when (ordering) {
            GroupOrdering.CTIME_ASC -> this.orderBy(GroupsTable.ctime, SortOrder.ASC)
            GroupOrdering.CTIME_DESC -> this.orderBy(GroupsTable.ctime, SortOrder.DESC)
            GroupOrdering.NAME_ASC -> this.orderBy(GroupsTable.name, SortOrder.ASC)
            GroupOrdering.NAME_DESC -> this.orderBy(GroupsTable.name, SortOrder.DESC)
        }
    }

    override fun add(entity: Group): Either<DomainError, Group> {
        return wrappedSQLStatement {
            GroupsTable.insert {
                it[id] = entity.id
                it[ctime] = entity.ctime
                it[creator] = entity.creator.id
                it[joiningKey] = entity.joiningKey
                it[state] = entity.state
                it[name] = entity.name
                it[description] = entity.description
                it[logo] = entity.logo
                it[entryMode] = entity.entryMode
                it[entryQuestion] = entity.entryQuestion
                it[domainRestrictions] = entity.domainRestrictions
            }

            entity
        }
    }

    override fun listByUser(
        userId: UserId,
        partOfName: String?,
        pagination: Repository.Pagination,
        ordering: GroupOrdering
    ): Either<DomainError, List<Group>> {
        return wrappedSQLStatement {
            with(GroupsTable) {
                val memberJoin = usersJoin.join(otherTable = GroupMembersTable,
                    joinType = JoinType.INNER,
                    onColumn = id,
                    otherColumn = GroupMembersTable.groupId,
                    additionalConstraint = {GroupMembersTable.userId eq userId})

                val fields = memberJoin.slice(memberJoin.fields + listOf(membersCount, ideasCount))

                val q = fields
                    .selectAll()
                    .notDeleted()
                    .withPartOfName(partOfName)
                    .limit(pagination)
                    .orderBy(ordering)

                q.map(this::mapResult)
            }
        }
    }

    override fun listOnlyAvailable(
        userId: UserId,
        userDomain: String,
        partOfName: String?,
        pagination: Repository.Pagination,
        ordering: GroupOrdering
    ): Either<DomainError, List<Group>> {
        return wrappedSQLStatement {
            with(GroupsTable) {
                val userGroups = GroupMembersTable
                    .slice(GroupMembersTable.groupId)
                    .select {GroupMembersTable.userId eq userId}

                val q =
                    allFields
                        .select {id notInSubQuery userGroups}
                        .andWhere {(userDomain eqAny domainRestrictions) or (cardinality(domainRestrictions) eq 0)}
                        .andWhere {entryMode neq GroupEntryMode.PRIVATE}
                        .withPartOfName(partOfName)
                        .notDeleted()
                        .limit(pagination)
                        .orderBy(ordering)


                q.map(GroupsTable::mapResult)
            }
        }
    }

    override fun loadByJoiningKey(key: String): Either<DomainError, Group> {
        return wrappedSQLStatementFlatten {
            GroupsTable
                .allFields
                .select {GroupsTable.joiningKey eq key}
                .notDeleted()
                .firstOrNotFound("Group", "joiningKey = $key", GroupsTable::mapResult)
        }
    }

    override fun update(group: Group): Either<DomainError, Group> {
        return wrappedSQLStatementFlatten {
            val updated = GroupsTable.update(
                where = {GroupsTable.id eq group.id},
                limit = null,
                body = {it: UpdateStatement ->
                    it[joiningKey] = group.joiningKey
                    it[state] = group.state
                    it[name] = group.name
                    it[description] = group.description
                    it[logo] = group.logo
                    it[entryMode] = group.entryMode
                    it[entryQuestion] = group.entryQuestion
                    it[domainRestrictions] = group.domainRestrictions
                }
            )
            zeroAsNotFound(updated, group, "Group", group.id)
        }
    }

    override fun load(id: GroupId): Either<DomainError, Group> {
        return wrappedSQLStatementFlatten {
            GroupsTable
                .allFields
                .select {GroupsTable.id eq id}
                .notDeleted()
                .firstOrNotFound("Group", id, GroupsTable::mapResult)
        }
    }
}