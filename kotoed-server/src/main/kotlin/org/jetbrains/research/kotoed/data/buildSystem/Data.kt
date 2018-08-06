package org.jetbrains.research.kotoed.data.buildSystem

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.util.*
import kotlin.reflect.KClass

enum class BuildCommandType: Jsonable { SHELL }

data class BuildCommand(
        val type: BuildCommandType = BuildCommandType.SHELL,
        val commandLine: List<String>
) : Jsonable

data class BuildRequest(val submissionId: Int,
                        val buildId: Int,
                        val buildScript: List<BuildCommand>,
                        val env: Map<String, String>?) : Jsonable
sealed class BuildResponse : JsonableSealed {
    data class BuildSuccess(val submissionId: Int,
                            val buildId: Int,
                            val results: JsonObject): BuildResponse()
    data class BuildFailed(val submissionId: Int,
                           val buildId: Int,
                           val log: String): BuildResponse()
}
data class BuildAck(val buildId: Int) : Jsonable
