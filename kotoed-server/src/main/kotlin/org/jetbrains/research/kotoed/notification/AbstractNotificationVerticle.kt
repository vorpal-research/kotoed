package org.jetbrains.research.kotoed.notification

import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.util.AbstractKotoedVerticle
import org.jetbrains.research.kotoed.util.betterFixedThreadPoolContext

abstract class AbstractNotificationVerticle : AbstractKotoedVerticle() {
    protected companion object {
        val Pool =
                betterFixedThreadPoolContext(Config.Notifications.PoolSize, "notifications.dispatcher")
    }
}
