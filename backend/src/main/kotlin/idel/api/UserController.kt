package idel.api

import arrow.core.*
import com.couchbase.client.core.error.DocumentExistsException
import idel.domain.Roles
import idel.domain.User
import idel.domain.UserRepository
import idel.infrastructure.repositories.PersistsUser
import idel.infrastructure.security.IdelOAuth2User
import mu.KLogger
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/users")
class UserController(val userRepository: UserRepository) {
    data class MeResult(val id: String,
                        val displayName: String,
                        val avatar: String,
                        val email: String,
                        val authorities: List<String>)


    val log: KLogger = KotlinLogging.logger {}

    @GetMapping("/me")
    @ResponseBody
    fun me(authentication: Authentication): ResponseEntity<DataOrError<MeResult>> {
        val principal = authentication.principal

        return if (principal is IdelOAuth2User) {
            val result = MeResult(
                    id = principal.id,
                    displayName = principal.displayName,
                    avatar = principal.avatar,
                    email = principal.email,
                    authorities = authentication.authorities.map {it.authority}
            )
            DataOrError.ok(result)
        } else {
            return DataOrError.internal("principal is not IdelOAuth2User");
        }
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
                        Either.right(user)
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
                userRepository.add(userInfo)
                DataOrError.ok(userInfo)
            } catch (ex: DocumentExistsException) {
                DataOrError.incorrectArgument("id", "User with same id already registered")
            }

        }
    }

    @GetMapping
    fun list(@RequestParam(required = false, defaultValue = "0") first: Int,
             @RequestParam(required = false, defaultValue = "10") last: Int): ResponseEntity<DataOrError<List<User>>> {
        val size = last - first;

        if (size <= 0) {
            return DataOrError.incorrectArgument("last: $last, first: $first", "first should be less then first")
        }

        if (size > 100) {
            val error = ErrorDescription.tooManyItems(size, 100);
            return DataOrError.errorResponse(error)
        }


        return try {
            DataOrError.ok(userRepository.load(first, last))

        } catch (e: Exception) {
            log.error(e) {"Can't load users list"}
            DataOrError.internal("Can't load data.  Exception message: " + e.message)
        }
    }

    @GetMapping("/{userId}")
    fun load(@PathVariable(name = "userId", required = true) userId: String): ResponseEntity<out DataOrError<out User>> {
        val eUser = userRepository.load(userId)
        return DataOrError.fromEither(eUser, log)

    }
}