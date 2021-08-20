package org.jetbrains.research.kotoed.api

import io.vertx.core.json.JsonObject
import kotlinx.warnings.Warnings.UNUSED_PARAMETER
import nl.martijndwars.webpush.Encoding
import nl.martijndwars.webpush.Notification
import nl.martijndwars.webpush.PushService
import org.apache.http.HttpResponse
import org.apache.http.concurrent.FutureCallback
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.notification.CurrentNotificationsQuery
import org.jetbrains.research.kotoed.data.notification.RenderedKind
import org.jetbrains.research.kotoed.data.notification.WebNotificationSubscription
import org.jetbrains.research.kotoed.data.notification.render
import org.jetbrains.research.kotoed.database.enums.NotificationStatus
import org.jetbrains.research.kotoed.database.tables.records.NotificationRecord
import org.jetbrains.research.kotoed.database.tables.records.PushSubscriptionRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun PushService.sendSuspendable(notification: Notification): HttpResponse = suspendCoroutine { cont ->
    preparePost(notification, Encoding.AESGCM).also { post ->
        val client = HttpAsyncClients.createSystem()
        client.start()
        client.execute(post, object: FutureCallback<HttpResponse> {
            override fun cancelled() {
                cont.resumeWithException(Exception("Cancelled"))
                client.close()
            }

            override fun completed(result: HttpResponse) {
                cont.resume(result)
                client.close()
            }

            override fun failed(ex: Exception) {
                cont.resumeWithException(ex)
                client.close()
            }
        });
    }
}


@AutoDeployable
class NotificationVerticle : AbstractKotoedVerticle() {
    @JsonableEventBusConsumerFor(Address.Api.Notification.Current)
    suspend fun handleCurrent(query: CurrentNotificationsQuery) =
            dbFindAsync(
                    NotificationRecord().apply {
                        denizenId = query.denizenId
                        status = NotificationStatus.unread
                    }
            )

    @JsonableEventBusConsumerFor(Address.Api.Notification.RenderCurrent)
    suspend fun handleRenderCurrent(query: CurrentNotificationsQuery) =
            handleCurrent(query).map(::render)

    @JsonableEventBusConsumerFor(Address.Api.Notification.MarkRead)
    suspend fun handleMarkRead(query: NotificationRecord) =
            dbUpdateAsync(
                    dbFetchAsync(query).apply {
                        status = NotificationStatus.read
                    }
            )

    @JsonableEventBusConsumerFor(Address.Api.Notification.MarkAllRead)
    suspend fun handleMarkAllRead(query: NotificationRecord) =
            dbBatchUpdateAsync(
                    NotificationRecord().apply {
                        denizenId = query.denizenId
                    },
                    NotificationRecord().apply {
                        status = NotificationStatus.read
                    })

    suspend fun sendWebNotifications(record: NotificationRecord) {

        if(Config.Notifications.Web.VapidKeyPublic == ""
        || Config.Notifications.Web.VapidKeyPrivate == "") {
            log.warn("No VAPID keys found, web push notifications are not available")
            return
        }

        dbFindAsync(PushSubscriptionRecord().apply { denizenId = record.denizenId }).forEach { sub ->
            val query: WebNotificationSubscription = (sub.subscriptionObject as? JsonObject)?.toJsonable() ?: return@forEach

            log.info("Pushing notifications to web for user ${sub.denizenId}")

            val not = Notification(query.endpoint, query.getUserPublicKey(), query.getAuthBytes(),
                    render(record, RenderedKind.RAW).toJson().encode().toByteArray())

            val pushService = PushService(
                    Config.Notifications.Web.VapidKeyPublic,
                    Config.Notifications.Web.VapidKeyPrivate,
                    "Kotoed"
            )

            val resp = pushService.sendSuspendable(not)

            log.info("Response: ${resp.statusLine}")
        }
    }

    @JsonableEventBusConsumerFor(Address.Api.Notification.Create)
    suspend fun handleCreate(query: NotificationRecord): NotificationRecord {
        val record = dbCreateAsync(query)

        publishJsonable(
                Address.Api.Notification.pushRendered(record.denizenId.toString()),
                render(record)
        )

        sendWebNotifications(record)

        return record
    }

    @JsonableEventBusConsumerFor(Address.Api.Notification.Web.Subscribe)
    suspend fun handleWebSubscribe(query: WebNotificationSubscription): PushSubscriptionRecord {
        val record = dbCreateAsync(PushSubscriptionRecord().apply {
            denizenId = query.denizenId
            subscriptionObject = query.toJson()
        })

        return record
    }

    @JsonableEventBusConsumerFor(Address.Api.Notification.Web.PublicKey)
    fun handleWebKey(@Suppress(UNUSED_PARAMETER) ignore: Unit): JsonObject =
            org.jetbrains.research.kotoed.util.JsonObject("key" to Config.Notifications.Web.VapidKeyPublic)

    @JsonableEventBusConsumerFor(Address.Api.Notification.Read)
    suspend fun handleRead(query: NotificationRecord) =
            dbFetchAsync(
                    NotificationRecord().apply {
                        id = query.id ?: throw NotFound("Notification not found: id = ${query.id}")
                    }
            )

    @JsonableEventBusConsumerFor(Address.Api.Notification.Render)
    suspend fun handleRender(query: NotificationRecord) =
            render(handleRead(query))

}
