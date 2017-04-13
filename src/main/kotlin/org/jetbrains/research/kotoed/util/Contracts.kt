@file:Suppress("NOTHING_TO_INLINE")

package org.jetbrains.research.kotoed.util

class ExpectationFailed(message: String) : Exception(message)

inline fun expect(v: Boolean, message: String = "Expectation failed") = v || throw ExpectationFailed(message)
inline fun<T> expectNotNull(v: T?, message: String = "Expectation failed") = v ?: throw ExpectationFailed(message)


