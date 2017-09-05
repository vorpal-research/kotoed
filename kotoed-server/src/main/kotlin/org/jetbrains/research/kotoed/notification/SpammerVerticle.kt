package org.jetbrains.research.kotoed.notification

import io.vertx.core.Future
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.research.kotoed.auxiliary.data.TimetableMessage
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.notification.*
import org.jetbrains.research.kotoed.database.enums.NotificationStatus
import org.jetbrains.research.kotoed.database.tables.records.DenizenRecord
import org.jetbrains.research.kotoed.database.tables.records.NotificationRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.web.UrlPattern
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset

@AutoDeployable
class SpammerVerticle: AbstractNotificationVerticle() {
    override fun start(startFuture: Future<Void>) {
        super.start(startFuture)
        launch(Unconfined) {
            setNext()
        }

    }

    suspend fun setNext() {
        run<Unit> {
            val nextEvent = LocalDateTime.now(ZoneOffset.UTC)
                    .withHour(14)
                    .withMinute(0)
                    .withSecond(0)
                    .plusDays(1)

            sendJsonableAsync(
                    Address.Schedule,
                    TimetableMessage(JsonObject(), nextEvent, Address.Notifications.Email.TriggerSpam)
            )
        }

    }

    @JsonableEventBusConsumerFor(Address.Notifications.Email.TriggerSpam)
    suspend fun handleTrigger(unit: Unit) {
        use(unit)
        setNext()

        val allDenizens = dbFindAsync(DenizenRecord())

        fun makeLink(link: LinkData): String {
            return "${Config.Root.Host}:${Config.Root.Port}" + UrlPattern.reverse(
                    UrlPattern.Redirect.ById,
                    link.toJson().map
            )
        }

        for(denizen in allDenizens) {
            if(denizen.email == null) continue

            val notifications: List<RenderedData> = sendJsonableCollectAsync(
                    Address.Api.Notification.RenderCurrent,
                    CurrentNotificationsQuery(denizen.id)
            )

            if(notifications.isNotEmpty()) {
                run<Unit> {
                    sendJsonableAsync(
                            Address.Notifications.Email.Send,
                            NotificationMessage(
                                    receiverId = denizen.id,
                                    service = NotificationService.EMAIL,
                                    subject = "You have ${notifications.size} unread notifications",
                                    contentsFormat = MessageFormat.HTML,
                                    contents = createHTML().div {
                                        h2 { +"Hello ${denizen.denizenId}," }
                                        p {
                                            +"""
                                                Kotoed here!
                                                You have some new updates currently waiting for you to see:
                                            """
                                        }
                                        notifications.forEach {
                                            div {
                                                attributes["style"] =
                                                        listOf(
                                                                "display:block",
                                                                "padding:10px 15px 10px 15px",
                                                                "border:1px solid #ddd",
                                                                "border-radius:4px"
                                                        ).joinToString(";")
                                                a(href = makeLink(it.linkTo)) {
                                                    span{ unsafe { +it.contents } }
                                                }
                                            }
                                        }
                                    }
                            )
                    )
                }
            }
        }

    }
}