package org.jetbrains.research.kotoed.data.notification

import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.research.kotoed.database.tables.records.NotificationRecord
import org.jetbrains.research.kotoed.util.get
import org.jetbrains.research.kotoed.util.safeNav
import org.jetbrains.research.kotoed.util.uncheckedCast
import org.w3c.dom.events.Event

class RawTagConsumer : TagConsumer<String> {
    override fun onTagComment(content: CharSequence) {
        TODO("Comment tags are not supported")
    }

    override fun finalize(): String = contents.toString()

    override fun onTagAttributeChange(tag: Tag, attribute: String, value: String?) {}

    override fun onTagContent(content: CharSequence) {
        contents.append(content)
    }

    override fun onTagContentEntity(entity: Entities) {
        contents.append(entity.text)
    }

    override fun onTagContentUnsafe(block: Unsafe.() -> Unit) {
        val unsafe = object : Unsafe {
            override fun String.unaryPlus() {
                contents.append(this)
            }
        }
        unsafe.block()
    }

    override fun onTagEnd(tag: Tag) {}

    override fun onTagEvent(tag: Tag, event: String, value: (Event) -> Unit) {}

    override fun onTagStart(tag: Tag) {}

    var contents: StringBuilder = StringBuilder()
}

enum class RenderedKind { HTML, RAW }

fun RenderedKind.buildConsumer() = when (this) {
    RenderedKind.HTML -> createHTML()
    RenderedKind.RAW -> RawTagConsumer()
}

private fun renderCommentClosed(id: Int, body: JsonObject, kind: RenderedKind = RenderedKind.HTML): RenderedData {
    val node = kind.buildConsumer().div {
        strong { +"${body.safeNav("author", "denizenId")}" }
        +" closed his comment to "
        strong { +"submission #${body["submissionId"]}" }
    }

    val link = LinkData("comment", body["id"].toString())
    return RenderedData(id, node, link)
}

private fun renderCommentReopened(id: Int, body: JsonObject, kind: RenderedKind = RenderedKind.HTML): RenderedData {
    val node = kind.buildConsumer().div {
        strong { +"${body.safeNav("author", "denizenId")}" }
        +" reopened his comment to "
        strong { +"submission #${body["submissionId"]}" }
    }
    val link = LinkData("comment", body["id"].toString())
    return RenderedData(id, node, link)
}

private fun renderNewComment(id: Int, body: JsonObject, kind: RenderedKind = RenderedKind.HTML): RenderedData {
    val node = kind.buildConsumer().div {
        strong { +"${body.safeNav("author", "denizenId")}" }
        +" wrote a comment to "
        strong { +"submission #${body["submissionId"]}" }
    }
    val link = LinkData("comment", body["id"].toString())
    return RenderedData(id, node, link)
}

private fun renderCommentRepliedTo(id: Int, body: JsonObject, kind: RenderedKind = RenderedKind.HTML): RenderedData {
    val node = kind.buildConsumer().div {
        strong { +"${body.safeNav("author", "denizenId")}" }
        +" wrote a comment to "
        strong { +"submission #${body["submissionId"]}" }
    }
    val link = LinkData("comment", body["id"].toString())
    return RenderedData(id, node, link)
}

private fun renderNewSubmissionResults(id: Int, body: JsonObject, kind: RenderedKind = RenderedKind.HTML): RenderedData {
    val node = kind.buildConsumer().div {
        +"New results for "
        strong { +"submission #${body.safeNav("submissionId")}" }
    }
    val link = LinkData("submissionResults", body.safeNav("submissionId").toString())
    return RenderedData(id, node, link)
}

private fun renderResubmission(id: Int, body: JsonObject, kind: RenderedKind = RenderedKind.HTML): RenderedData {
    val times = (body["times"] as? Int)?.let {
        if (it == 1) "" else " $it times"
    } ?: ""
    val node = kind.buildConsumer().div {
        strong { +"${body.safeNav("author", "denizenId")}" }
        +" resubmitted his "
        strong { +"submission #${body["oldSubmissionId"]}" }
        +times
    }
    val link = LinkData("submission", body.safeNav("submissionId").toString())
    return RenderedData(id, node, link)
}

private fun renderSubmissionUpdate(id: Int, body: JsonObject, kind: RenderedKind = RenderedKind.HTML): RenderedData {
    val pastParticiple = if (body.safeNav("state").toString() == "open") "reopened" else "closed"
    val node = kind.buildConsumer().div {
        strong { +"Submission #${body["id"]}" }
        +" was $pastParticiple"
    }
    val link = LinkData("submission", body["id"].toString())
    return RenderedData(id, node, link)
}

private fun renderCustomNotification(id: Int, body: JsonObject, kind: RenderedKind = RenderedKind.HTML): RenderedData {
    val node = kind.buildConsumer().pre {
        +body.encodePrettily()
    }
    val link = LinkData(".", ".")
    return RenderedData(id, node, link)
}

private fun renderInvalid(id: Int, kind: RenderedKind = RenderedKind.HTML): RenderedData {
    val node = kind.buildConsumer().div {
        strong { +"Invalid notification" }
    }
    val link = LinkData(".", ".")
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
            NotificationType.SUBMISSION_UPDATE to ::renderSubmissionUpdate,
            NotificationType.CUSTOM_NOTIFICATION to ::renderCustomNotification
    )
}

fun render(notification: NotificationRecord): RenderedData = render(notification, RenderedKind.HTML)

fun render(notification: NotificationRecord, kind: RenderedKind): RenderedData {
    val type = NotificationType.valueOf(notification.type)
    val body = notification.body as? JsonObject ?: jsonObjectOf("data" to notification.body)
    return try {
        renderers[type]!!(notification.id, body, kind)
    } catch (ex: Exception) {
        renderInvalid(notification.id, kind)
    }
}
