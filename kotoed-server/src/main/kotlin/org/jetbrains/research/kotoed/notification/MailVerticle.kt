package org.jetbrains.research.kotoed.notification

import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.notification.MessageFormat
import org.jetbrains.research.kotoed.data.notification.NotificationMessage
import org.jetbrains.research.kotoed.data.notification.NotificationService
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.AutoDeployable
import org.jetbrains.research.kotoed.util.JsonableEventBusConsumerFor
import org.jetbrains.research.kotoed.util.Loggable
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder
import org.simplejavamail.mailer.config.TransportStrategy

@AutoDeployable
class MailVerticle : AbstractNotificationVerticle(), Loggable {

    private val transport by lazy {
        when {
            Config.Notifications.Mail.UseTLS -> TransportStrategy.SMTP_TLS
            Config.Notifications.Mail.UseSSL -> TransportStrategy.SMTPS
            else -> TransportStrategy.SMTP
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


        log.info("Sending email to $denizen")

        spawn(Pool) { try{ mailer.sendMail(email, true) } catch(ex: Exception) { log.error("", ex) } }
    }

}
