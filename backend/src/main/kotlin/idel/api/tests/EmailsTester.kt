package idel.api.tests

import arrow.core.Either
import idel.api.DataOrError
import idel.api.ResponseDataOrError
import idel.domain.*
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.util.*

@RestController
@RequestMapping("/emails")
class EmailsTester(val emailSender: EmailSender) {
    private val log = KotlinLogging.logger {}

    @Value("\${testmode}")
    private var testMode = false

    private fun <T> onlyInTestMode(action: () -> Either<DomainError, T>): Either<DomainError, T> {
        return if (testMode) {
            action()
        } else {
            Either.Left(OperationNotPermitted())
        }
    }

    @GetMapping("/send-test-invite")
    fun sendTestInvite(
        @AuthenticationPrincipal user: User,
        @RequestParam(required = true) to: String,
        @RequestParam(required = true) groupName: String,
        @RequestParam(required = true) groupDescription: String,
        @RequestParam(required = true) message: String
    ): ResponseDataOrError<String> {
        val result = onlyInTestMode {
            val invite = Invite.createForPerson(
                groupId = UUID.randomUUID(),
                email = to,
                message = "This is test invite",
                authorId = user.id
            )
            val fakeGroup = Group(
                id = UUID.randomUUID(),
                ctime = LocalDateTime.now(),
                creator = UserInfo.ofUser(user),
                state = GroupState.ACTIVE,
                name = groupName,
                description = groupDescription,
                logo = "",
                entryMode = GroupEntryMode.CLOSED,
                entryQuestion = "",
                domainRestrictions = emptyList(),
                membersCount = 1,
                ideasCount = 1,
                joiningKey = "inviteTestJK"
            )
            emailSender.sendInvite(to, user, fakeGroup, message).map {"Email was sent to $to"}
        }

        return DataOrError.fromEither(result, log);
    }
}