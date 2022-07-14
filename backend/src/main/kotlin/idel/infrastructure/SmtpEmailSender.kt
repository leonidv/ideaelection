package idel.infrastructure

import arrow.core.Either
import arrow.core.continuations.either
import idel.domain.*
import mu.KotlinLogging
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.util.MimeType
import org.springframework.util.MimeTypeUtils
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.util.*
import javax.mail.Message
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


class SmtpEmailSender(
    private val mailSender: JavaMailSender,
    private val thymeleafEngine: TemplateEngine,
) : EmailSender {
    private val log = KotlinLogging.logger {}

    private val mailSenderImpl = mailSender as JavaMailSenderImpl;

    override fun sendInvite(personEmail: String, author: User, group: Group, inviteMessage : String): Either<DomainError, Unit> {
        log.trace {"Send invite: personEmail = [$personEmail]"}
        val context = Context(Locale.ENGLISH,
            mapOf(
                "author" to author,
                "group" to group,
                "message" to inviteMessage
            )
        )

        val emailText = thymeleafEngine.process("email-invite.html", context)

        val message = mailSender.createMimeMessage().apply {
            setFrom(InternetAddress(mailSenderImpl.username))
            setSubject("Invite to Saedi from ${author.displayName}")
            setRecipients(Message.RecipientType.TO, personEmail)
            setContent(emailText, MimeTypeUtils.TEXT_HTML_VALUE)
        }

        return Either.catch {
            mailSender.send(message)
        }.mapLeft {it.asError()}
    }
}