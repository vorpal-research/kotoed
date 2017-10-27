package org.jooq.impl

import org.jooq.Configuration
import org.jooq.exception.DataAccessException

suspend fun <T> transactionResult0_(transactional: suspend (Configuration) -> T, configuration: Configuration): T {

    val result: T?

    val ctx = DefaultTransactionContext(configuration.derive())
    val provider = ctx.configuration().transactionProvider()
    val listeners = TransactionListeners(ctx.configuration())

    try {
        try {
            listeners.beginStart(ctx)
            provider.begin(ctx)
        } finally {
            listeners.beginEnd(ctx)
        }

        result = transactional.invoke(ctx.configuration())

        try {
            listeners.commitStart(ctx)
            provider.commit(ctx)
        } finally {
            listeners.commitEnd(ctx)
        }
    } catch (error: Error) {
        // [#6608] Propagating errors directly
        throw error
    } catch (cause: Throwable) {
        if (cause is Exception) {
            ctx.cause(cause)
        } else {
            ctx.causeThrowable(cause)
        }

        listeners.rollbackStart(ctx)
        try {
            provider.rollback(ctx)
        } catch (suppress: Exception) {
            // [#3718] Use reflection to support also JDBC 4.0
            cause.addSuppressed(suppress)
        }
        listeners.rollbackEnd(ctx)

        if (cause is RuntimeException) {
            throw cause
        } else {
            throw DataAccessException("Rollback caused", cause)
        }
    }

    return result
}
