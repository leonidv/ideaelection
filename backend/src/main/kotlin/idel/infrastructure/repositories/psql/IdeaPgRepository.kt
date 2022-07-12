@file:Suppress("DuplicatedCode") // detect duplicate in GroupsPgRepository

package idel.infrastructure.repositories.psql

import arrow.core.Either
import arrow.core.flatMap
import idel.domain.*
import idel.infrastructure.repositories.psql.exposed.*
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import java.util.*

object IdeasTable : UUIDTable(name = "ideas") {
    val ctime = datetime("ctime")
    val groupId = reference("group_id", GroupsTable)
    val summary = varchar("summary", 255)
    val description = varchar("description",10_000)
    val descriptionPlainText = varchar("description_plain_text",2_000)
    val link = varchar("link",1000)
    val assignee = uuid("assignee").nullable()
    val implemented = bool("implemented")
    val author = reference("author",UsersTable)
    val voters = array<UUID>("voters", UUIDColumnType())
    val archived = bool("archived")
    val deleted = bool("deleted")

    fun mapResult(resultRow: ResultRow) : Idea {
        return Idea(
            id = resultRow[id].value,
            groupId = resultRow[groupId].value,
            ctime = resultRow[ctime],
            summary = resultRow[summary],
            description = resultRow[description],
            descriptionPlainText = resultRow[descriptionPlainText],
            link = resultRow[link],
            assignee = resultRow[assignee],
            implemented = resultRow[implemented],
            author = resultRow[author].value,
            voters = resultRow[voters],
            archived = resultRow[archived],
            deleted = resultRow[deleted]
        )
    }
}

class IdeaPgRepository : IdeaRepository {
    private fun Query.orderBy(ideaOrdering: IdeaOrdering) : Query {
       return when(ideaOrdering) {
            IdeaOrdering.CTIME_ASC -> this.orderBy(IdeasTable.ctime, SortOrder.ASC)
            IdeaOrdering.CTIME_DESC -> this.orderBy(IdeasTable.ctime, SortOrder.DESC)
            IdeaOrdering.VOTES_DESC -> this.orderBy(cardinality(IdeasTable.voters), SortOrder.DESC)
        }
    }

    private fun Query.notDeleted() : Query = this.andWhere {IdeasTable.deleted eq false}


    override fun add(entity: Idea): Either<DomainError, Idea> {
        return wrappedSQLStatement {
            IdeasTable.insert {
                it[id] = entity.id
                it[ctime] = entity.ctime
                it[groupId] = entity.groupId
                it[summary] = entity.summary
                it[description] = entity.description
                it[descriptionPlainText] = entity.descriptionPlainText
                it[link] = entity.link
                it[assignee] = entity.assignee
                it[implemented] = entity.implemented
                it[author] = entity.author
                it[voters] = entity.voters
                it[archived] = entity.archived
                it[deleted] = entity.deleted
            }
            entity
        }
    }

    override fun update(idea: Idea): Either<DomainError, Idea> {
       return wrappedSQLStatementFlatten {
            val updated = IdeasTable
                .update (
                    where = { IdeasTable.id eq idea.id },
                    body = {
                        it[groupId] = idea.groupId
                        it[summary] = idea.summary
                        it[description] = idea.description
                        it[descriptionPlainText] = idea.descriptionPlainText
                        it[link] = idea.link
                        it[assignee] = idea.assignee
                        it[implemented] = idea.implemented
                        it[author] = idea.author
                        it[voters] = idea.voters
                        it[archived] = idea.archived
                        it[deleted] = idea.deleted
                    }
                )

            zeroAsNotFound(updated, idea, "idea", idea.id)
        }
    }

    override fun update(idea: Either<DomainError, Idea>) =  idea.flatMap {update(it)}


    override fun list(
        groupId: GroupId,
        ordering: IdeaOrdering,
        filtering: IdeaFiltering,
        pagination: Repository.Pagination
    ): Either<DomainError, List<Idea>> {
        return wrappedSQLStatement {
            val q = IdeasTable
                .select {IdeasTable.groupId eq groupId}
                .orderBy(ordering)
                .limit(pagination)

            with(filtering) {
                author?.let { q.andWhere {IdeasTable.author eq author}}
                implemented?.let {q.andWhere {IdeasTable.implemented eq implemented}}
                assignee?.let { q.andWhere {IdeasTable.assignee eq assignee} }
                text?.let { q.andWhere {
                    (IdeasTable.summary ilike "%$text%") or (IdeasTable.descriptionPlainText ilike "%$text%")
                }}
                votedBy?.let { q.andWhere {votedBy eqAny IdeasTable.voters  } }

                q.andWhere {IdeasTable.deleted eq listDeleted}
                q.andWhere {IdeasTable.archived eq listArchived}
            }

            q.map(IdeasTable::mapResult)
        }
    }

    override fun load(id: IdeaId): Either<DomainError, Idea> {
        return wrappedSQLStatementFlatten {
            IdeasTable
                .select {IdeasTable.id eq id}
                .andWhere {IdeasTable.deleted eq false}
                .firstOrNotFound("idea",id, IdeasTable::mapResult)
        }
    }

}