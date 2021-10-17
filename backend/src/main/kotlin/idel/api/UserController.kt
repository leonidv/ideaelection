package idel.api

import arrow.core.Either
import arrow.core.Option
import arrow.core.flatMap
import com.couchbase.client.core.error.DocumentExistsException
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
    private val userService: UserService
) {
    data class MeResult(val id: String,
                        val domain : String,
                        val displayName: String,
                        val avatar: String,
                        val email: String,
                        val authorities: List<String>)


    val log: KLogger = KotlinLogging.logger {}


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
    fun me2(@AuthenticationPrincipal user : User) : User {
        return user
    }
    /**
     * Check [roles] and if it correct call [actionIfCorrect]
     *
     * @param roles - roles which should be checked
     * @param actionIfCorrect - function which will be called if roles are correct
     */
    private fun <T> rolesAreNotMisspelled(roles: Set<String>, actionIfCorrect: () -> ResponseEntity<DataOrError<T>>): ResponseEntity<DataOrError<T>> {
        val incorrectRoles = Roles.findIncorrect(roles)
        return if (incorrectRoles.isEmpty()) {
            actionIfCorrect()
        } else {
            DataOrError.incorrectArgument("roles", "Bad roles: [$incorrectRoles]")
        }
    }


    @PutMapping("/{userId}/roles")
    @ResponseBody
    fun putRoles(@PathVariable(name = "userId", required = true) userId: String,
                 @RequestBody(required = true) roles: Set<String>): EntityOrError<User> =
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
            try {
                userService.register(userInfo)
                DataOrError.ok(userInfo)
            } catch (ex: DocumentExistsException) {
                DataOrError.incorrectArgument("id", "User with same id already registered")
            }

        }
    }

    @GetMapping
    fun list(@AuthenticationPrincipal user : User,
             @RequestParam filter : String?,
             pagination: Repository.Pagination): ResponseEntity<DataOrError<List<User>>> {
        return DataOrError.fromEither(userRepository.load(Option.fromNullable(filter), pagination), log)
    }

    @GetMapping("/{userId}")
    fun load(@PathVariable(name = "userId", required = true) userId: String): ResponseEntity<out DataOrError<out User>> {
        val eUser = userRepository.load(userId)
        return DataOrError.fromEither(eUser, log)

    }
}

class UserSecurity(private val controllerLog : KLogger) {

    /**
     * Check that user is owner of resource.
     */
    fun <T> asHimSelf(userId : String, user: User, action : () -> Either<Exception,T>) : EntityOrError<T>  {
       val result = if (userId == user.id) {
           action()
       } else {
           Either.Left(OperationNotPermitted())
       }

        return DataOrError.fromEither(result, controllerLog)
    }
}