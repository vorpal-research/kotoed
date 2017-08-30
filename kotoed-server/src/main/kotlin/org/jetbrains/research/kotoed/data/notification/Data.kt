package org.jetbrains.research.kotoed.data.notification

import org.jetbrains.research.kotoed.util.Jsonable

data class CurrentNotificationsQuery(val denizenId: Int): Jsonable

enum class NotificationService{ EMAIL }
enum class MessageFormat { PLAIN, HTML }

data class NotificationMessage(
        val receiverId: Int,
        val service: NotificationService,
        val subject: String,
        val contentsFormat: MessageFormat,
        val contents: String
): Jsonable

enum class NotificationType {
    NEW_COMMENT,
    COMMENT_REPLIED_TO,
    COMMENT_CLOSED,
    COMMENT_REOPENED
}

data class LinkData(
        val entity: String,
        val id: String
): Jsonable

data class RenderedData(
        val id: Int,
        val contents: String,
        val linkTo: LinkData
): Jsonable
