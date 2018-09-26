package org.jetbrains.research.kotoed.api

import org.jetbrains.research.kotoed.data.notification.CurrentNotificationsQuery
import org.jetbrains.research.kotoed.data.notification.RenderedData
import org.jetbrains.research.kotoed.data.notification.render
import org.jetbrains.research.kotoed.database.enums.NotificationStatus
import org.jetbrains.research.kotoed.database.tables.records.NotificationRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*

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


    @JsonableEventBusConsumerFor(Address.Api.Notification.Create)
    suspend fun handleCreate(query: NotificationRecord): NotificationRecord {
        val record = dbCreateAsync(query)

        publishJsonable(
                Address.Api.Notification.pushRendered(record.denizenId.toString()),
                render(record)
        )

        return record
    }

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