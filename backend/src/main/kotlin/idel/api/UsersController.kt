package idel.api

import arrow.core.Either
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
import java.util.*


@RestController
@RequestMapping("/users")
class UsersController(
    private val userRepository: UserRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val userService: UserService
) {
    data class MeResult(
        val id: UUID,
        val externalId: String,
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
                externalId = principal.externalId,
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
        @PathVariable(name = "userId", required = true) userId: UUID,
        @RequestBody(required = true) roles: Set<String>
    ): ResponseDataOrError<User> =
        rolesAreNotMisspelled(roles) {
            val eUser = fTransaction {
                userRepository.load(userId).flatMap {user ->
                    if (user.roles == roles) {
                        Either.Right(user)
                    } else {
                        val pUser = PersistsUser.of(user).copy(roles = roles)
                        userRepository.update(pUser)
                    }
                }
            }

            DataOrError.fromEither(eUser, log)
        }


    @PostMapping
    fun register(@RequestBody userInfo: PersistsUser): ResponseEntity<out DataOrError<out User>> {
        return rolesAreNotMisspelled<User>(userInfo.roles) {
            val result = either.eager<DomainError, User> {
                val normalizedUserInfo = PersistsUser.of(userInfo)
                userService.register(normalizedUserInfo).bind()
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
        val result = fTransaction {
            userRepository.list(filter, pagination)
        }
        return DataOrError.fromEither(result, log)
    }

    @GetMapping("/{userId}")
    fun load(
        @PathVariable(
            name = "userId",
            required = true
        ) userId: UserId
    ): ResponseEntity<out DataOrError<out User>> {
        val eUser = fTransaction {userRepository.load(userId)}
        return DataOrError.fromEither(eUser, log)

    }

    @GetMapping("/settings")
    fun loadSettings(@AuthenticationPrincipal user: User): ResponseDataOrError<SettingsResult> {
        val settings = fTransaction {
            userSettingsRepository
                .loadForUser(user)
                .map {SettingsResult(mustReissueJwt = null, settings = UserSettingsEditableProperties.of(it))}
        }
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
    ): ResponseDataOrError<SettingsResult> {
        val result = fTransaction {
            either.eager<DomainError, SettingsResult> {
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

                val nextSettings = userSettingsRepository
                    .update(user.id, settings.settings)
                    .bind()
                SettingsResult(mustReissueJwt, nextSettings)
            }
        }

        return DataOrError.fromEither(result, log)
    }
}
