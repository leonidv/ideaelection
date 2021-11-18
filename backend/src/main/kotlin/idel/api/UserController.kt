package idel.api

import arrow.core.Either
import arrow.core.Option
import arrow.core.computations.either
import arrow.core.flatMap
import idel.domain.*
import idel.infrastructure.repositories.PersistsUser
import mu.KLogger
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/users")
class UserController(
    private val userRepository: UserRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val userService: UserService
) {
    data class MeResult(
        val id: String,
        val domain: String,
        val displayName: String,
        val avatar: String,
        val email: String,
        val authorities: List<String>
    )


    private val log: KLogger = KotlinLogging.logger {}

    private val userSettingsFactory = UserSettingsFactory()

    @GetMapping("/me")
    @ResponseBody
    fun me(authentication: Authentication): ResponseEntity<DataOrError<MeResult>> {
        val principal = authentication.principal

        return if (principal is User) {
            val result = MeResult(
                id = principal.id,
                domain = principal.domain,
                displayName = principal.displayName,
                avatar = principal.avatar,
                email = principal.email,
                authorities = authentication.authorities.map {it.authority}
            )
            DataOrError.ok(result)
        } else {
            return DataOrError.internal("principal is not User");
        }
    }

    @GetMapping("/me2")
    fun me2(@AuthenticationPrincipal user: User): User {
        return user
    }

    /**
     * Check [roles] and if it correct call [actionIfCorrect]
     *
     * @param roles - roles which should be checked
     * @param actionIfCorrect - function which will be called if roles are correct
     */
    private fun <T> rolesAreNotMisspelled(
        roles: Set<String>,
        actionIfCorrect: () -> ResponseEntity<DataOrError<T>>
    ): ResponseEntity<DataOrError<T>> {
        val incorrectRoles = Roles.findIncorrect(roles)
        return if (incorrectRoles.isEmpty()) {
            actionIfCorrect()
        } else {
            DataOrError.incorrectArgument("roles", "Bad roles: [$incorrectRoles]")
        }
    }


    @PutMapping("/{userId}/roles")
    @ResponseBody
    fun putRoles(
        @PathVariable(name = "userId", required = true) userId: String,
        @RequestBody(required = true) roles: Set<String>
    ): EntityOrError<User> =
        rolesAreNotMisspelled(roles) {
            val eUser = userRepository.load(userId).flatMap {user ->
                if (user.roles == roles) {
                    Either.Right(user)
                } else {
                    val pUser = PersistsUser.of(user).copy(roles = roles)
                    userRepository.update(pUser)
                }
            }

            DataOrError.fromEither(eUser, log)
        }


    @PostMapping
    fun register(@RequestBody userInfo: PersistsUser): ResponseEntity<out DataOrError<out User>> {
        return rolesAreNotMisspelled<User>(userInfo.roles) {
            val result = either.eager<Exception, User> {
                userService.register(userInfo).bind()
            }
            DataOrError.fromEither(result, log)
        }
    }

    @GetMapping
    fun list(
        @AuthenticationPrincipal user: User,
        @RequestParam filter: String?,
        pagination: Repository.Pagination
    ): ResponseEntity<DataOrError<List<User>>> {
        return DataOrError.fromEither(userRepository.load(Option.fromNullable(filter), pagination), log)
    }

    @GetMapping("/{userId}")
    fun load(
        @PathVariable(
            name = "userId",
            required = true
        ) userId: String
    ): ResponseEntity<out DataOrError<out User>> {
        val eUser = userRepository.load(userId)
        return DataOrError.fromEither(eUser, log)

    }

    @GetMapping("/settings")
    fun settings(@AuthenticationPrincipal user: User): EntityOrError<SettingsResult> {
        val settings = userSettingsRepository
            .loadForUser(user)
            .map {SettingsResult(mustReissueJwt = null, settings = UserSettingsEditableProperties.of(it))}
        return DataOrError.fromEither(settings, log)
    }

    data class EditableSettings(
        val displayName: String,
        val subscriptionPlan: SubscriptionPlan,
        val settings: UserSettingsEditableProperties
    )

    data class SettingsResult(
        val mustReissueJwt: Boolean?,
        val settings: IUserSettingsEditableProperties
    )

    @PutMapping("/settings")
    fun updateSettings(
        @AuthenticationPrincipal user: User,
        @RequestBody settings: EditableSettings
    ): EntityOrError<SettingsResult> {
        val result = either.eager<Exception, SettingsResult> {
            val userFromStorage = userRepository.load(user.id).bind()

            val mustReissueJwt =
                if (userFromStorage.displayName != settings.displayName ||
                    userFromStorage.subscriptionPlan != settings.subscriptionPlan
                ) {
                    val nextUser = PersistsUser.of(user)
                        .copy(
                            displayName = settings.displayName,
                            subscriptionPlan = settings.subscriptionPlan
                        )
                    userRepository.update(nextUser).bind()
                    true
                } else {
                    false
                }

            val userSettings = userSettingsFactory.fromProperties(user, settings.settings)
            val nextSettings = userSettingsRepository
                .replace(userSettings)
                .map(UserSettingsEditableProperties::of)
                .bind()
            SettingsResult(mustReissueJwt, nextSettings)
        }

        return DataOrError.fromEither(result, log)
    }
}

class UserSecurity(private val controllerLog: KLogger) {

    /**
     * Check that user is owner of resource.
     */
    fun <T> asHimSelf(userId: String, user: User, action: () -> Either<Exception, T>): EntityOrError<T> {
        val result = if (userId == user.id) {
            action()
        } else {
            Either.Left(OperationNotPermitted())
        }

        return DataOrError.fromEither(result, controllerLog)
    }
}