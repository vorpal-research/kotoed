package org.jetbrains.research.kotoed.data.statistics

import org.jetbrains.research.kotoed.util.Jsonable

data class ReportResponse(val data: List<List<String>>): Jsonable
