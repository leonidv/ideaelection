package idel.api

import idel.domain.Roles
import idel.domain.UserRepository
import idel.infrastructure.repositories.PersistsUser
import idel.infrastructure.security.IdelOAuth2User
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/init")
class FirstSetupController(val userRepository: UserRepository)  {

    val HTML_TEMPLATE =
            "<html><title>Idea election initialization</title><html><body>#{content}</body>"

    val HTML_ALREADY_INITIALIZED_CONTENT = HTML_TEMPLATE.replace("#{content}","Already initialized. Use the admin panel for configuration.")

    val HTML_OK_CONTENT = "#{name}, all is ok! You are super user of installation."

    @GetMapping("", produces = arrayOf(MediaType.TEXT_HTML_VALUE))
    @ResponseBody
    fun init(authentication: Authentication) : String  {

        val user = authentication.principal as IdelOAuth2User

        val users = userRepository.load(0,10)

        // in this point current user already registered as user. User is registered by idel authorization flow.
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

        if (isInitialized) return HTML_ALREADY_INITIALIZED_CONTENT

        val persistsUser = PersistsUser.of(user).copy(roles = setOf(Roles.USER, Roles.SUPER_USER))
        userRepository.update(persistsUser)

        val responseContent = HTML_OK_CONTENT
            .replace("#{name}", user.displayName)
            .replace("#{id}", user.id())

        return HTML_TEMPLATE.replace("#{content}", responseContent)
    }
}