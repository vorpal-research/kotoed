package org.jetbrains.research.kotoed.buildSystem

import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.run
import org.jetbrains.research.kotoed.code.vcs.CommandLine
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.buildSystem.BuildCommand
import org.jetbrains.research.kotoed.data.buildSystem.BuildCommandType
import org.jetbrains.research.kotoed.data.buildSystem.BuildRequest
import org.jetbrains.research.kotoed.data.buildSystem.BuildResponse
import org.jetbrains.research.kotoed.data.db.ComplexDatabaseQuery
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import java.io.File
import java.util.*

@AutoDeployable
class BuildVerticle : AbstractKotoedVerticle() {
    override fun start(startFuture: Future<Void>) = spawn {
        val fs = vertx.fileSystem()
        if (dir.exists()) fs.deleteRecursiveAsync(dir.absolutePath)
        dir.mkdir()
        super.start(startFuture)
    }

    private val dir by lazy {
        File(System.getProperty("user.dir"), Config.BuildSystem.StoragePath)
    }
    private val ee by lazy {
        newFixedThreadPoolContext(Config.BuildSystem.PoolSize, "buildVerticle.dispatcher")
    }

    var currentBuildId = 0

    @JsonableEventBusConsumerFor(Address.BuildSystem.Build.Submission.Request)
    fun handleBuildSubmission(submission: SubmissionRecord) = spawn {
        val explodedSubmission = dbQueryAsync(
                ComplexDatabaseQuery(Tables.SUBMISSION)
                        .find(submission)
                        .join(ComplexDatabaseQuery(Tables.PROJECT)
                                .join(ComplexDatabaseQuery(Tables.COURSE).join(Tables.BUILD_TEMPLATE))))
                .first()

        val rev = explodedSubmission.getString("revision")
        val repo = explodedSubmission.getJsonObject("project").getString("repo_url")
        val pattern = explodedSubmission.getJsonObject("project")
                .getJsonObject("course")
                .getJsonObject("build_template")
                .getJsonArray("command_line")
                .map { fromJson<BuildCommand>(it as JsonObject) }
                .map { it.copy(commandLine = it.commandLine.map {
                    when(it) {
                        "\$REPO" -> repo
                        "\$REVISION" -> rev
                        else -> it
                    }
                }) }
        val res = executeBuild(BuildRequest(submission.id, ++currentBuildId, pattern))
        publishJsonable(Address.BuildSystem.Build.Result, res)
    }

    @JsonableEventBusConsumerFor(Address.BuildSystem.Build.Request)
    fun handleBuild(request: BuildRequest) = spawn {
        val res = executeBuild(request)
        publishJsonable(Address.BuildSystem.Build.Result, res)
    }

    suspend fun executeBuild(request: BuildRequest): BuildResponse {
        val fs = vertx.fileSystem()
        val uid = UUID.randomUUID()
        val randomName = File(dir, "$uid")
        randomName.mkdirs()

        try {
            log.info("Build request assigned to directory $randomName")

            for(command in request.buildScript) {
                (when(command.type) {
                    BuildCommandType.SHELL -> {
                        val result =
                                run(ee) { CommandLine(command.commandLine).execute(randomName).complete() }

                        if(result.rcode.get() != 0) {
                            log.error(result.cout.joinToString("\n"))
                            log.error(result.cerr.joinToString("\n"))
                            log.info("Build failed, exit code is ${result.rcode.get()}")

                            return BuildResponse.BuildFailed(
                                    request.submissionId,
                                    request.buildId,
                                    result.cout.joinToString("\n") +
                                    result.cerr.joinToString("\n"))
                        }
                        Unit
                    }
                }) // parens are here to enforce when exhaustiveness checking
            }

            val res = fs.readFileAsync(File(randomName, "results.json").absolutePath).toJsonObject()

            log.info("Build request finished in directory $randomName")

            return BuildResponse.BuildSuccess(
                    request.submissionId,
                    request.buildId,
                    res
            )
        } finally {
            fs.deleteRecursiveAsync(randomName.absolutePath)
        }
    }

}
