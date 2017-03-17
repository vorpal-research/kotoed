package org.jetbrains.research.kotoed.util.database

import org.jooq.Query
import org.jooq.Record
import org.jooq.Result
import org.jooq.ResultQuery
import org.jooq.impl.DSL
import kotlin.coroutines.experimental.suspendCoroutine


suspend fun Query.executeKAsync(): Int =
    suspendCoroutine<Int> { cont ->
        this.executeAsync().whenComplete { v, ex ->
            if(ex == null) cont.resume(v)
            else cont.resumeWithException(ex)
        }
    }

suspend fun<T: Record> ResultQuery<T>.fetchKAsync(): Result<T> =
    suspendCoroutine { cont ->
        this.fetchAsync().whenComplete { v, ex ->
            if(ex == null) cont.resume(v)
            else cont.resumeWithException(ex)
        }
    }

