package idel.infrastructure.repositories.psql

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.VarCharColumnType
import kotlin.reflect.KClass

enum class CaseConversion {
    NONE, TO_LOWER, TO_UPPER
}

fun <T : Enum<T>> Table.enumerationByName(
    name: String,
    conversion: CaseConversion,
    klass: KClass<T>,
    length : Int = 25
): Column<T> =
    this.registerColumn(name, CaseInsetivityEnumerationNameColumnType(klass, conversion, length))

class CaseInsetivityEnumerationNameColumnType<T : Enum<T>>(
    private val klass: KClass<T>,
    private val conversion: CaseConversion,
    colLength: Int = 25
) : VarCharColumnType(colLength) {

    private fun convert(enum: T, conversion: CaseConversion): String = when (conversion) {
        CaseConversion.NONE -> enum.name
        CaseConversion.TO_LOWER -> enum.name.lowercase()
        CaseConversion.TO_UPPER -> enum.name.uppercase()
    }

    @Suppress("UNCHECKED_CAST")
    override fun valueFromDB(value: Any): T = when (value) {
        is String ->
            klass.java.enumConstants!!.firstOrNull {it.name.equals(value, ignoreCase = true)}
                ?: error("$value can't be associated with any from enum ${klass.qualifiedName}")

        is Enum<*> -> value as T
        else -> error("$value of ${value::class.qualifiedName} is not valid for enum ${klass.qualifiedName}")
    }


    override fun notNullValueToDB(value: Any) = when (value) {
        is String -> super.notNullValueToDB(value)
        is Enum<*> -> super.notNullValueToDB(convert(value as T, conversion))
        else -> error("$value of ${value::class.qualifiedName} is not valid for enum ${klass.qualifiedName}")
    }

}