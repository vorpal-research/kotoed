package org.jetbrains.research.kotoed.integration

import io.vertx.core.Vertx
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.Loggable
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.Future

class TeamCityInteropTestIntegration : Loggable {

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
    fun pingTeamCity() {
        println(wpost("debug/eventbus/${Address.TeamCity.Build.Info}", payload = "{}"))
    }
}
