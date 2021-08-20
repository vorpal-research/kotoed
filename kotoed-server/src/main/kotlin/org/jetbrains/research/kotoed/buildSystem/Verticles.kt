package org.jetbrains.research.kotoed.buildSystem

import io.vertx.core.file.FileSystemException
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import org.jetbrains.research.kotoed.code.vcs.CommandLine
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.buildSystem.*
import org.jetbrains.research.kotoed.data.notification.NotificationType
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.BuildRecord
import org.jetbrains.research.kotoed.database.tables.records.NotificationRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionResultRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.database.toJson
import ru.spbstu.ktuples.Tuple
import ru.spbstu.ktuples.joinToString
import java.io.ByteArrayInputStream
import java.io.File
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

@AutoDeployable
class BuildVerticle : AbstractKotoedVerticle() {
    override suspend fun start() {
        val fs = vertx.fileSystem()
        if (dir.exists()) fs.deleteRecursiveAsync(dir.absolutePath)
        dir.mkdir()
        super.start()
    }

    private val dir by lazy {
        File(System.getProperty("user.dir"), Config.BuildSystem.StoragePath)
    }
    private val ee by lazy {
        betterFixedThreadPoolContext(Config.BuildSystem.PoolSize, "buildVerticle.dispatcher")
    }

    var currentBuildId = 0

    val buildStatusTable: MutableMap<Int, BuildStatus> = mutableMapOf()
    val submissionBuilds: MutableMap<Int, Int> = mutableMapOf()

    @JsonableEventBusConsumerFor(Address.BuildSystem.Build.Submission.Request)
    suspend fun handleBuildSubmission(submission: SubmissionRecord): BuildAck {
        val buildId = currentBuildId++
        spawn {
            if (submission.id in submissionBuilds) {
                log.error("Build for submission ${submission.id} already running")
                // XXX: do something about it?
            } else try {
                submissionBuilds[submission.id] = buildId
                val explodedSubmission = dbQueryAsync(Tables.SUBMISSION) {
                    find(submission)
                    join(Tables.PROJECT) {
                        join(Tables.COURSE) {
                            join(Tables.BUILD_TEMPLATE)
                        }
                    }
                }.single()

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

                val defaultEnv = Config.BuildSystem.DefaultEnvironment
                val env = template.getJsonObject("environment")
                        .map.mapValues { (_, v) -> "$v" }

                val res = executeBuild(BuildRequest(submission.id, buildId, pattern, defaultEnv + env))

                res.forEach {
                    publishJsonable(Address.BuildSystem.Build.Result, it)
                }

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
            } finally {
                submissionBuilds.remove(submission.id)
            }
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

    private val schedulingSemaphore = Semaphore(Config.BuildSystem.MaxProcesses)

    suspend fun executeBuild(request: BuildRequest): List<BuildResponse> {
        val fs = vertx.fileSystem()
        val uid = UUID.randomUUID()
        val randomName = File(dir, "$uid")
        randomName.mkdirs()

        val env = request.env.orEmpty()

        try {
            val status = BuildStatus(
                    request,
                    "$uid",
                    request.buildScript.map {
                        BuildCommandStatus(
                                it.commandLine.joinToString(" "),
                                BuildCommandState.WAITING,
                                StringBuilder(),
                                StringBuilder()
                        )
                    }
            )
            log.info("Build request assigned to directory $randomName")

            buildStatusTable += request.buildId to status

            schedulingSemaphore.acquire()

            for ((i, command) in request.buildScript.withIndex()) {
                val cmdStatus = buildStatusTable.getValue(request.buildId).commands[i]
                (when (command.type) {
                    BuildCommandType.SHELL -> {
                        val result =
                                withContext(ee) {
                                    cmdStatus.state = BuildCommandState.RUNNING
                                    val cl = CommandLine(command.commandLine, cmdStatus.cout, cmdStatus.cerr)
                                            .execute(randomName, env = env).complete()
                                    cmdStatus.state = BuildCommandState.FINISHED
                                    cl
                                }
                        if (result.rcode.await() != 0) {
                            log.error("[$randomName] " + result.cout)
                            log.error("[$randomName] " + result.cerr)
                            log.info("[$randomName] " + "Build failed, exit code is ${result.rcode.await()}")

                            return listOf(BuildResponse.BuildFailed(
                                    request.submissionId,
                                    request.buildId,
                                    Tuple(result.cout, result.cerr).joinToString("\n")))
                        }
                        Unit
                    }
                }) // parens are here to enforce when exhaustiveness checking
            }

            val res =
                    try {
                        fs.readFileAsync(File(randomName, "results.json").absolutePath).toJsonObject()
                    } catch (ex: FileSystemException) {
                        JsonObject("data" to emptyList<Any?>())
                    }

            val qodanaFile = File(randomName, "report/qodana.json")
            val inspectionsFile = File(randomName, "report/inspections.xml")

            val inspections = when {
                qodanaFile.exists() -> {
                    fs.readFileAsync(qodanaFile.absolutePath).toJsonObject()
                }
                inspectionsFile.exists() -> {
                    val inspectionsXml = fs.readFileAsync(inspectionsFile.absolutePath)
                    xml2json(ByteArrayInputStream(inspectionsXml.bytes))
                }
                else -> null
            }

            log.info("Build request finished in directory $randomName")

            return if (inspections != null) {
                listOf(
                        BuildResponse.BuildSuccess(
                                request.submissionId,
                                request.buildId,
                                res
                        ),
                        BuildResponse.BuildInspection(
                                request.submissionId,
                                request.buildId,
                                inspections
                        )
                )
            } else {
                listOf(
                        BuildResponse.BuildSuccess(
                                request.submissionId,
                                request.buildId,
                                res
                        )
                )
            }

        } catch (ex: Exception) {
            log.info("Build request failed in directory $randomName: $ex")
            throw ex
        } finally {
            fs.deleteRecursiveAsync(randomName.absolutePath)
            schedulingSemaphore.release()
            vertx.setTimer(300000) {
                buildStatusTable.remove(request.buildId)
            }
        }
    }

    @JsonableEventBusConsumerFor(Address.Api.BuildSystem.Build.Status)
    fun handleStatus(statusRequest: BuildStatusRequest) =
            buildStatusTable[statusRequest.buildId] ?:
                throw NoSuchElementException("No build currently running for id ${statusRequest.buildId}")

    @JsonableEventBusConsumerFor(Address.Api.BuildSystem.Build.Summary)
    fun handleSummary() = buildStatusTable.values.toList().sortedByDescending { it.startTime }
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
        is BuildResponse.BuildInspection -> {
            log.trace("Processing $build")

            val result: SubmissionResultRecord = SubmissionResultRecord().apply {
                submissionId = build.submissionId
                time = OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                type = "inspections.json"
                body = build.results
            }
            dbCreateAsync(result)
        }
    }
}
