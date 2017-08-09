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

    val RESULT_TYPE = "klonecheck"

    var tree = SuffixTree<Token>()
    val submitted = mutableSetOf<Int>()
    val ee by lazy { newSingleThreadContext("KloneVerticle.executor") }

    @JsonableEventBusConsumerFor(Address.Code.KloneCheck)
    suspend fun handleCheck(course: CourseRecord) {
        val allProjects = dbFindAsync(ProjectRecord().apply { courseId = course.id })

        val allSubmissions = allProjects.map {
            dbFindAsync(SubmissionRecord().apply { projectId = it.id; state = SubmissionState.open }).firstOrNull()
        }.filterNotNull()

        for(sub in allSubmissions) handleCheck(sub)
    }

    suspend fun handleCheck(sub_: SubmissionRecord) {
        val submission = when(sub_.state) {
            null -> dbFetchAsync(sub_)
            else -> sub_
        }

        val files: SubmissionCode.ListResponse = sendJsonableAsync(
                Address.Api.Submission.Code.List,
                SubmissionCode.ListRequest(submission.id)
        )

        if (files.status != CloneStatus.done) {
            log.trace("Repository not cloned yet")
            return
        }
        if (submission.id in submitted) return;

        val compilerEnv = run(ee) { FooBarCompiler.setupMyEnv(CompilerConfiguration()) }

        val allFiles = files.root?.toFileSeq().orEmpty()

        val ktFiles = allFiles
                .filter { it.endsWith(".kt") }
                .toList()
                .map { filename ->
                    log.trace("filename = ${filename}")
                    val resp: SubmissionCode.ReadResponse =
                            sendJsonableAsync(Address.Api.Submission.Code.Read,
                                    SubmissionCode.ReadRequest(submissionId = submission.id, path = filename)
                            )
                    run(ee) { KtPsiFactory(compilerEnv.project).createFile(filename, resp.contents) }
                }

        val ids = mutableMapOf<Long, PsiElement>()

        ktFiles.asSequence().flatMap { file ->
            file.collectDescendantsOfType<PsiElement>{ it is KtNamedFunction }.asSequence()
        }
                .map { method ->
                    method to method.dfs { children.asSequence() }
                            .filter(Token.DefaultFilter)
                            .map((::makeLiteralToken).bind(_0, submission.id))
                }
                .forEach { (method, tokens) ->
                    val lst = tokens.toList()
                    log.trace("lst = ${lst}")
                    val id = tree.addSequence(lst)
                    ids.put(id, method)
                }

        submitted += submission.id

        run(ee) { FooBarCompiler.tearDownMyEnv(compilerEnv) }

        val clones =
                tree.root.dfs {
                    edges
                            .asSequence()
                            .map { it.terminal }
                            .filterNotNull()
                }.filter { node ->
                    node.edges
                            .asSequence()
                            .all { it.begin == it.end && it.begin == it.sequence.size - 1 }
                }.filter { node ->
                    0 == node.parentEdges.last().begin
                }

        val filtered = clones
                .map(::CloneClass)
                .filter { cc -> cc.clones.isNotEmpty() }
                .filter { cc -> cc.clones.map { it.submissionId }.toSet().size != 1}
                .toList()

        filtered.forEachIndexed { i, cloneClass ->
            val builder = StringBuilder()
            val fname = cloneClass.clones
                    .take(1) // FIXME
                    .fold("") { _, c -> c.functionName }
            builder.appendln("($fname) Clone class $i:")
            cloneClass.clones.forEach { c ->
                builder.appendln("${c.submissionId}/${c.file.path}:${c.fromLine}:${c.toLine}")
            }
            builder.appendln()
            log.trace(builder)

            dbCreateAsync(SubmissionResultRecord().apply {
                submissionId = submission.id
                type = RESULT_TYPE
                body = TODO()
            })
        }

        log.trace(clones.joinToString("\n"))

    }
}