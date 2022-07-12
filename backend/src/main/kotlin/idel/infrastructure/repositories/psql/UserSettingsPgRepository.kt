package idel.infrastructure.repositories.psql

import arrow.core.Either
import idel.domain.*
import idel.domain.UserSettings
import idel.infrastructure.repositories.psql.exposed.firstOrNotFound
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update

object UserSettingsTable : UUIDTable(name = "user_settings", columnName = "user_id") {
    val notificationsFrequency = this.enumerationByName("notification_frequency", CaseConversion.TO_LOWER, NotificationsFrequency::class)
    val subscribedToNews = bool("subscribed_to_news")

    fun mapToResult(resultRow: ResultRow) : UserSettings {
        return UserSettings(
            user_id = resultRow[id].value,
            notificationsFrequency = resultRow[notificationsFrequency],
            subscribedToNews = resultRow[subscribedToNews]
        )
    }
}

class UserSettingsPgRepository : UserSettingsRepository {
    override fun loadForUser(user: User): Either<DomainError, UserSettings> {
        return wrappedSQLStatementFlatten {
                UserSettingsTable
                    .select {UserSettingsTable.id eq user.id}
                    .firstOrNotFound("UserSettings","user.id = ${user.id}", UserSettingsTable::mapToResult)
        }
    }

    override fun update(
        userId: UserId,
        userSettings: IUserSettingsEditableProperties
    ): Either<DomainError, IUserSettingsEditableProperties> {
        return wrappedSQLStatementFlatten {
            val updated : Int = UserSettingsTable.update({UserSettingsTable.id eq userId}) {
                it[notificationsFrequency] = userSettings.notificationsFrequency
                it[subscribedToNews] = userSettings.subscribedToNews
            }

            zeroAsNotFound(updated, userSettings, "userSettings",userId)
        }
    }

    override fun add(userSettings: UserSettings): Either<DomainError, UserSettings> {
        return wrappedSQLStatement {
            UserSettingsTable.insert {
                it[id] = userSettings.user_id
                it[notificationsFrequency] = userSettings.notificationsFrequency
                it[subscribedToNews] = userSettings.subscribedToNews
            }
            userSettings
        }
    }
}