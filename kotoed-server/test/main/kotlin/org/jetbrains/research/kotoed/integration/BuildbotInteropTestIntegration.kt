package org.jetbrains.research.kotoed.integration

import com.sun.jersey.api.client.UniformInterfaceException
import io.vertx.core.Vertx
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.Loggable
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.Future
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
        assertEquals(
                404,
                assertFailsWith(UniformInterfaceException::class) {
                    wpost("debug/eventbus/${Address.Buildbot.Build.RequestInfo}",
                            payload = """{ "build_request_id": -1 }""")
                }.response.status
        )
    }
}
