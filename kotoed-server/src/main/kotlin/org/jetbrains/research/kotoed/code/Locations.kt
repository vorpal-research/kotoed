package org.jetbrains.research.kotoed.code

import org.jetbrains.research.kotoed.util.Jsonable
import org.wickedsource.diffparser.api.model.Diff
import org.wickedsource.diffparser.api.model.Line

enum class FileLocType { NORMAL, NON_EXISTENT, UNKNOWN }

data class Filename(val type: FileLocType = FileLocType.NORMAL, val path: String) : Jsonable

data class Location(val filename: Filename, val line: Int, val col: Int = 0) : Jsonable {
    companion object {
        val Unknown = Location(Filename(FileLocType.UNKNOWN, ""), 0, 0)
    }
}

private const val NonExistentFile = "/dev/null"
private fun String.asFilename() =
        when(this) {
            NonExistentFile -> Filename(FileLocType.NON_EXISTENT, NonExistentFile)
            else -> Filename(FileLocType.NORMAL, this)
        }

fun Location.applyDiffs(diffs: List<Diff>): Location {
    val diff = diffs.find { it.fromFileName == filename.path }
    diff ?: return this
    val hunk = diff.hunks.find {
        val fromLoc = it.fromFileRange
        line in (fromLoc.lineStart .. (fromLoc.lineStart + fromLoc.lineCount))
    }
    hunk ?: return this

    var curFrom = hunk.fromFileRange.lineStart
    var curTo = hunk.toFileRange.lineStart

    for(line in hunk.lines) {
        when(line.lineType) {
            null -> throw IllegalArgumentException("Hunk line should not be null")
            Line.LineType.NEUTRAL -> { ++curFrom; ++curTo }
            Line.LineType.TO -> { ++curTo }
            Line.LineType.FROM -> { ++curFrom }
        }

        if(curFrom == this.line) break
    }

    return Location(diff.toFileName.asFilename(), curTo, col)

}
