package org.jetbrains.research.kotoed.notification

import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.run
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
import org.simplejavamail.mailer.Mailer
import org.simplejavamail.mailer.config.TransportStrategy


@AutoDeployable
class MailVerticle : AbstractNotificationVerticle(), Loggable {

    private val transport by lazy {
        when {
            Config.Notifications.Mail.UseTLS -> TransportStrategy.SMTP_TLS
            Config.Notifications.Mail.UseSSL -> TransportStrategy.SMTP_SSL
            else -> TransportStrategy.SMTP_PLAIN
        }
    }

    private val mailer by lazy {
        Mailer(
                Config.Notifications.Mail.ServerHost,
                Config.Notifications.Mail.ServerPort,
                Config.Notifications.Mail.User,
                Config.Notifications.Mail.Password,
                transport
        )
    }

    @JsonableEventBusConsumerFor(Address.Notifications.Email.Send)
    suspend fun sendEmail(message: NotificationMessage) {
        if (message.service != NotificationService.EMAIL)
            throw IllegalArgumentException("Email service expected")

        val denizen = fetchByIdAsync(Tables.DENIZEN, message.receiverId)

        if (denizen.email == null)
            throw IllegalArgumentException("User ${denizen.denizenId} does not have a specified email")

        val email =
                EmailBuilder()
                        .to(denizen.denizenId, denizen.email)
                        .from(Config.Notifications.Mail.KotoedSignature, Config.Notifications.Mail.KotoedAddress)
                        .replyTo(Config.Notifications.Mail.KotoedSignature, Config.Notifications.Mail.KotoedAddress)
                        .subject(message.subject)
                        .run {
                            if(message.contentsFormat == MessageFormat.HTML) {
                                textHTML(message.contents)
                            } else {
                                text(message.contents)
                            }
                        }
                        .build()


        log.info("Sending email to $denizen")

        spawn(Pool) { try{ mailer.sendMail(email) } catch(ex: Exception) { log.error("", ex) } }
    }

}