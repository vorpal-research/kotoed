package org.jetbrains.research.kotoed.data.buildSystem

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.util.*
import kotlin.reflect.KClass

enum class BuildCommandType { SHELL }

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

data class KotoedRunnerFailure(
        val nestedException: String
): Jsonable
// acquired by fair
// ```SQL
// select distinct (jsonb_array_elements(jsonb_array_elements(body->'data')->'results')->'status')
// from submission_result where type = 'results.json';
// ```
enum class KotoedRunnerStatus : Jsonable { ABORTED, SUCCESSFUL, NOT_IMPLEMENTED, FAILED }
data class KotoedRunnerTestResult(
        val status: KotoedRunnerStatus,
        val failure: KotoedRunnerFailure?
): Jsonable
data class KotoedRunnerTestMethodRun(
        val tags: List<String>,
        val results: List<KotoedRunnerTestResult>,
        val methodName: String,
        val packageName: String
): Jsonable
data class KotoedRunnerTestRun(val data: List<KotoedRunnerTestMethodRun>): Jsonable
