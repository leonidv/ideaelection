package idel.api

import arrow.core.None
import arrow.core.Some
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
import java.lang.Exception

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
    fun me(authentication: Authentication): ResponseEntity<ResponseOrError<MeResult>> {
        val principal = authentication.principal

        return if (principal is IdelOAuth2User) {
            val result = MeResult(
                    id = principal.id(),
                    displayName = principal.displayName,
                    avatar = principal.avatar,
                    email = principal.email,
                    authorities = authentication.authorities.map {it.authority}
            )
            ResponseOrError.ok(result)
        } else {
            return ResponseOrError.internal("principal is not IdelOAuth2User");
        }
    }

    /**
     * Check [roles] and if it correct call [actionIfCorrect]
     *
     * @param roles - roles which should be checked
     * @param actionIfCorrect - function which will be called if roles are correct
     */
    private fun <T> rolesAreNotMisspelled(roles: Set<String>, actionIfCorrect: () -> ResponseEntity<ResponseOrError<T>>): ResponseEntity<ResponseOrError<T>> {
        val incorrectRoles = Roles.findIncorrect(roles)
        return if (incorrectRoles.isEmpty()) {
            actionIfCorrect()
        } else {
            ResponseOrError.incorrectArgument("roles", "Bad roles: [$incorrectRoles]")
        }
    }

    @PutMapping("/{userId}/roles")
    @ResponseBody
    fun putRoles(@PathVariable(name = "userId", required = true) userId: String,
                 @RequestBody(required = true) roles: Set<String>): ResponseEntity<out ResponseOrError<out User>> {
        return rolesAreNotMisspelled(roles) {
            when (val userOption = userRepository.load(userId)) {
                is Some -> {
                    val user = userOption.t
                    if (user.roles == roles) {
                        ResponseOrError.ok(user)
                    } else {
                        val newUser = PersistsUser.of(user).copy(roles = roles)
                        ResponseOrError.fromLoading(userRepository.update(newUser), log)
                    }
                }
                is None -> {
                    ResponseOrError.notFound(userId)
                }
            }
        }
    }

    @PostMapping
    fun register(@RequestBody userInfo: PersistsUser): ResponseEntity<out ResponseOrError<out User>> {
        return rolesAreNotMisspelled<User>(userInfo.roles) {
            try {
                userRepository.add(userInfo)
                ResponseOrError.ok(userInfo)
            } catch (ex: DocumentExistsException) {
                ResponseOrError.incorrectArgument("id", "User with same id already registered")
            }

        }
    }

    @GetMapping
    fun list(@RequestParam(required = false, defaultValue = "0") first: Int,
             @RequestParam(required = false, defaultValue = "10") last: Int): ResponseEntity<ResponseOrError<List<User>>> {
        val size = last - first;

        if (size <= 0) {
            return ResponseOrError.incorrectArgument("last: $last, first: $first", "first should be less then first")
        }

        if (size > 100) {
            val error = ErrorDescription.tooManyItems(size, 100);
            return ResponseOrError.badRequest(error)
        }


        return try {
            ResponseOrError.ok(userRepository.load(first, last))

        } catch (e: Exception) {
            log.error(e) {"Can't load users list"}
            ResponseOrError.internal("Can't load data.  Exception message: " + e.message)
        }
    }

    @GetMapping("/{userId}")
    fun load(@PathVariable(name = "userId", required = true) userId: String): ResponseEntity<out ResponseOrError<out User>> {
       return ResponseOrError.fromLoading(userId, userRepository.load(userId))
    }
}