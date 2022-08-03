package idel.infrastructure.repositories.psql

import arrow.core.Either
import idel.domain.*
import idel.infrastructure.repositories.psql.exposed.firstOrNotFound
import idel.infrastructure.repositories.psql.exposed.limit
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import java.util.*


object CommentsTable : UUIDTable(name = "comments") {
    val ctime = datetime("ctime")
    val ideaId = reference("idea_id", IdeasTable)
    val author = reference("author", UsersTable)
    val content = text("content")
    val lastEditTime = datetime("last_edit_time").nullable()
    val lastEditBy = reference("last_edit_by", UsersTable).nullable()
    val replyTo = uuid("reply_to").nullable()

    @Suppress("DuplicatedCode")
    fun addFields(comment: Comment, includeId: Boolean, updateBuilder: UpdateBuilder<Number>) {
        if (includeId) updateBuilder[id] = comment.id
        updateBuilder[ideaId] = comment.ideaId
        updateBuilder[ctime] = comment.ctime
        updateBuilder[author] = comment.author
        updateBuilder[content] = comment.content
        updateBuilder[lastEditTime] = comment.lastEditedTime
        updateBuilder[lastEditBy] = comment.lastEditedBy
        updateBuilder[replyTo] = comment.replyTo
    }

    fun mapResult(resultRow: ResultRow): Comment {
        return Comment(
            id = resultRow[id].value,
            ideaId = resultRow[ideaId].value,
            ctime = resultRow[ctime],
            author = resultRow[author].value,
            content = resultRow[content],
            lastEditedTime = resultRow[lastEditTime],
            lastEditedBy = resultRow[lastEditBy]?.value,
            replyTo = resultRow[replyTo]
        )
    }
}

class CommentPgRepository : CommentRepository {

    private fun Query.orderBy(orderBy: CommentsOrder): Query {
        return when (orderBy) {
            CommentsOrder.CTIME_ASC -> this.orderBy(CommentsTable.ctime, SortOrder.ASC)
            CommentsOrder.CTIME_DESC -> this.orderBy(CommentsTable.ctime, SortOrder.DESC)
        }
    }

    override fun add(comment: Comment): Either<DomainError, Comment> {
        return wrappedSQLStatement {

            CommentsTable.insert {insSt ->
                addFields(comment, includeId = true, insSt)
            }
            comment
        }
    }

    override fun load(commentId: UUID): Either<DomainError, Comment> {
        return wrappedSQLStatementFlatten {
             CommentsTable
                .select {CommentsTable.id eq commentId}
                .firstOrNotFound("Comment", commentId, CommentsTable::mapResult)
        }
    }

    override fun countForIdeas(ideaIds: List<IdeaId>): Either<DomainError, Map<IdeaId, Int>> {
        return wrappedSQLStatement {
            val commentsCount = CommentsTable.id.count()
            IdeasTable.join(
                otherTable = CommentsTable,
                onColumn = IdeasTable.id,
                otherColumn = CommentsTable.ideaId,
                joinType = JoinType.LEFT
            )
                .slice(IdeasTable.id, commentsCount)
                .select {CommentsTable.ideaId inList ideaIds}
                .groupBy(IdeasTable.id)
                .associate {resultRow ->
                    Pair(resultRow[IdeasTable.id].value, resultRow[commentsCount].toInt())
                }
        }
    }

    override fun list(
        ideaId: IdeaId,
        order: CommentsOrder,
        pagination: Repository.Pagination
    ): Either<DomainError, List<Comment>> {
        return wrappedSQLStatement {
            val q = CommentsTable
                .select {CommentsTable.ideaId eq ideaId}
                .orderBy(order)
                .limit(pagination)

            q.map(CommentsTable::mapResult)
        }
    }

    override fun delete(id: UUID): Either<DomainError, Unit> {
        return wrappedSQLStatementFlatten {
            val deletedCount = CommentsTable.deleteWhere {CommentsTable.id eq id}
            zeroAsNotFound(deletedCount, Unit, "comment", id)
        }
    }

    override fun update(comment: Comment): Either<DomainError, Comment> {
        return wrappedSQLStatementFlatten {
            val updatedCount =
                CommentsTable.update(
                    where = {CommentsTable.id eq comment.id},
                    body = {updSt -> addFields(comment, includeId = false, updSt)}
                )
            zeroAsNotFound(updatedCount, comment, "Comment", comment.id)
        }
    }
}