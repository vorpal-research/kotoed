package org.jetbrains.research.kotoed.notification

import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.notification.MessageFormat
import org.jetbrains.research.kotoed.data.notification.NotificationMessage
import org.jetbrains.research.kotoed.data.notification.NotificationService
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.simplejavamail.email.Email
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder
import org.simplejavamail.mailer.config.TransportStrategy
import java.util.concurrent.LinkedBlockingQueue

@AutoDeployable
class MailVerticle : AbstractNotificationVerticle(), Loggable {

    private val transport by lazy {
        when {
            Config.Notifications.Mail.UseTLS -> TransportStrategy.SMTP_TLS
            Config.Notifications.Mail.UseSSL -> TransportStrategy.SMTPS
            else -> TransportStrategy.SMTP.apply { setOpportunisticTLS(false) }
        }
    }

    private val mailer by lazy {
        MailerBuilder
                .withSMTPServer(
                        Config.Notifications.Mail.ServerHost,
                        Config.Notifications.Mail.ServerPort,
                        Config.Notifications.Mail.User,
                        Config.Notifications.Mail.Password
                )
                .withTransportStrategy(transport)
                .buildMailer()
    }

    private val queue = LinkedBlockingQueue<Email>()
    private enum class State { FREE, WORKING, PENDING }
    private var state = State.FREE

    fun sendPending() {
        state = State.WORKING
        log.info("Sending emails")

        while(queue.isNotEmpty()) {
            val current = queue.peek() ?: break
            try { mailer.sendMail(current, true) }
            catch(ex: Exception) {
                log.error("Error while trying to send mail", ex)
                break
            }
            queue.remove()
        }

        if(queue.isNotEmpty()) {
            log.warn("Pending emails found, reattempt in 2 minutes...")
            state = State.PENDING
            spawn(Pool) { sendPending() }
        } else {
            state = State.FREE
        }
    }

    @JsonableEventBusConsumerFor(Address.Notifications.Email.Send)
    suspend fun sendEmail(message: NotificationMessage) {
        if (message.service != NotificationService.EMAIL)
            throw IllegalArgumentException("Email service expected")

        val denizen = fetchByIdAsync(Tables.DENIZEN, message.receiverId)

        if (denizen.email.isNullOrBlank())
            throw IllegalArgumentException("User ${denizen.denizenId} does not have a specified email")

        val email =
                EmailBuilder
                        .startingBlank()
                        .to(denizen.denizenId, denizen.email)
                        .from(Config.Notifications.Mail.KotoedSignature, Config.Notifications.Mail.KotoedAddress)
                        .withReplyTo(Config.Notifications.Mail.KotoedSignature, Config.Notifications.Mail.KotoedAddress)
                        .withSubject(message.subject)
                        .apply {
                            if(message.contentsFormat == MessageFormat.HTML) {
                                withHTMLText(message.contents)
                            } else {
                                withPlainText(message.contents)
                            }
                        }
                        .buildEmail()

        log.info("Queueing email to $denizen")

        queue.add(email)

        if (state == State.FREE) spawn(Pool) { sendPending() }
    }

}
