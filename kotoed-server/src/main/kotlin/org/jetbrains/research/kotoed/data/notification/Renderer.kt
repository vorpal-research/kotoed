package org.jetbrains.research.kotoed.data.notification

import io.vertx.core.json.JsonObject
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import kotlinx.html.strong
import org.jetbrains.research.kotoed.database.tables.records.NotificationRecord
import org.jetbrains.research.kotoed.util.get
import org.jetbrains.research.kotoed.util.safeNav
import org.jetbrains.research.kotoed.util.uncheckedCast
import org.jetbrains.research.kotoed.util.uncheckedCastOrNull

private fun renderCommentClosed(body: JsonObject): RenderedData {
    val node = createHTML().div {
        strong { +"${body.safeNav("author", "denizenId")}" }
        +" closed his comment to "
        strong { +"submission #${body["submissionId"]}" }
    }

    val link = LinkData("comment", body["id"].toString())
    return RenderedData(node, link)
}
private fun renderCommentReopened(body: JsonObject): RenderedData {
    val node = createHTML().div {
        strong { +"${body.safeNav("author", "denizenId")}" }
        +" reopened his comment to "
        strong { +"submission #${body["submissionId"]}" }
    }
    val link = LinkData("comment", body["id"].toString())
    return RenderedData(node, link)
}
private fun renderNewComment(body: JsonObject): RenderedData {
    val node = createHTML().div {
        strong { +"${body.safeNav("author", "denizenId")}" }
        +" wrote a comment to "
        strong { +"submission #${body["submissionId"]}" }
    }
    val link = LinkData("comment", body["id"].toString())
    return RenderedData(node, link)
}
private fun renderCommentRepliedTo(body: JsonObject): RenderedData {
    val node = createHTML().div {
        strong { +"${body.safeNav("author", "denizenId")}" }
        +" wrote a comment to "
        strong { +"submission #${body["submissionId"]}" }
    }
    val link = LinkData("comment", body["id"].toString())
    return RenderedData(node, link)
}

internal val renderers by lazy {
    mapOf(
            NotificationType.COMMENT_CLOSED to ::renderCommentClosed,
            NotificationType.COMMENT_REOPENED to ::renderCommentReopened,
            NotificationType.NEW_COMMENT to ::renderNewComment,
            NotificationType.COMMENT_REPLIED_TO to ::renderCommentRepliedTo
    )
}

fun render(notification: NotificationRecord): RenderedData {
    val type = NotificationType.valueOf(notification.type)
    val body = notification.body.uncheckedCast<JsonObject>()
    return renderers[type]!!(body)
}


