package org.jetbrains.research.kotoed.code.klones

import com.google.common.collect.Queues
import com.intellij.psi.PsiElement
import com.suhininalex.suffixtree.SuffixTree
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.run
import org.jetbrains.kootstrap.FooBarCompiler
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.research.kotoed.data.api.Code
import org.jetbrains.research.kotoed.data.db.ComplexDatabaseQuery
import org.jetbrains.research.kotoed.data.vcs.CloneStatus
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.database.tables.records.CourseRecord
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionResultRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import ru.spbstu.ktuples.placeholders._0
import ru.spbstu.ktuples.placeholders.bind

sealed class KloneRequest(val priority: Int) : Jsonable, Comparable<KloneRequest> {
    override fun compareTo(other: KloneRequest): Int = priority - other.priority
}

data class ProcessCourseBaseRepo(val course: CourseRecord) : KloneRequest(1)
data class ProcessSubmission(val submissionData: JsonObject) : KloneRequest(2)
data class BuildSubmissionReport(val submissionData: JsonObject) : KloneRequest(3)
data class BuildCourseReport(val course: CourseRecord) : KloneRequest(4)

@AutoDeployable
class KloneVerticle : AbstractKotoedVerticle(), Loggable {

    private val RESULT_TYPE = "klonecheck"

    enum class Mode {
        COURSE,
        SUBMISSION
    }

    private val ee by lazy { newSingleThreadContext("kloneVerticle.executor") }

    private val suffixTree = SuffixTree<Token>()
    private val processed = mutableSetOf<Pair<Mode, Int>>()

    private val kloneRequests = Queues.newPriorityBlockingQueue<KloneRequest>()

    suspend fun courseSubmissionData(course: CourseRecord): List<JsonObject> {

        val projQ = ComplexDatabaseQuery("project")
                .find(ProjectRecord().apply {
                    courseId = course.id
                    deleted = false
                })
                .join("denizen", field = "denizen_id")

        val q = ComplexDatabaseQuery("submission")
                .find(SubmissionRecord().apply {
                    state = SubmissionState.open
                })
                .join(projQ, field = "project_id")

        return sendJsonableCollectAsync(Address.DB.query("submission"), q)
    }

    @JsonableEventBusConsumerFor(Address.Code.KloneCheck)
    suspend fun handleCheck(course: CourseRecord) {

        val data = courseSubmissionData(course)

        with(kloneRequests) {
            offer(ProcessCourseBaseRepo(course))
            for (sub in data) offer(ProcessSubmission(sub))
            offer(BuildCourseReport(course))
        }
    }

    override fun start(startFuture: Future<Void>) {
        vertx.setTimer(5000, this::handleRequest)
        super.start(startFuture)
    }

    fun handleRequest(timerId: Long) {

        fun handleException(ex: Throwable) {
            log.error("Klones failed!", ex)
            vertx.setTimer(5000, this::handleRequest)
        }

        val req = kloneRequests.poll()
        when (req) {
            is ProcessCourseBaseRepo -> {
                spawn(VertxContext(vertx) +
                        WithExceptions(::handleException)) {
                    if (!handleBase(req.course)) kloneRequests.offer(req)
                    vertx.setTimer(100, this::handleRequest)
                }
            }
            is ProcessSubmission -> {
                spawn(VertxContext(vertx) +
                        WithExceptions(::handleException)) {
                    if (!handleSub(req.submissionData)) kloneRequests.offer(req)
                    vertx.setTimer(100, this::handleRequest)
                }
            }
            is BuildCourseReport -> {
                spawn(VertxContext(vertx) +
                        WithExceptions(::handleException)) {
                    val data = courseSubmissionData(req.course)
                    if (!handleReport(data)) kloneRequests.offer(req)
                    vertx.setTimer(100, this::handleRequest)
                }
            }
            else -> {
                vertx.setTimer(5000, this::handleRequest)
            }
        }
    }

    suspend fun handleBase(crs: CourseRecord): Boolean {

        if (Mode.COURSE to crs.id in processed) return true

        val course = when (crs.state) {
            null -> dbFetchAsync(crs)
            else -> crs
        }

        val files: Code.ListResponse = sendJsonableAsync(
                Address.Api.Course.Code.List,
                Code.Course.ListRequest(course.id)
        )

        return handleFiles(
                Mode.COURSE,
                course.id,
                -1, // Course tokens do not have an owning denizen
                files)
    }

    suspend fun handleSub(sub: JsonObject): Boolean {

        if (Mode.SUBMISSION to sub.getInteger("id") in processed) return true

        val files: Code.ListResponse = sendJsonableAsync(
                Address.Api.Submission.Code.List,
                Code.Submission.ListRequest(sub.getInteger("id"))
        )

        return handleFiles(
                Mode.SUBMISSION,
                sub.getInteger("id"),
                sub.safeNav("project", "denizen", "id") as Int,
                files)
    }

    suspend fun handleFiles(
            mode: Mode,
            id: Int,
            denizenId: Int,
            files: Code.ListResponse): Boolean {

        if (mode to id in processed) return true

        if (files.status != CloneStatus.done) {
            log.trace("Repository not cloned yet")
            return false
        }

        val compilerEnv = run(ee) { FooBarCompiler.setupMyEnv(CompilerConfiguration()) }

        val allFiles = files.root?.toFileSeq().orEmpty()

        val ktFiles = allFiles
                .filter { it.endsWith(".kt") }
                .toList()
                .map { filename ->
                    log.trace("filename = $filename")
                    val resp: Code.Submission.ReadResponse = when (mode) {
                        Mode.COURSE ->
                            sendJsonableAsync(Address.Api.Course.Code.Read,
                                    Code.Course.ReadRequest(
                                            courseId = id, path = filename))
                        Mode.SUBMISSION ->
                            sendJsonableAsync(Address.Api.Submission.Code.Read,
                                    Code.Submission.ReadRequest(
                                            submissionId = id, path = filename))
                    }
                    run(ee) { KtPsiFactory(compilerEnv.project).createFile(filename, resp.contents) }
                }

        ktFiles.asSequence()
                .flatMap { file ->
                    file.collectDescendantsOfType<KtNamedFunction>().asSequence()
                }
                .filter { method ->
                    method.annotationEntries.all { anno -> "@Test" != anno.text }
                }
                .map {
                    it as PsiElement
                }
                .map { method ->
                    method to method.dfs { children.asSequence() }
                            .filter(Token.DefaultFilter)
                            .map((::makeAnonimizedToken)
                                    .bind(_0, mode)
                                    .bind(_0, id)
                                    .bind(_0, denizenId))
                }
                .forEach { (_, tokens) ->
                    val lst = tokens.toList()
                    log.trace("lst = $lst")
                    val seqId = suffixTree.addSequence(lst)
                }

        run(ee) { FooBarCompiler.tearDownMyEnv(compilerEnv) }

        processed.add(mode to id)

        return true
    }

    suspend fun handleReport(data: List<JsonObject>): Boolean {

        val dataBySubmissionId = data.map { it.getInteger("id") to it }.toMap()

        val clones =
                suffixTree.root.dfs {
                    edges
                            .asSequence()
                            .mapNotNull { it.terminal }
                }.filter { node ->
                    node.edges
                            .asSequence()
                            .all { it.begin == it.end && it.begin == it.sequence.size - 1 }
                }.filter { node ->
                    0 == node.parentEdges.lastOrNull()?.begin
                }

        log.trace(clones.joinToString("\n"))

        val filtered = clones
                .map(::CloneClass)
                .filter { cc -> cc.clones.isNotEmpty() }
                .filter { cc -> Mode.COURSE !in cc.clones.map { it.type } }
                .filter { cc -> cc.clones.map { it.submissionId }.toSet().size != 1 }
                .toList()

        filtered.forEachIndexed { i, cloneClass ->
            val builder = StringBuilder()
            val fname = cloneClass.clones
                    .map { clone -> clone.functionName }
                    .distinct()
                    .joinToString()
            builder.appendln("($fname) Clone class $i:")
            cloneClass.clones.forEach { c ->
                builder.appendln("${c.submissionId}/${c.functionName}/${c.file.path}:${c.fromLine}:${c.toLine}")
            }
            builder.appendln()
            log.trace(builder)
        }

        val clonesBySubmission = filtered
                .flatMap { cloneClass ->
                    cloneClass.clones.map { clone -> clone.submissionId to clone.denizenId to cloneClass }
                }
                .groupBy { it.first }
                .mapValues { (key, value) ->
                    value.map { e ->
                        e.second.clones.filter { clone ->
                            key == clone.submissionId to clone.denizenId ||
                                    key.second != clone.denizenId
                        }
                    }.filterNot { it.isEmpty() }
                }
                .filterNot { it.value.isEmpty() }

        clonesBySubmission.asSequence()
                .forEach { (cloneId, cloneClasses) ->
                    dbCreateAsync(SubmissionResultRecord().apply {
                        submissionId = cloneId.first
                        type = RESULT_TYPE
                        // FIXME: akhin Make this stuff Jsonable or what?
                        body = cloneClasses.map { cloneClass ->
                            cloneClass.map { clone ->
                                object : Jsonable {
                                    val submissionId = clone.submissionId
                                    val denizen = dataBySubmissionId[clone.submissionId].safeNav("project", "denizen", "denizen_id")
                                    val project = dataBySubmissionId[clone.submissionId].safeNav("project", "name")
                                    val file = clone.file
                                    val fromLine = clone.fromLine
                                    val toLine = clone.toLine
                                    val functionName = clone.functionName
                                }
                            }
                        }
                    })
                }

        return true

    }
}
