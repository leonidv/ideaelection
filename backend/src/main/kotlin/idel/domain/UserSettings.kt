package idel.domain

import arrow.core.Either


enum class NotificationsFrequency {
    INSTANTLY,
    DAILY,
    WEEKLY,
    DISABLED
}

interface IUserSettingsEditableProperties {
    val notificationsFrequency: NotificationsFrequency
    val subscribedToNews: Boolean
}

class UserSettingsEditableProperties(
    override val notificationsFrequency: NotificationsFrequency,
    override val subscribedToNews: Boolean
) : IUserSettingsEditableProperties {
    companion object {
        fun of(settings : IUserSettingsEditableProperties) =
            UserSettingsEditableProperties(
                settings.notificationsFrequency,
                settings.subscribedToNews
            )
    }
}

data class UserSettings(
    override val id: String,
    val userId: UserId,
    override val notificationsFrequency: NotificationsFrequency,
    override val subscribedToNews: Boolean
) : Identifiable, IUserSettingsEditableProperties {
    companion object {
        fun generateId(user: User) = compositeId("us", user.id)
    }
}

class UserSettingsFactory {
    fun createDefault(user: User): UserSettings = UserSettings(
        id = UserSettings.generateId(user),
        userId = user.id,
        notificationsFrequency = NotificationsFrequency.INSTANTLY,
        subscribedToNews = false
    )

    fun fromProperties(user: User, editableProperties: IUserSettingsEditableProperties) =
        UserSettings(
            id = UserSettings.generateId(user),
            userId = user.id,
            notificationsFrequency = editableProperties.notificationsFrequency,
            subscribedToNews = editableProperties.subscribedToNews
        )
}

interface UserSettingsRepository : BaseRepository<UserSettings> {
    /**
     * Load settings for user.
     */
    fun loadForUser(user: User): Either<Exception, UserSettings>

    /**
     * Replace settings by new.
     */
    fun replace(userSettings: UserSettings) : Either<Exception,UserSettings>
}