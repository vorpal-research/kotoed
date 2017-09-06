package org.jetbrains.research.kotoed.util.diffs

import com.sksamuel.diffpatch.DiffMatchPatch

data class Chunk(val operation: DiffMatchPatch.Operation, val contents: List<String>)

class LineDiff {
    var uid = 1
    val cache: MutableMap<Int, String> = mutableMapOf()
    val revCache: MutableMap<String, Int> = mutableMapOf()

    val impl = DiffMatchPatch()

    fun lineToChar(key: String): Char =
            when {
                key in revCache -> revCache[key]!!.toChar()
                else -> uid
                        .also { cache[uid] = key  }
                        .also { revCache[key] = uid }
                        .also { ++uid }
                        .toChar()
            }
    fun linesToString(lines: Sequence<String>): String = lines.map(this::lineToChar).joinToString("")
    fun charToLine(ch: Char) = cache[ch.toInt()]!!
    fun stringToLines(string: String): List<String> = string.map(this::charToLine)

    fun diff(before: Sequence<String>, after: Sequence<String>): List<Chunk> =
            impl.diff_main(
                    linesToString(before).also { println(it) },
                    linesToString(after).also { println(it) }
            ).map { Chunk(it.operation, stringToLines(it.text)) }

}