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
    val user_id: UserId,
    override val notificationsFrequency: NotificationsFrequency,
    override val subscribedToNews: Boolean
) :  IUserSettingsEditableProperties {
    companion object {
        fun generateId(user: User) = compositeId("us", user.id)
    }
}

class UserSettingsFactory {
    fun createDefault(user: User): UserSettings = UserSettings(
        user_id = user.id,
        notificationsFrequency = NotificationsFrequency.INSTANTLY,
        subscribedToNews = false
    )

    fun fromProperties(user: User, editableProperties: IUserSettingsEditableProperties) =
        UserSettings(
            user_id = user.id,
            notificationsFrequency = editableProperties.notificationsFrequency,
            subscribedToNews = editableProperties.subscribedToNews
        )
}

interface UserSettingsRepository  {
    /**
     * Load settings for user.
     */
    fun loadForUser(user: User): Either<DomainError, UserSettings>

    /**
     * Replace settings by new.
     */
    fun update(userId: UserId, userSettings: IUserSettingsEditableProperties): Either<DomainError, IUserSettingsEditableProperties>

    /**
     * Add new user's settings
     */
    fun add(userSettings: UserSettings) : Either<DomainError, UserSettings>
}