package org.jetbrains.research.kotoed.code

sealed class Filename
data class NormalFile(val path: String) : Filename()
object NonExistent : Filename()
object Unknown : Filename()

data class Location(val filename: Filename, val line: Int, val col: Int)
