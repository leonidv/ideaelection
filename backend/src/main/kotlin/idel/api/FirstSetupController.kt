package idel.api

import arrow.core.continuations.either
import idel.domain.*
import idel.infrastructure.repositories.PersistsUser
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/init")
class FirstSetupController(val userRepository: UserRepository) {
    companion object {
        const val HTML_TEMPLATE =
            "<html><title>Idea election initialization</title><html><body>#{content}</body>"

        val HTML_ALREADY_INITIALIZED_CONTENT =
            this.HTML_TEMPLATE.replace("#{content}", "Already initialized. Use the admin panel for configuration.")

        const val HTML_OK_CONTENT = "#{name}, all is ok! You are super user of installation."

    }

    private val log = KotlinLogging.logger {}

    @GetMapping("", produces = [MediaType.TEXT_HTML_VALUE])
    @ResponseBody
    fun init(@AuthenticationPrincipal user: User): ResponseDataOrError<String> {

        val result = fTransaction {
            either.eager {
                val users = userRepository.list(null, Repository.Pagination(0, 10)).bind()
                val isInitialized =
                    when {
                        user.roles.contains(Roles.SUPER_USER) -> {
                            true
                        }
                        users.size > 1 -> {
                            true
                        }

                        else -> {
                            false
                        }
                    }
                isInitialized
                if (isInitialized) {
                    HTML_ALREADY_INITIALIZED_CONTENT
                } else {
                    val persistsUser = PersistsUser.of(user).copy(roles = setOf(Roles.USER, Roles.SUPER_USER))
                    userRepository.update(persistsUser)

                    val responseContent = HTML_OK_CONTENT
                        .replace("#{name}", user.displayName)
                        .replace("#{id}", user.id.toString())

                    HTML_TEMPLATE.replace("#{content}", responseContent)
                }
            }
        }

        return DataOrError.fromEither(result, log)
    }
}