package org.jetbrains.research.kotoed.integration

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.UniformInterfaceException
import com.sun.jersey.api.client.config.DefaultClientConfig
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.future
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.startApplication
import java.util.concurrent.Executors
import java.util.concurrent.Future
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.UriBuilder

fun startServer(): Future<Vertx> {
    System.setProperty("kotoed.settingsFile", "testenvSettings.json")
    val stc = Executors.newSingleThreadExecutor(
            ThreadFactoryBuilder().setNameFormat("kotoed.testing.tc").build()
    ).asCoroutineDispatcher()
    return CoroutineScope(stc).future { startApplication() } // FIXME: how to wait for a coroutine in a better way?
}

fun stopServer(vertx: Future<Vertx>) {
    vertx.get().close()
}

fun wdoit(method: HttpMethod,
          path: String,
          mediaType: MediaType = MediaType.APPLICATION_JSON_TYPE,
          params: Iterable<Pair<String, Any?>> = listOf(),
          payload: String? = null,
          port: Int = Config.Root.ListenPort,
          additionalHeaders: Map<String, String> = mapOf()): String {
    val config = DefaultClientConfig()
    val client = Client.create(config)
    val resource = client.resource(UriBuilder.fromUri("http://localhost:$port").build())

    try {
        return resource.path(path).let {
            params.fold(it) { res, (k, v) -> res.queryParam(k, "$v") }
        }.accept(mediaType).apply { additionalHeaders.forEach { (k, v) -> header(k, v) } }.method(method.name, String::class.java, payload)
    } catch (ex: UniformInterfaceException) {
        println(ex.response)
        println(ex.response.getEntity(String::class.java))
        throw ex
    }
}

fun wget(path: String,
         mediaType: MediaType = MediaType.APPLICATION_JSON_TYPE,
         params: Iterable<Pair<String, Any?>> = listOf()): String =
        wdoit(HttpMethod.GET, path, mediaType, params)

fun wpost(path: String,
          mediaType: MediaType = MediaType.APPLICATION_JSON_TYPE,
          payload: String = "",
          port: Int = Config.Root.ListenPort,
          additionalHeaders: Map<String, String> = mapOf()): String =
        wdoit(HttpMethod.POST, path, mediaType, payload = payload, port = port, additionalHeaders = additionalHeaders)

fun wdelete(path: String,
            mediaType: MediaType = MediaType.APPLICATION_JSON_TYPE,
            port: Int = Config.Root.ListenPort,
            additionalHeaders: Map<String, String> = mapOf()): String =
        wdoit(HttpMethod.DELETE, path, mediaType, port = port, additionalHeaders = additionalHeaders)
