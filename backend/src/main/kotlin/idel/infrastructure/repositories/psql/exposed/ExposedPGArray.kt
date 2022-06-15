package idel.infrastructure.repositories.psql.exposed

import com.zaxxer.hikari.pool.HikariProxyConnection
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.postgresql.PGConnection

fun <T> Table.array(name: String, columnType: ColumnType): Column<List<T>> {
    return this.registerColumn(name, ExposedPGArray(columnType))
}

class AnyArrayOp(val op: ComparisonOp) : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder): Unit = queryBuilder {
        append('(', op.expr1, ')')
        append(" ${op.opSign} ANY (")
        append(op.expr2)
        append(")")
    }

}

fun <T>cardinality(column: Column<List<T>>) = CardinalityArrayFunction(column)

class CardinalityArrayFunction<T>(column: Column<List<T>> ) : CustomFunction<Int>(
    functionName = "cardinality",
    _columnType = ExposedPGArray(AnyColumnType),
    expr = arrayOf(column)
)



infix fun <T> T.eqAny(arrayColumn: ExpressionWithColumnType<List<T>>): AnyArrayOp {
    require(arrayColumn.columnType is ExposedPGArray) {"eqAny used with ExposedPGArray columns"}
    val param = QueryParameter(this, (arrayColumn.columnType as ExposedPGArray).type)
    return AnyArrayOp(EqOp(param, arrayColumn))
}

object AnyColumnType : ColumnType() {
    override fun sqlType() = "AnyColumnType can't be used as real type"
}

class ExposedPGArray(val type: ColumnType) : ColumnType() {
    override fun sqlType() = "${type.sqlType()} ARRAY"

    private val nativeSqlType = type.sqlType().split("(")[0]

    private fun pgArray(columnType: String, value: Array<*>): Any {
        val manager = TransactionManager.current();
        val jdbcConnection = (manager.connection.connection as java.sql.Connection)
        return jdbcConnection.createArrayOf(columnType, value) ?: error("Can't create non null array for $value")
    }

    override fun valueFromDB(value: Any): Any {
        return when (value) {
            is java.sql.Array -> {
                val v = value.array as Array<*>
                v.toList()
            }
            else -> error("Unsupported type ${value.javaClass}")
        }
    }

    override fun notNullValueToDB(value: Any): Any {
        return when (value) {
            is List<*> -> pgArray(nativeSqlType, value.toTypedArray())
            else -> super.notNullValueToDB(value)
        }
    }


    override fun valueToDB(value: Any?): Any? {
        return when (value) {
            is List<*> -> pgArray(nativeSqlType, value.toTypedArray())
            else -> super.valueToDB(value)
        }
    }
}

