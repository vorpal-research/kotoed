package org.jetbrains.research.kotoed.api

import com.intellij.psi.PsiElement
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.research.kotoed.code.diff.HunkJsonable
import org.jetbrains.research.kotoed.code.diff.RangeJsonable
import org.jetbrains.research.kotoed.data.api.Code
import org.jetbrains.research.kotoed.data.api.VerificationData
import org.jetbrains.research.kotoed.data.api.VerificationStatus
import org.jetbrains.research.kotoed.data.vcs.CloneStatus
import org.jetbrains.research.kotoed.database.tables.records.*
import org.jetbrains.research.kotoed.db.processors.getFullName
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.code.getPsi
import org.jetbrains.research.kotoed.util.code.temporaryKotlinEnv
import java.util.function.Consumer

@AutoDeployable
class HashComputingVerticle : AbstractKotoedVerticle(), Loggable {
    private val ee by lazy { betterSingleThreadContext("hashComputingVerticle.executor") }
    private val treeHashVisitor = TreeHashVisitor()

    @JsonableEventBusConsumerFor(Address.Code.Hashes)
    suspend fun computeHashesFromSub(res: SubmissionRecord): VerificationData {
        log.info("Start computing hashing for submission=[${res.id}]")
        try {
            val diffResponse: Code.Submission.DiffResponse = sendJsonableAsync(
                Address.Api.Submission.Code.DiffWithPrevious,
                Code.Submission.DiffRequest(submissionId = res.id)
            )

            val files: Code.ListResponse = sendJsonableAsync(
                Address.Api.Submission.Code.List,
                Code.Submission.ListRequest(res.id)
            )

            temporaryKotlinEnv {
                withContext(ee) {
                    val ktFiles =
                        files.root?.toFileSeq()
                            .orEmpty()
                            .filter { it.endsWith(".kt") }
                            .toList()                           //FIXME
                            .map { filename ->
                                val resp: Code.Submission.ReadResponse = sendJsonableAsync(
                                    Address.Api.Submission.Code.Read,
                                    Code.Submission.ReadRequest(
                                        submissionId = res.id, path = filename
                                    )
                                )
                                getPsi(resp.contents, filename)
                            }
                    val functionsList = ktFiles.asSequence()
                        .flatMap { file ->
                            file.collectDescendantsOfType<KtNamedFunction>().asSequence()
                        }
                        .filter { method ->
                            method.annotationEntries.all { anno -> "@Test" != anno.text } &&
                                    !method.containingFile.name.startsWith("test")
                        }
                        .toList()

                    val changesInFiles = diffResponse.diff.associate {
                        if (it.toFile != it.fromFile) {
                            log.warn("File [${it.fromFile}] is renamed to [${it.toFile}]")
                        }
                        it.toFile to it.changes
                    }
                    val project = dbFindAsync(ProjectRecord().apply { id = res.projectId }).first()
                    for (function in functionsList) {
                        val needProcess = function.isTopLevel || function.parent is KtClassBody
                        if (!needProcess) {
                            continue
                        }
                        processFunction(function, res, changesInFiles, project)
                    }

                }
            }
        } catch (ex: Throwable) {
            log.error(ex)
            return VerificationData(VerificationStatus.Invalid, emptyList())
        }
        return VerificationData(VerificationStatus.Processed, emptyList())
    }

    private suspend fun processFunction(
        psiFunction: KtNamedFunction,
        res: SubmissionRecord,
        changesInFiles: Map<String, List<HunkJsonable>>,
        project: ProjectRecord
    ) {
        val functionFullName = psiFunction.getFullName()
        if (psiFunction.bodyExpression == null) {
            log.info("BodyExpression is null in function=${functionFullName}, submission=${res.id}")
            return
        }
        val functionRecord = dbFindAsync(FunctionRecord().apply { name = functionFullName })
        val functionFromDb: FunctionRecord
        when (functionRecord.size) {
            0 -> {
                log.info("Add new function=[${functionFullName}] in submission=[${res.id}]")
                try {
                    functionFromDb = dbCreateAsync(FunctionRecord().apply { name = functionFullName })
                } catch (e: Exception) {
                    log.error("Cant add function $functionFullName to functions table", e)
                    return
                }
            }

            1 -> {
                functionFromDb = functionRecord.first()!!
            }

            else -> {
                throw IllegalStateException(
                    "Amount of function [${functionFullName}] in table is ${functionRecord.size}"
                )
            }
        }
        val document = psiFunction.containingFile.viewProvider.document
            ?: throw IllegalStateException("Function's=[${psiFunction.containingFile.name}] document is null")
        val fileChanges = changesInFiles[psiFunction.containingFile.name] ?: return //no changes in file at all
        val funStartLine = document.getLineNumber(psiFunction.startOffsetSkippingComments) + 1
        val funFinishLine = document.getLineNumber(psiFunction.endOffset) + 1
        for (change in fileChanges) {
            val fileRange = change.to
            if (isNeedToRecomputeHash(funStartLine, funFinishLine, fileRange)) {
                val hashesForLevels: MutableList<VisitResult> = computeHashesForElement(psiFunction.bodyExpression!!)
                putHashesInTable(hashesForLevels, functionFromDb, res, project)
                return
            }
        }
    }

    private suspend fun putHashesInTable(
        hashes: MutableList<VisitResult>,
        functionFromDb: FunctionRecord,
        res: SubmissionRecord,
        project: ProjectRecord
    ) {
        if (hashes.isEmpty()) {
            log.info("Hashes for funId=${functionFromDb.id}, subId=${res.id} is empty")
            return
        }
        dbBatchCreateAsync(hashes.map {
            FunctionPartHashRecord().apply {
                functionid = functionFromDb.id
                submissionid = res.id
                projectid = project.id
                leftbound = it.leftBound
                rightbound = it.rightBound
                hash = it.levelHash
            }
        })
        log.info("functionid = ${functionFromDb.id}, submissionid = ${res.id}, leavesСount = ${hashes.last().leafNum}")
        dbCreateAsync(FunctionLeavesRecord().apply {
            functionid = functionFromDb.id
            submissionid = res.id
            leavescount = hashes.last().leafNum
        })
    }

    fun computeHashesForElement(root: PsiElement): MutableList<VisitResult> {
        val visitResults = mutableListOf<VisitResult>()
        val consumers = listOf(Consumer<VisitResult>{
            visitResults.add(it)
        })
        treeHashVisitor.visitTree(root, consumers)
        return visitResults
    }

    private fun isNeedToRecomputeHash(funStartLine: Int, funFinishLine: Int, changesRange: RangeJsonable): Boolean {
        val start = changesRange.start
        val finish = start + changesRange.count
        val out = start > funFinishLine || finish < funStartLine
        return !out
    }
}