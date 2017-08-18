package org.jetbrains.research.kotoed.data.notification

import io.vertx.core.json.JsonObject
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import kotlinx.html.strong
import org.jetbrains.research.kotoed.database.tables.records.NotificationRecord
import org.jetbrains.research.kotoed.util.get
import org.jetbrains.research.kotoed.util.safeNav

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

fun render(notification: NotificationRecord): RenderedData {
    return when(NotificationType.valueOf(notification.type)) {
        NotificationType.COMMENT_CLOSED -> renderCommentClosed(notification.body as JsonObject)
        NotificationType.COMMENT_REOPENED -> renderCommentReopened(notification.body as JsonObject)
        NotificationType.NEW_COMMENT -> renderNewComment(notification.body as JsonObject)
        NotificationType.COMMENT_REPLIED_TO -> renderCommentRepliedTo(notification.body as JsonObject)
    }
}


