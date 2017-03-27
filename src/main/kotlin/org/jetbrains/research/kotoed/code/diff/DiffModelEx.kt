package org.jetbrains.research.kotoed.code.diff

import org.jetbrains.research.kotoed.util.JsonObject
import org.wickedsource.diffparser.api.model.Diff
import org.wickedsource.diffparser.api.model.Hunk
import org.wickedsource.diffparser.api.model.Line
import org.wickedsource.diffparser.api.model.Range

fun Line.toJson() = JsonObject("type" to "$lineType", "contents" to content)
fun Range.toJson() = JsonObject("start" to lineStart, "count" to lineCount)
fun Hunk.toJson() = JsonObject(
        "from" to fromFileRange.toJson(),
        "to" to toFileRange.toJson(),
        "lines" to lines.map { it.toJson() }
)
fun Diff.toJson() = JsonObject(
        "fromFile" to fromFileName.split('\t', limit = 2).let {
            JsonObject("name" to it[0], "str" to "$it")
        },
        "toFile" to toFileName.split('\t', limit = 2).let {
            JsonObject("name" to it[0], "str" to "$it")
        },
        "changes" to hunks.map { it.toJson() }
)