package org.jetbrains.research.kotoed.api

import com.google.common.cache.CacheBuilder
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.api.RestorePasswordSecret
import org.jetbrains.research.kotoed.data.db.LoginMsg
import org.jetbrains.research.kotoed.data.notification.MessageFormat
import org.jetbrains.research.kotoed.data.notification.NotificationMessage
import org.jetbrains.research.kotoed.data.notification.NotificationService
import org.jetbrains.research.kotoed.database.tables.records.DenizenRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.web.UrlPattern
import java.util.*
import java.util.concurrent.TimeUnit

@AutoDeployable
class RestoreAuthVerticle : AbstractKotoedVerticle(), Loggable {

    val requestCache = CacheBuilder
            .newBuilder()
            .expireAfterAccess(24, TimeUnit.HOURS)
            .build<String, UUID>()

    @JsonableEventBusConsumerFor(Address.User.Auth.Restore)
    suspend fun handleRestore(denizen: DenizenRecord) {
        log.info("Password restoration request from ${denizen.denizenId}")
        val uid = UUID.randomUUID()
        requestCache.put(denizen.denizenId, uid)

        val callback = "${Config.Root.Host}:${Config.Root.Port}" +
                UrlPattern.reverse(
                        UrlPattern.Auth.RestorePassword,
                        mapOf("uid" to uid)
                )

        run<Unit> {
            sendJsonableAsync(
                    Address.Notifications.Email.Send,
                    NotificationMessage(
                            receiverId = denizen.id,
                            service = NotificationService.EMAIL,
                            subject = "Password reset requested for user ${denizen.denizenId}",
                            contentsFormat = MessageFormat.HTML,
                            contents = createHTML().div {
                                h2 { +"Hello ${denizen.denizenId}," }
                                p {
                                    +"""
                                    A password change has been requested for you account in Kotoed.
                                    If you want to reset your password, please follow the link below.
                                """
                                }
                                p {
                                    +"""
                                    Otherwise, just ignore this message
                                """
                                }
                                a(href = callback) { +callback }
                            }
                    ))
        }

    }

    @JsonableEventBusConsumerFor(Address.User.Auth.RestoreSecret)
    suspend fun handleRestoreSecret(request: RestorePasswordSecret) {
        log.info("Password restoration callback from ${request.denizenId}" +
                "with secret '${request.secret}'")

        val secret = UUID.fromString(request.secret)
        if (request.denizenId in requestCache
                && requestCache[request.denizenId] == secret) {

            requestCache.invalidate(request.denizenId)

            run<Unit> {
                sendJsonableAsync(Address.User.Auth.SetPassword, LoginMsg(request.denizenId, request.password))
            }
        } else throw Forbidden("Illegal request")
    }

}