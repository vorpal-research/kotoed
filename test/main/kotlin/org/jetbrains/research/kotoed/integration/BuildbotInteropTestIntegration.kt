package org.jetbrains.research.kotoed.integration

import io.vertx.core.Vertx
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.Loggable
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.Future

class BuildbotInteropTestIntegration : Loggable {

    companion object {
        lateinit var server: Future<Vertx>
        @JvmStatic
        @BeforeClass
        fun before() {
            server = startServer()
            server.get()
        }

        @JvmStatic
        @AfterClass
        fun after() {
            stopServer(server)
        }
    }

    @Test
    fun pingBuildbot() {
        wpost("debug/eventbus/${Address.Buildbot.Build.RequestInfo}",
                payload = """{ "build_request_id": -1 }""")
    }
}
