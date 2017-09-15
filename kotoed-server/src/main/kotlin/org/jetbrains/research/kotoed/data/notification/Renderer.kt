package org.jetbrains.research.kotoed.data.notification

import io.vertx.core.json.JsonObject
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import kotlinx.html.strong
import org.jetbrains.research.kotoed.database.tables.records.NotificationRecord
import org.jetbrains.research.kotoed.util.get
import org.jetbrains.research.kotoed.util.safeNav
import org.jetbrains.research.kotoed.util.uncheckedCast

private fun renderCommentClosed(id: Int, body: JsonObject): RenderedData {
    val node = createHTML().div {
        strong { +"${body.safeNav("author", "denizenId")}" }
        +" closed his comment to "
        strong { +"submission #${body["submissionId"]}" }
    }

    val link = LinkData("comment", body["id"].toString())
    return RenderedData(id, node, link)
}

private fun renderCommentReopened(id: Int, body: JsonObject): RenderedData {
    val node = createHTML().div {
        strong { +"${body.safeNav("author", "denizenId")}" }
        +" reopened his comment to "
        strong { +"submission #${body["submissionId"]}" }
    }
    val link = LinkData("comment", body["id"].toString())
    return RenderedData(id, node, link)
}

private fun renderNewComment(id: Int, body: JsonObject): RenderedData {
    val node = createHTML().div {
        strong { +"${body.safeNav("author", "denizenId")}" }
        +" wrote a comment to "
        strong { +"submission #${body["submissionId"]}" }
    }
    val link = LinkData("comment", body["id"].toString())
    return RenderedData(id, node, link)
}

private fun renderCommentRepliedTo(id: Int, body: JsonObject): RenderedData {
    val node = createHTML().div {
        strong { +"${body.safeNav("author", "denizenId")}" }
        +" wrote a comment to "
        strong { +"submission #${body["submissionId"]}" }
    }
    val link = LinkData("comment", body["id"].toString())
    return RenderedData(id, node, link)
}

private fun renderNewSubmissionResults(id: Int, body: JsonObject): RenderedData {
    val node = createHTML().div {
        +"New results for "
        strong { +"submission #${body.safeNav("submissionId")}" }
    }
    val link = LinkData("submissionResults", body.safeNav("submissionId").toString())
    return RenderedData(id, node, link)
}

private fun renderResubmission(id: Int, body: JsonObject): RenderedData {
    val node = createHTML().div {
        strong { +"${body.safeNav("author", "denizenId")}" }
        +" resubmitted his "
        strong { +"submission #${body["oldSubmissionId"]}" }
    }
    val link = LinkData("submission", body.safeNav("submissionId").toString())
    return RenderedData(id, node, link)
}

private fun renderSubmissionUpdate(id: Int, body: JsonObject): RenderedData {
    val pastParticiple = if (body.safeNav("state").toString() == "open") "reopened" else "closed"
    val node = createHTML().div {
        strong { +"Submission #${body["id"]}" }
        + " was $pastParticiple"
    }
    val link = LinkData("submission", body["id"].toString())
    return RenderedData(id, node, link)
}

internal val renderers by lazy {
    mapOf(
            NotificationType.COMMENT_CLOSED to ::renderCommentClosed,
            NotificationType.COMMENT_REOPENED to ::renderCommentReopened,
            NotificationType.NEW_COMMENT to ::renderNewComment,
            NotificationType.COMMENT_REPLIED_TO to ::renderCommentRepliedTo,
            NotificationType.NEW_SUBMISSION_RESULTS to ::renderNewSubmissionResults,
            NotificationType.RESUBMISSION to ::renderResubmission,
            NotificationType.SUBMISSION_UPDATE to ::renderSubmissionUpdate
    )
}

fun render(notification: NotificationRecord): RenderedData {
    val type = NotificationType.valueOf(notification.type)
    val body = notification.body.uncheckedCast<JsonObject>()
    return renderers[type]!!(notification.id, body)
}


