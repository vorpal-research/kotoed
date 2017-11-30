package org.jetbrains.research.kotoed.code.klones

import com.intellij.psi.PsiElement
import com.suhininalex.suffixtree.SuffixTree
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

@AutoDeployable
class KloneVerticle : AbstractKotoedVerticle(), Loggable {

    private val RESULT_TYPE = "klonecheck"

    private val BASE_SUBMISSION_ID = -1

    enum class Mode {
        COURSE,
        SUBMISSION
    }

    private val ee by lazy { newSingleThreadContext("kloneVerticle.executor") }

    @JsonableEventBusConsumerFor(Address.Code.KloneCheck)
    suspend fun handleCheck(course: CourseRecord) {

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

        val data: List<JsonObject> = sendJsonableCollectAsync(Address.DB.query("submission"), q)

        val tree = SuffixTree<Token>()
        val ids = mutableMapOf<Long, PsiElement>()

        if (!handleBase(tree, ids, course)) return // we cannot do shirt without the base repo

        for (sub in data) handleSub(tree, ids, sub)

        handleReport(tree, ids, data)
    }

    suspend fun handleBase(
            tree: SuffixTree<Token>,
            ids: MutableMap<Long, PsiElement>,
            crs: CourseRecord): Boolean {

        val course = when (crs.state) {
            null -> dbFetchAsync(crs)
            else -> crs
        }

        val files: Code.ListResponse = sendJsonableAsync(
                Address.Api.Course.Code.List,
                Code.Course.ListRequest(course.id)
        )

        return handleFiles(tree, ids, Mode.COURSE, course.id, files)
    }

    suspend fun handleSub(
            tree: SuffixTree<Token>,
            ids: MutableMap<Long, PsiElement>,
            sub: JsonObject): Boolean {

        val files: Code.ListResponse = sendJsonableAsync(
                Address.Api.Submission.Code.List,
                Code.Submission.ListRequest(sub.getInteger("id"))
        )

        return handleFiles(tree, ids, Mode.SUBMISSION, sub.getInteger("id"), files)
    }

    suspend fun handleFiles(
            tree: SuffixTree<Token>,
            ids: MutableMap<Long, PsiElement>,
            mode: Mode,
            id: Int,
            files: Code.ListResponse): Boolean {

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
                            .map((::makeLiteralToken).bind(_0, when (mode) {
                                Mode.COURSE -> BASE_SUBMISSION_ID
                                Mode.SUBMISSION -> id
                            }))
                }
                .forEach { (method, tokens) ->
                    val lst = tokens.toList()
                    log.trace("lst = $lst")
                    val seqId = tree.addSequence(lst)
                    ids.put(seqId, method)
                }

        run(ee) { FooBarCompiler.tearDownMyEnv(compilerEnv) }

        return true
    }

    suspend fun handleReport(
            tree: SuffixTree<Token>,
            ids: MutableMap<Long, PsiElement>,
            data: List<JsonObject>) {

        val dataBySubmissionId = data.map { it.getInteger("id") to it }.toMap()

        val clones =
                tree.root.dfs {
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
                .filter { cc -> BASE_SUBMISSION_ID !in cc.clones.map { it.submissionId } }
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
                    cloneClass.clones.map { clone -> clone.submissionId to cloneClass }
                }
                .groupBy { it.first }
                .mapValues { it.value.map { it.second } }

        clonesBySubmission.asSequence()
                .forEach { (submissionId_, cloneClasses) ->
                    dbCreateAsync(SubmissionResultRecord().apply {
                        submissionId = submissionId_
                        type = RESULT_TYPE
                        // FIXME: akhin Make this stuff Jsonable or what?
                        body = cloneClasses.map { cloneClass ->
                            cloneClass.clones.map { clone ->
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

    }
}
