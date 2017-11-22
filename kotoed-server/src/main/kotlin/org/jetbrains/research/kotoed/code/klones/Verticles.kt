package org.jetbrains.research.kotoed.code.klones

import com.intellij.psi.PsiElement
import com.suhininalex.suffixtree.SuffixTree
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.run
import org.jetbrains.kootstrap.FooBarCompiler
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.research.kotoed.data.api.SubmissionCode
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

    private val ee by lazy { newSingleThreadContext("kloneVerticle.executor") }

    @JsonableEventBusConsumerFor(Address.Code.KloneCheck)
    suspend fun handleCheck(course: CourseRecord) {
        val allProjects = dbFindAsync(ProjectRecord().apply { courseId = course.id })

        val allSubmissions = allProjects.mapNotNull {
            dbFindAsync(SubmissionRecord().apply {
                projectId = it.id
                state = SubmissionState.open
            }).firstOrNull()
        }

        val tree = SuffixTree<Token>()
        val ids = mutableMapOf<Long, PsiElement>()

        for (sub in allSubmissions) handleCheck(tree, ids, sub)

        handleReport(tree, ids)
    }

    suspend fun handleCheck(
            tree: SuffixTree<Token>,
            ids: MutableMap<Long, PsiElement>,
            sub: SubmissionRecord) {

        val submission = when (sub.state) {
            null -> dbFetchAsync(sub)
            else -> sub
        }

        val files: SubmissionCode.ListResponse = sendJsonableAsync(
                Address.Api.Submission.Code.List,
                SubmissionCode.ListRequest(submission.id)
        )

        if (files.status != CloneStatus.done) {
            log.trace("Repository not cloned yet")
            return
        }

        val compilerEnv = run(ee) { FooBarCompiler.setupMyEnv(CompilerConfiguration()) }

        val allFiles = files.root?.toFileSeq().orEmpty()

        val ktFiles = allFiles
                .filter { it.endsWith(".kt") }
                .toList()
                .map { filename ->
                    log.trace("filename = ${filename}")
                    val resp: SubmissionCode.ReadResponse =
                            sendJsonableAsync(Address.Api.Submission.Code.Read,
                                    SubmissionCode.ReadRequest(
                                            submissionId = submission.id, path = filename))
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
                            .map((::makeLiteralToken).bind(_0, submission.id))
                }
                .forEach { (method, tokens) ->
                    val lst = tokens.toList()
                    log.trace("lst = $lst")
                    val id = tree.addSequence(lst)
                    ids.put(id, method)
                }

        run(ee) { FooBarCompiler.tearDownMyEnv(compilerEnv) }
    }

    suspend fun handleReport(
            tree: SuffixTree<Token>,
            ids: MutableMap<Long, PsiElement>) {

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
                    0 == node.parentEdges.last().begin
                }

        log.trace(clones.joinToString("\n"))

        val filtered = clones
                .map(::CloneClass)
                .filter { cc -> cc.clones.isNotEmpty() }
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
