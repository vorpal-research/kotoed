package org.jetbrains.research.kotoed.integration

import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.UniformInterfaceException
import com.sun.jersey.api.client.config.DefaultClientConfig
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import kotlinx.coroutines.experimental.future.future
import kotlinx.coroutines.experimental.newSingleThreadContext
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.startApplication
import java.util.concurrent.Future
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.UriBuilder

fun startServer(): Future<Vertx> {
    System.setProperty("kotoed.settingsFile", "testenvSettings.json")
    val stc = newSingleThreadContext("kotoed.testing.tc")
    return future(stc) { startApplication() } // FIXME: how to wait for a coroutine in a better way?
}

fun stopServer(vertx: Future<Vertx>) {
    vertx.get().close()
}

fun wdoit(method: HttpMethod,
          path: String,
          mediaType: MediaType = MediaType.APPLICATION_JSON_TYPE,
          params: Iterable<Pair<String, Any?>> = listOf(),
          payload: String? = null,
          port: Int = Config.Root.Port,
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
          port: Int = Config.Root.Port,
          additionalHeaders: Map<String, String> = mapOf()): String =
        wdoit(HttpMethod.POST, path, mediaType, payload = payload, port = port, additionalHeaders = additionalHeaders)

fun wdelete(path: String,
          mediaType: MediaType = MediaType.APPLICATION_JSON_TYPE,
          port: Int = Config.Root.Port,
          additionalHeaders: Map<String, String> = mapOf()): String =
        wdoit(HttpMethod.DELETE, path, mediaType, port = port, additionalHeaders = additionalHeaders)


fun setupTC() {
    val teamcity_request = """
{
  "id": "Test_build_template_id",
  "name": "Test build template",
  "templateFlag": true,
  "projectName": "<Root project>",
  "projectId": "_Root",
  "project": {
    "id": "_Root",
    "name": "<Root project>",
    "description": "Contains all other projects"
  },
  "vcs-root-entries": {
    "count": 0,
    "vcs-root-entry": []
  },
  "settings": {
    "count": 1,
    "property": [
      {
        "name": "artifactRules",
        "value": "+:target/surefire-reports/*.xml => test-results"
      }
    ]
  },
  "parameters": {
    "count": 0,
    "property": []
  },
  "steps": {
    "count": 1,
    "step": [
      {
        "id": "RUNNER_1",
        "name": "",
        "type": "Maven2",
        "properties": {
          "count": 5,
          "property": [
            {
              "name": "goals",
              "value": "clean test"
            },
            {
              "name": "mavenSelection",
              "value": "mavenSelection:default"
            },
            {
              "name": "pomLocation",
              "value": "pom.xml"
            },
            {
              "name": "teamcity.step.mode",
              "value": "default"
            },
            {
              "name": "userSettingsSelection",
              "value": "userSettingsSelection:default"
            }
          ]
        }
      }
    ]
  },
  "features": {
    "count": 0
  },
  "triggers": {
    "count": 1,
    "trigger": [
      {
        "id": "vcsTrigger",
        "type": "vcsTrigger",
        "properties": {
          "count": 2,
          "property": [
            {
              "name": "branchFilter",
              "value": "+:*"
            },
            {
              "name": "quietPeriodMode",
              "value": "DO_NOT_USE"
            }
          ]
        }
      }
    ]
  },
  "snapshot-dependencies": {
    "count": 0
  },
  "artifact-dependencies": {
    "count": 0
  },
  "agent-requirements": {
    "count": 0
  }
}
"""

    wpost(
            path = "/app/rest/buildTypes/", port = Config.TeamCity.Port, payload = teamcity_request,
            additionalHeaders = mapOf(
                    "authorization" to Config.TeamCity.AuthString,
                    "content-type" to "application/json"
            )
    )


}
