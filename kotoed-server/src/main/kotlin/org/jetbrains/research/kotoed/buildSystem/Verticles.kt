package org.jetbrains.research.kotoed.buildSystem

import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.run
import org.jetbrains.research.kotoed.code.vcs.CommandLine
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.buildSystem.*
import org.jetbrains.research.kotoed.data.buildbot.build.LogContent
import org.jetbrains.research.kotoed.data.db.ComplexDatabaseQuery
import org.jetbrains.research.kotoed.data.notification.NotificationType
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.BuildRecord
import org.jetbrains.research.kotoed.database.tables.records.NotificationRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionResultRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toJson
import java.io.File
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
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
    suspend fun handleBuildSubmission(submission: SubmissionRecord): BuildAck {
        val buildId = currentBuildId++
        spawn {
            val explodedSubmission = dbQueryAsync(
                    ComplexDatabaseQuery(Tables.SUBMISSION)
                            .find(submission)
                            .join(ComplexDatabaseQuery(Tables.PROJECT)
                                    .join(ComplexDatabaseQuery(Tables.COURSE).join(Tables.BUILD_TEMPLATE))))
                    .first()

            val rev = explodedSubmission.getString("revision")
            val repo = explodedSubmission.getJsonObject("project").getString("repo_url")
            val denizenId = explodedSubmission.getJsonObject("project").getInteger("denizen_id")
            val template = explodedSubmission.getJsonObject("project")
                    .getJsonObject("course")
                    .getJsonObject("build_template")
            val pattern = template
                    .getJsonArray("command_line")
                    .map { fromJson<BuildCommand>(it as JsonObject) }
                    .map {
                        it.copy(commandLine = it.commandLine.map {
                            when (it) {
                                "\$REPO" -> repo
                                "\$REVISION" -> rev
                                else -> it
                            }
                        })
                    }

            val env = template.getJsonObject("environment")
                    .map.mapValues { (_, v) -> "$v" }

            val res = executeBuild(BuildRequest(submission.id, buildId, pattern, env))
            publishJsonable(Address.BuildSystem.Build.Result, res)

            createNotification(
                    NotificationRecord().apply {
                        this.type = NotificationType.NEW_SUBMISSION_RESULTS.name
                        this.denizenId = denizenId
                        this.body = BuildRecord().apply {
                            // XXX: fetch an actual BuildRecord here?
                            this.buildRequestId = buildId;
                            this.submissionId = submission.id
                        }.toJson()
                    }
            )

        }
        return BuildAck(buildId)
    }

    @JsonableEventBusConsumerFor(Address.BuildSystem.Build.Request)
    fun handleBuild(request: BuildRequest): BuildAck {
        spawn {
            val res = executeBuild(request)
            publishJsonable(Address.BuildSystem.Build.Result, res)

        }
        return BuildAck(request.buildId)
    }

    suspend fun executeBuild(request: BuildRequest): BuildResponse {
        val fs = vertx.fileSystem()
        val uid = UUID.randomUUID()
        val randomName = File(dir, "$uid")
        randomName.mkdirs()

        val env = request.env.orEmpty()

        try {
            log.info("Build request assigned to directory $randomName")

            for (command in request.buildScript) {
                (when (command.type) {
                    BuildCommandType.SHELL -> {
                        val result =
                                run(ee) {
                                    CommandLine(command.commandLine)
                                            .execute(randomName, env = env).complete()
                                }

                        if (result.rcode.get() != 0) {
                            log.error("[$randomName]" + result.cout.joinToString("\n"))
                            log.error("[$randomName]" + result.cerr.joinToString("\n"))
                            log.info("[$randomName]" + "Build failed, exit code is ${result.rcode.get()}")

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
        } catch (ex: Exception) {
            log.info("Build request failed in directory $randomName: $ex")
            throw ex
        } finally {
            fs.deleteRecursiveAsync(randomName.absolutePath)
        }
    }
}

@AutoDeployable
class BuildResultVerticle : AbstractKotoedVerticle() {

    @JsonableEventBusConsumerFor(Address.BuildSystem.Build.Result)
    suspend fun consumeBuildResult(build: BuildResponse) = when (build) {
        is BuildResponse.BuildSuccess -> {
            log.trace("Processing $build")

            val result: SubmissionResultRecord = SubmissionResultRecord().apply {
                submissionId = build.submissionId
                time = OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                type = "results.json"
                body = build.results
            }
            dbCreateAsync(result)
        }
        is BuildResponse.BuildFailed -> {
            log.trace("Processing $build")

            val result: SubmissionResultRecord = SubmissionResultRecord().apply {
                submissionId = build.submissionId
                time = OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                type = "Failed build log"
                body = JsonObject("log" to build.log)
            }
            dbCreateAsync(result)
        }
    }
}
