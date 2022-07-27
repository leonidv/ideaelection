package idel.domain

import arrow.core.Either


interface EmailSender {
    /**
     * Send invite via email.
     */
    fun sendInvite(personEmail: String, author: User, group: Group, inviteMessage: String): Either<DomainError, Unit>
}