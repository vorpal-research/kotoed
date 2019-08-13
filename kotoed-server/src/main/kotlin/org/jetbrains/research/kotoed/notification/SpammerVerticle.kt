package org.jetbrains.research.kotoed.notification

import io.vertx.core.Promise
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.research.kotoed.auxiliary.data.TimetableMessage
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.notification.*
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.DenizenRecord
import org.jetbrains.research.kotoed.database.tables.records.ProfileRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toRecord
import org.jetbrains.research.kotoed.web.UrlPattern
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset

@AutoDeployable
class SpammerVerticle : AbstractNotificationVerticle(), Loggable {
    override fun start(startPromise: Promise<Void>) {
        super.start(startPromise)
        spawn {
            setNext()
        }
    }

    suspend fun setNext() {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        var nextEvent = now
                .withHour(Config.Notifications.Mail.SendTime.Hours)
                .withMinute(Config.Notifications.Mail.SendTime.Minutes)
                .withSecond(0)
                .withNano(0)

        if (nextEvent <= now) nextEvent = nextEvent.plusDays(1)

        log.info("Scheduling next spam session at $nextEvent")

        var tryOut: Unit? = null
        while(tryOut == null) {
            log.info("Trying to send message to timetable")
            tryOut = trySendJsonableAsync(
                    Address.Schedule,
                    TimetableMessage(JsonObject(), nextEvent, Address.Notifications.Email.TriggerSpam)
            )
            if(tryOut != null) break

            log.info("Scheduler not available, retrying in 5 minutes")
            vertx.delayAsync(Duration.ofMinutes(5).toMillis())
        }

    }

    @JsonableEventBusConsumerFor(Address.Notifications.Email.TriggerSpam)
    suspend fun handleTrigger(unit: Unit) {
        log.info("Sending spam in 3..2..1..")

        use(unit)
        setNext()

        val allDenizens = dbQueryAsync(Tables.PROFILE) { join(Tables.DENIZEN) }

        fun makeLink(link: RenderedData): String {
            return "${Config.Root.Host}:${Config.Root.Port}" + UrlPattern.reverse(
                    UrlPattern.Notification.ById,
                    mapOf("id" to link.id)
            )
        }

        for (profileJson in allDenizens) {
            val profile: ProfileRecord = profileJson.toRecord()
            val denizen: DenizenRecord = profileJson.getJsonObject("denizen").toRecord()
            if (denizen.email.isNullOrBlank() || !profile.emailNotifications) continue

            val notifications: List<RenderedData> = sendJsonableCollectAsync(
                    Address.Api.Notification.RenderCurrent,
                    CurrentNotificationsQuery(denizen.id)
            )

            if (notifications.isNotEmpty()) {
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
                                                a(href = makeLink(it)) {
                                                    span { unsafe { +it.contents } }
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