package org.jetbrains.research.kotoed.code

import org.jetbrains.research.kotoed.util.Jsonable
import org.wickedsource.diffparser.api.model.Diff

enum class FileLocType { NORMAL, NON_EXISTENT, UNKNOWN }

data class Filename(val type: FileLocType = FileLocType.NORMAL, val path: String) : Jsonable

data class Location(val filename: Filename, val line: Int, val col: Int = 0) : Jsonable {
    companion object {
        val Unknown = Location(Filename(FileLocType.UNKNOWN, ""), 0, 0)
    }
}

fun Location.applyDiffs(diffs: Sequence<Diff>): Location {
    val relevant = diffs.find { it.fromFileName == filename.path }
    relevant ?: return this

    var adj: Int = 0
    var curLine: Int = 0
    for(hunk in relevant.hunks) {
        if(curLine > line) {
            return Location(Filename(FileLocType.NORMAL, relevant.toFileName), line + adj, col)
        }

        curLine = hunk.fromFileRange.lineStart + hunk.fromFileRange.lineCount
        adj += (hunk.toFileRange.lineCount - hunk.fromFileRange.lineCount)
    }

    return Location(Filename(FileLocType.NORMAL, relevant.toFileName), line + adj, col)

}
