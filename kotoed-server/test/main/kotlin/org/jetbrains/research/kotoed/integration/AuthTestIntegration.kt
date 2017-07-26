package org.jetbrains.research.kotoed.integration

import com.sun.jersey.api.client.UniformInterfaceException
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.AnyAsJson
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.Future
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class OAuthTestIntegration {
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
    fun testStandardAuth() {

        val login = "TestSubject0012-A"

        val signupResp =
                wpost(
                        path = "debug/eventbus/${Address.User.Auth.SignUp}",
                        payload = """
                              {
                                  "denizen_id" : "$login",
                                  "password" : "krenogubka"
                              }
                            """
                ).let(::JsonObject)

        with(AnyAsJson) {
            assertEquals(login, signupResp["denizen_id"])
            assertNull(signupResp["password"])
            assertNull(signupResp["salt"])
        }

        val loginResp =
                wpost(
                        path = "debug/eventbus/${Address.User.Auth.Login}",
                        payload = """
                              {
                                  "denizen_id" : "$login",
                                  "password" : "krenogubka"
                              }
                            """
                ).let(::JsonObject)

        with(AnyAsJson) {
            assertEquals(login, loginResp["denizen_id"])
            assertNull(loginResp["password"])
            assertNull(loginResp["salt"])
        }

        assertFailsWith<UniformInterfaceException> {
            wpost(
                    path = "debug/eventbus/${Address.User.Auth.Login}",
                    payload = """
                              {
                                  "denizen_id" : "$login",
                                  "password" : "qwerty"
                              }
                            """
            ).let(::JsonObject)
        }.also {
            assertEquals(403, it.response.clientResponseStatus.statusCode)
        }


    }

    @Test
    fun testOAuth() {

        val newProvider = wpost(
                path = "debug/eventbus/${Address.DB.create("oauth_provider")}",
                payload = """ { "name" : "http://spbstu.ru" } """
        ).let(::JsonObject)

        val login = "TestSubject0012-B"

        val signupResp =
                wpost(
                        path = "debug/eventbus/${Address.User.Auth.SignUp}",
                        payload = """
                              {
                                  "denizen_id" : "$login",
                                  "password" : "krenogubka"
                              }
                            """
                ).let(::JsonObject)

        with(AnyAsJson) {
            assertEquals(login, signupResp["denizen_id"])
            assertNull(signupResp["password"])
            assertNull(signupResp["salt"])
        }

        val oauthSingUpResp =
                wpost(
                        path = "debug/eventbus/${Address.User.OAuth.SignUp}",
                        payload = """
                              {
                                  "denizen_id" : "$login",
                                  "oauth_provider" : "http://spbstu.ru",
                                  "oauth_user" : "XXXnagibator98XXX"
                              }
                            """
                ).let(::JsonObject)

        val oauthLoginResp =
                wpost(
                        path = "debug/eventbus/${Address.User.OAuth.Login}",
                        payload = """
                              {
                                  "oauth_provider" : "http://spbstu.ru",
                                  "oauth_user" : "XXXnagibator98XXX"
                              }
                            """
                ).let(::JsonObject)

        with(AnyAsJson) {
            assertEquals(login, oauthLoginResp["denizen_id"])
        }

        assertFailsWith<UniformInterfaceException> {
            wpost(
                    path = "debug/eventbus/${Address.User.OAuth.SignUp}",
                    payload = """
                              {
                                  "denizen_id" : "Some other guy",
                                  "oauth_provider" : "http://spbstu.ru",
                                  "oauth_user" : "XXXnagibator98XXX"
                              }
                            """
            ).let(::JsonObject)
        }

        val oauthLoginRespTry2 =
                wpost(
                        path = "debug/eventbus/${Address.User.OAuth.Login}",
                        payload = """
                              {
                                  "oauth_provider" : "http://spbstu.ru",
                                  "oauth_user" : "XXXnagibator98XXX"
                              }
                            """
                ).let(::JsonObject)

        with(AnyAsJson) {
            assertEquals(login, oauthLoginRespTry2["denizen_id"])
        }

    }

}