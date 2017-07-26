@file:Suppress("NOTHING_TO_INLINE")

package org.jetbrains.research.kotoed.util

import com.google.common.cache.Cache

// Adapting guava cache to kotlin Map<*, *> conventions

inline operator fun <K, V> Cache<K, V>.contains(key: K) = this.getIfPresent(key) != null
inline operator fun <K, V> Cache<K, V>.get(key: K): V? = this.getIfPresent(key)
inline operator fun <K, V> Cache<K, V>.set(key: K, value: V) = this.put(key, value)

inline operator fun <A, R> com.google.common.base.Function<A, R>.invoke(arg: A?): R? = apply(arg)
