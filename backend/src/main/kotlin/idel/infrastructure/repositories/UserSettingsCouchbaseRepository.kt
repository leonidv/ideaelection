package idel.infrastructure.repositories

import arrow.core.Either
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Collection
import idel.domain.User
import idel.domain.UserSettings
import idel.domain.UserSettingsFactory
import idel.domain.UserSettingsRepository
import mu.KotlinLogging

class UserSettingsCouchbaseRepository(cluster: Cluster, collection: Collection) :
    AbstractTypedCouchbaseRepository<UserSettings>(
        cluster,
        collection,
        typedClass = UserSettings::class.java,
        type = "userSettings"
    ), UserSettingsRepository {
    override val log = KotlinLogging.logger {}

    override fun loadForUser(user: User): Either<Exception, UserSettings> {
        val id = UserSettings.generateId(user)
        return super.load(id)
    }


}