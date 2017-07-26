package org.jetbrains.research.kotoed.notification

import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.util.AbstractKotoedVerticle

abstract class AbstractNotificationVerticle : AbstractKotoedVerticle() {
    protected companion object {
        val Pool =
                newFixedThreadPoolContext(Config.Notifications.PoolSize, "notifications.dispatcher")
    }
}