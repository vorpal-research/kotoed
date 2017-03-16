package org.jetbrains.research.kotoed.data

import org.jetbrains.research.kotoed.util.Jsonable

interface EventBusDatum<T> : Jsonable {
    val address: String
}
