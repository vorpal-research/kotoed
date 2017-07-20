package org.jetbrains.research.kotoed.api

import org.jetbrains.research.kotoed.data.notification.CurrentNotificationsQuery
import org.jetbrains.research.kotoed.database.enums.NotificationStatus
import org.jetbrains.research.kotoed.database.tables.records.NotificationRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.AbstractKotoedVerticle
import org.jetbrains.research.kotoed.util.AutoDeployable
import org.jetbrains.research.kotoed.util.JsonableEventBusConsumerFor

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

    @JsonableEventBusConsumerFor(Address.Api.Notification.MarkRead)
    suspend fun handleMarkRead(query: NotificationRecord) =
            dbUpdateAsync(
                    dbFetchAsync(query).apply {
                        status = NotificationStatus.read
                    }
            )

}