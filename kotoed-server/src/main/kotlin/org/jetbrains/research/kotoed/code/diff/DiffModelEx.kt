package org.jetbrains.research.kotoed.code.diff

import org.jetbrains.kotlin.utils.sure
import org.jetbrains.research.kotoed.util.*
import org.wickedsource.diffparser.api.model.Diff
import org.wickedsource.diffparser.api.model.Hunk
import org.wickedsource.diffparser.api.model.Line
import org.wickedsource.diffparser.api.model.Range

fun Line.toJson() = JsonObject("vcs" to "$lineType", "contents" to content)

fun Range.toJson() = JsonObject("start" to lineStart, "count" to lineCount)

fun Hunk.toJson() = JsonObject(
        "from" to fromFileRange?.toJson(),
        "to" to toFileRange?.toJson(),
        "lines" to lines.map { it?.toJson() }
)

fun Diff.toJson() = JsonObject(
        "fromFile" to fromFileName.split('\t', limit = 2).let {
            JsonObject("name" to it[0], "str" to "$it")
        },
        "toFile" to toFileName.split('\t', limit = 2).let {
            JsonObject("name" to it[0], "str" to "$it")
        },
        "changes" to hunks.map { it?.toJson() }
)

// a very sloppy git diff format parser implementation, do not repeat this at home
fun parseGitDiff(lines: Sequence<String>): List<Diff> {
    val diffs = mutableListOf<Diff>()
    fun newDiff() = Diff().also { diffs.add(it) }
    fun currentDiff(): Diff = diffs.lastOrNull() ?: newDiff()

    fun newHunk() = Hunk().also { currentDiff().hunks.add(it)  }
    fun currentHunk() = currentDiff().hunks.lastOrNull() ?: newHunk()

    val hunkHeader = Regex("""@@\s*-(\d+\s*)(,\s*\d+)?\s*\+(\d+\s*)(,\s*\d+)?\s*@@.*""")

    for(line in lines) when {
        line.startsWith("diff") -> newDiff()
        // XXX: in unified diff, these filenames may(sic!) be followed by whatever-formatted timestamps
        // making it one hell of a parsing experience if filepaths may contains spaces
        line.startsWith("---") -> {
            currentDiff().fromFileName = line.removePrefix("---").trim().removePrefix("a/")
        }
        line.startsWith("+++") -> {
            currentDiff().toFileName = line.removePrefix("+++").trim().removePrefix("b/")
        }

        line.startsWith("@@") -> {
            newHunk()

            val parsed = hunkHeader.matchEntire(line)
            parsed ?: throw IllegalStateException("Failed to parse diff: ${lines.joinToString("\n")}")
            val (_, fromLine, fromCount, toLine, toCount) = parsed.groups

            fromLine ?: throw IllegalStateException("Failed to parse diff: ${lines.joinToString("\n")}")
            toLine ?: throw IllegalStateException("Failed to parse diff: ${lines.joinToString("\n")}")

            currentHunk().fromFileRange = Range(
                    fromLine.value.trim().toInt(),
                    fromCount?.value?.drop(1)?.trim()?.toInt() ?: 1
            )

            currentHunk().toFileRange = Range(
                    toLine.value.trim().toInt(),
                    toCount?.value?.drop(1)?.trim()?.toInt() ?: 1
            )
        }

        line.startsWith("-") -> {
            currentHunk().lines.add(Line(Line.LineType.FROM, line.drop(1)))
        }

        line.startsWith("+") -> {
            currentHunk().lines.add(Line(Line.LineType.TO, line.drop(1)))
        }

        line.startsWith(" ") -> {
            currentHunk().lines.add(Line(Line.LineType.NEUTRAL, line.drop(1)))
        }

    }

    return diffs

}
