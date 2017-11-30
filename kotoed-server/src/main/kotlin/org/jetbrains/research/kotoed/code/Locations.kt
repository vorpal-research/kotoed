package org.jetbrains.research.kotoed.code

import org.jetbrains.research.kotoed.util.GlobalLogging.log
import org.jetbrains.research.kotoed.util.Jsonable
import org.jetbrains.research.kotoed.util.chunksBy
import org.wickedsource.diffparser.api.model.Diff
import org.wickedsource.diffparser.api.model.Hunk
import org.wickedsource.diffparser.api.model.Line
import org.wickedsource.diffparser.api.model.Range
import ru.spbstu.ktuples.Tuple
import ru.spbstu.ktuples.compareTo

enum class FileLocType { NORMAL, NON_EXISTENT, UNKNOWN }

data class Filename(val type: FileLocType = FileLocType.NORMAL, val path: String) : Jsonable, Comparable<Filename> {
    override fun compareTo(other: Filename) = Tuple(type, path).compareTo(Tuple(other.type, other.path))

}

data class Location(val filename: Filename, val line: Int, val col: Int = 0) : Jsonable, Comparable<Location> {
    override fun compareTo(other: Location) =
            Tuple(filename, line, col).compareTo(Tuple(other.filename, other.line, other.col))

    // locations are primarily line-based
    operator fun minus(other: Location) = when {
        filename != other.filename -> Int.MAX_VALUE
        else -> line - other.line
    }
    operator fun plus(offset: Int) = copy(line = line + offset)

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

private val Range.lineEnd get() = lineStart + lineCount
private val Hunk.lineShift get() = toFileRange.lineEnd - fromFileRange.lineEnd

fun Location.applyDiffs(diffs: List<Diff>): Location {
    val diff = diffs.find { it.fromFileName == filename.path }
    diff ?: return this
    val hunk = diff.hunks.find {
        val fromLoc = it.fromFileRange
        line in (fromLoc.lineStart .. (fromLoc.lineStart + fromLoc.lineCount))
    }

    if(hunk == null) {
        val preHunk = diff.hunks.firstOrNull {
            val fromLoc = it.fromFileRange
            line > fromLoc.lineStart
        }

        preHunk ?: return this

        return Location(diff.toFileName.asFilename(), line + preHunk.lineShift, col)
    }

    var curFrom = hunk.fromFileRange.lineStart
    var curTo = hunk.toFileRange.lineStart

    var lastNeutralFrom = curFrom
    var lastNeutralTo = curTo
    var nextNeutralTo = curTo

    val hunkIter = hunk.lines.iterator()

    for(line in hunkIter) {
        when(line.lineType) {
            null -> throw IllegalArgumentException("Hunk line should not be null")
            Line.LineType.NEUTRAL -> {
                lastNeutralFrom = curFrom
                lastNeutralTo = curTo
                ++curFrom; ++curTo
            }
            Line.LineType.TO -> { ++curTo }
            Line.LineType.FROM -> { ++curFrom }
        }

        if(curFrom == this.line) {
            break
        }
    }
    val resultTo = (curFrom - lastNeutralFrom) + lastNeutralTo

    // second for on an iterator visits only elements not visited first time, which is pretty nifty
    loop@ for(line in hunkIter) {
        when(line.lineType) {
            null -> throw IllegalArgumentException("Hunk line should not be null")
            Line.LineType.NEUTRAL -> break@loop
            Line.LineType.TO -> { ++curTo }
            Line.LineType.FROM -> {}
        }
    }

    return Location(diff.toFileName.asFilename(), minOf(resultTo, curTo), col)

}
