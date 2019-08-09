@file:Suppress(kotlinx.warnings.Warnings.NOTHING_TO_INLINE)

package org.jetbrains.research.kotoed.util

class ExpectationFailed(message: String) : Exception(message)

inline fun expect(v: Boolean, message: String = "Expectation failed") =
        v || throw ExpectationFailed(message)

inline fun <T> expectNotNull(v: T?, message: String = "Expectation failed: object is null") =
        v ?: throw ExpectationFailed(message)

inline fun <T> T.expecting(message: String = "Expectation failed", pred: (T) -> Boolean) =
        if (pred(this)) this else throw ExpectationFailed(message)

inline fun <reified U> Any.expectingIs(message: String = "Expectation failed: expected ${U::class}, got ${this::class}"): U =
        if (this is U) this else throw ExpectationFailed(message)
