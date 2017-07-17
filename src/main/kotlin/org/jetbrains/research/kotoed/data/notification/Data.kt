package org.jetbrains.research.kotoed.data.notification

import org.jetbrains.research.kotoed.util.Jsonable

data class CurrentNotificationsQuery(val denizenId: Int): Jsonable

enum class NotificationService{ EMAIL }

data class NotificationMessage(
        val receiverId: Int,
        val service: NotificationService,
        val subject: String,
        val contents: String
): Jsonable
