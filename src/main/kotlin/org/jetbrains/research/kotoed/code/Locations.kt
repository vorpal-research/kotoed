package org.jetbrains.research.kotoed.code

import org.jetbrains.research.kotoed.util.Jsonable

enum class FileLocType { NORMAL, NON_EXISTENT, UNKNOWN }

data class Filename(val type: FileLocType, val path: String) : Jsonable

data class Location(val filename: Filename, val line: Int, val col: Int) : Jsonable
