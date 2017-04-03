package org.jetbrains.research.kotoed.integration

import io.vertx.core.Vertx
import kotlinx.coroutines.experimental.future.future
import kotlinx.coroutines.experimental.newSingleThreadContext
import org.jetbrains.research.kotoed.startApplication
import java.util.concurrent.Future

fun startServer(): Future<Vertx> {
    System.setProperty("kotoed.settingsFile", "testenvSettings.json")
    val stc = newSingleThreadContext("kotoed.testing.tc")
    return future(stc) { startApplication(arrayOf()) } // FIXME: how to wait for a coroutine in a better way?
}

fun stopServer(vertx: Future<Vertx>) {
    vertx.get().close()
}
