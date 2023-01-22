package org.jetbrains.research.kotoed.db.processors

import com.intellij.psi.PsiElement
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.withContext
import kotlinx.warnings.Warnings
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.research.kotoed.api.getFullName
import org.jetbrains.research.kotoed.code.Filename
import org.jetbrains.research.kotoed.code.Location
import org.jetbrains.research.kotoed.code.diff.HunkJsonable
import org.jetbrains.research.kotoed.code.diff.RangeJsonable
import org.jetbrains.research.kotoed.data.api.Code
import org.jetbrains.research.kotoed.data.api.VerificationData
import org.jetbrains.research.kotoed.data.api.VerificationStatus
import org.jetbrains.research.kotoed.data.buildSystem.BuildAck
import org.jetbrains.research.kotoed.data.vcs.*
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.Tables.*
import org.jetbrains.research.kotoed.database.enums.SubmissionState
import org.jetbrains.research.kotoed.database.tables.records.*
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.code.getPsi
import org.jetbrains.research.kotoed.util.code.temporaryKotlinEnv
import org.jetbrains.research.kotoed.util.database.executeKAsync
import org.jetbrains.research.kotoed.util.database.toRecord
import org.jooq.ForeignKey
import java.io.File
import java.util.function.BiConsumer
import java.util.function.BiFunction

data class BuildTriggerResult(
        val result: String,
        val buildRequestId: Int
) : Jsonable

@AutoDeployable
class SubmissionProcessorVerticle : ProcessorVerticle<SubmissionRecord>(Tables.SUBMISSION) {
    private val ee by lazy { betterSingleThreadContext("submissionProcessorVerticle.executor") }

    // parent submission id can be invalid, filter it out
    override val checkedReferences: List<ForeignKey<SubmissionRecord, *>>
        get() = super.checkedReferences
                .filterNot { Tables.SUBMISSION.PARENT_SUBMISSION_ID in it.fieldsArray }

    private val SubmissionCommentRecord.location
        get() = Location(Filename(path = sourcefile), sourceline)

    private suspend fun recreateCommentsAsync(vcsUid: String, parent: SubmissionRecord, child: SubmissionRecord) {
        val submissionCacheAsync = AsyncCache { id: Int -> fetchByIdAsync(Tables.SUBMISSION, id) }
        val commentCacheAsync = AsyncCache { id: Int -> fetchByIdAsync(Tables.SUBMISSION_COMMENT, id) }
        val ancestorCommentCacheAsync = AsyncCache { comment: SubmissionCommentRecord ->
            dbFindAsync(SubmissionCommentRecord().apply {
                submissionId = comment.originalSubmissionId
                persistentCommentId = comment.persistentCommentId
            }).expecting(
                    message = "Duplicate or missing comment in chain detected: " +
                            "submission.id = ${comment.originalSubmissionId} " +
                            "comment.id = ${comment.persistentCommentId}"
            ) { 1 == it.size }
                    .first()
        }

        val parentComments =
                dbFindAsync(SubmissionCommentRecord().apply { submissionId = parent.id })

        val alreadyMappedPersistentIds =
                dbFindAsync(SubmissionCommentRecord().apply { submissionId = child.id }).map { it.persistentCommentId }

        // first, we create all the missing comments

        val childComments: List<SubmissionCommentRecord> =
                parentComments
                        .asSequence()
                        .filter { it.persistentCommentId !in alreadyMappedPersistentIds }
                        .mapTo(mutableListOf()) { comment ->
                            dbCreateAsync(comment.copy().apply { submissionId = child.id })
                        }

        // second, we remap all the locations and reply-chains

        childComments.forEach { comment ->
            val ancestorComment = ancestorCommentCacheAsync(comment)
            val ancestorSubmission = submissionCacheAsync(ancestorComment.submissionId)

            val adjustedLocation: LocationResponse =
                    sendJsonableAsync(
                            Address.Code.LocationDiff,
                            LocationRequest(
                                    vcsUid,
                                    ancestorComment.location,
                                    ancestorSubmission.revision,
                                    child.revision
                            )
                    )
            comment.sourcefile = adjustedLocation.location.filename.path
            comment.sourceline = adjustedLocation.location.line

            if (comment.previousCommentId != null) {
                val prevAncestor = ancestorCommentCacheAsync(commentCacheAsync(comment.previousCommentId))
                val previousComment = childComments.find { it.persistentCommentId == prevAncestor.persistentCommentId }
                comment.previousCommentId = previousComment?.id
            }

            dbUpdateAsync(comment)
        }

        data class DialoguePoint(var prev: DialoguePoint? = null, val value: SubmissionCommentRecord) {
            val head: DialoguePoint get() = prev?.head?.also { prev = it } ?: this
        }

        val dialogues = childComments.map { it.id to DialoguePoint(value =  it) }.toMap()
        dialogues.forEach { (_, v) ->
            v.prev = dialogues[v.value.previousCommentId]
        }
        dialogues.forEach { (_, v) ->
            v.value.sourceline = v.head.value.sourceline
            v.value.sourcefile = v.head.value.sourcefile
            dbUpdateAsync(v.value)
        }
    }

    private suspend fun copyTagsFrom(parent: SubmissionRecord, child: SubmissionRecord) {
        val parentTags = dbFindAsync(
                SubmissionTagRecord().apply { submissionId = parent.id })

        try {
            dbBatchCreateAsync(parentTags.map { it.apply { submissionId = child.id } })
        } catch (ex: Exception) {
            log.error(ex.message, ex)
        }
    }

    private suspend fun getVcsInfo(project: ProjectRecord): RepositoryInfo {
        return sendJsonableAsync(
                Address.Code.Download,
                RemoteRequest(VCS.valueOf(project.repoType), project.repoUrl).toJson()
        )
    }

    private suspend fun getVcsStatus(
            vcsInfo: RepositoryInfo,
            submission: SubmissionRecord): VerificationData {

        return when (vcsInfo.status) {
            CloneStatus.pending -> VerificationData.Unknown
            CloneStatus.done -> VerificationData.Processed
            CloneStatus.failed ->
                dbCreateAsync(
                        SubmissionStatusRecord().apply {
                            this.submissionId = submission.id
                            this.data = JsonObject(
                                    "failure" to "Fetching remote repository failed",
                                    "details" to vcsInfo.toJson()
                            )
                        }
                ).id.let { VerificationData.Invalid(it) }
        }
    }

    suspend override fun doProcess(data: JsonObject): VerificationData = run {
        val sub: SubmissionRecord = data.toRecord()
        val project: ProjectRecord = fetchByIdAsync(Tables.PROJECT, sub.projectId)

        val parentSub: SubmissionRecord? = sub.parentSubmissionId?.let {
            fetchByIdAsync(Tables.SUBMISSION, sub.parentSubmissionId)
        }

        parentSub?.let {
            it.state = SubmissionState.obsolete
            dbUpdateAsync(it)
        }

        val vcsReq = getVcsInfo(project)

        val vcsStatus = getVcsStatus(vcsReq, sub)

        if (vcsStatus != VerificationData.Processed) return@run vcsStatus

        if (sub.revision == null) {
            val vcsInfo: InfoFormat = sendJsonableAsync(Address.Code.Info, InfoFormat(uid = vcsReq.uid))
            sub.revision = vcsInfo.revision
            dbUpdateAsync(sub)
        }

        parentSub?.let {
            recreateCommentsAsync(vcsReq.uid, it, sub)

            copyTagsFrom(it, sub)
        }

        val buildInfos = dbFindAsync(BuildRecord().apply { submissionId = sub.id })

        val localVerificationData = try {

            when (buildInfos.size) {
                0 -> {
                    val ack: BuildAck = sendJsonableAsync(
                            Address.BuildSystem.Build.Submission.Request,
                            SubmissionRecord().apply { id = sub.id }
                    )

                    dbCreateAsync(
                            BuildRecord().apply {
                                submissionId = sub.id
                                buildRequestId = ack.buildId
                            }
                    )
                    computeHashesFromSub(sub) //FIXME call only when create submission
                    VerificationData.Processed
                }
                1 -> {
                    VerificationData.Processed
                }
                else -> {
                    val errorId = dbCreateAsync(
                            SubmissionStatusRecord().apply {
                                this.submissionId = sub.id
                                this.data = JsonObject(
                                        "failure" to "Several builds found for submission ${sub.id}",
                                        "details" to buildInfos.tryToJson()
                                )
                            }
                    ).id

                    return VerificationData.Invalid(errorId)
                }
            }

        } catch (ex: Exception) {
            val errorId = dbCreateAsync(
                    SubmissionStatusRecord().apply {
                        this.submissionId = sub.id
                        this.data = JsonObject(
                                "failure" to "Triggering build for ${project.name}:${sub.id} failed",
                                "details" to ex.message
                        )
                    }
            ).id

            VerificationData.Invalid(errorId)
        }

        return@run localVerificationData and verify(data)

    }.apply {
        val sub: SubmissionRecord = data.toRecord()
        val subFresh = dbFetchAsync(sub)
        if (status == VerificationStatus.Processed
                && subFresh.state == SubmissionState.pending) {
            dbUpdateAsync(subFresh.apply { state = SubmissionState.open })
        } else if (status == VerificationStatus.Invalid
                    && subFresh.state == SubmissionState.pending) {
            dbUpdateAsync(subFresh.apply { state = SubmissionState.invalid })
        }
    }

    suspend override fun verify(data: JsonObject?): VerificationData {
        data ?: throw IllegalArgumentException("Cannot verify null submission")

        val sub: SubmissionRecord = data.toRecord()
        val project: ProjectRecord = fetchByIdAsync(Tables.PROJECT, sub.projectId)
        val parentSub: SubmissionRecord? = sub.parentSubmissionId?.let {
            fetchByIdAsync(Tables.SUBMISSION, sub.parentSubmissionId)
        }

        val vcsReq = getVcsInfo(project)

        val vcsStatus = getVcsStatus(vcsReq, sub)

        if (vcsStatus != VerificationData.Processed) return vcsStatus

        try {
            val list: ListResponse = sendJsonableAsync(
                    Address.Code.List,
                    ListRequest(vcsReq.uid, sub.revision)
            )

            list.ignore()

        } catch (ex: Exception) {
            val errorId = dbCreateAsync(
                    SubmissionStatusRecord().apply {
                        this.submissionId = sub.id
                        this.data = JsonObject(
                                "failure" to "Fetching revision ${sub.revision} for repository ${project.repoUrl} failed",
                                "details" to ex.message
                        )
                    }
            ).id

            return VerificationData.Invalid(errorId)
        }

        parentSub?.let {
            val parentComments = dbFindAsync(SubmissionCommentRecord().apply { submissionId = parentSub.id })

            val ourComments = dbFindAsync(SubmissionCommentRecord().apply { submissionId = sub.id })
                    .asSequence()
                    .map { it.persistentCommentId }
                    .toSet()

            if (parentSub.state != SubmissionState.obsolete) {
                return VerificationData.Unknown
            }

            if (!parentComments.all { it.persistentCommentId in ourComments }) {
                return VerificationData.Unknown
            }
        }

        val buildInfos = dbFindAsync(BuildRecord().apply { submissionId = sub.id })

        return when (buildInfos.size) {
            1 -> VerificationData.Processed
            0 -> VerificationData.Unknown
            else -> {
                dbCreateAsync(
                        SubmissionStatusRecord().apply {
                            this.submissionId = sub.id
                            this.data = JsonObject(
                                    "failure" to "Several builds found for submission ${sub.id}",
                                    "details" to buildInfos.tryToJson()
                            )
                        }
                ).id.let { VerificationData.Invalid(it) }
            }
        }
    }

    suspend override fun doClean(data: JsonObject): VerificationData {
        val sub: SubmissionRecord = data.toRecord()

        async {
            val project = dbFetchAsync(ProjectRecord().apply { id = sub.projectId })
            sendJsonable(Address.Code.PurgeCache, RemoteRequest(vcs = null, url = project.repoUrl))
        }

        dbWithTransactionAsync {
            deleteFrom(SUBMISSION_STATUS)
                    .where(SUBMISSION_STATUS.SUBMISSION_ID.eq(sub.id))
                    .executeKAsync()
            deleteFrom(SUBMISSION_RESULT)
                    .where(SUBMISSION_RESULT.SUBMISSION_ID.eq(sub.id))
                    .executeKAsync()
            deleteFrom(BUILD)
                    .where(BUILD.SUBMISSION_ID.eq(sub.id))
                    .executeKAsync()
        }

        return VerificationData.Unknown
    }
    private suspend fun computeHashesFromSub(res: SubmissionRecord) {
        log.info("Start computing hashing for submission=[${res.id}]")
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
                        method.annotationEntries.all { anno -> "@Test" != anno.text }
                    }
                    .map {
                        @Suppress(Warnings.USELESS_CAST)
                        it as PsiElement
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
                    processFunction(function, res, changesInFiles, project)
                }

            }
        }

    }

    private suspend fun processFunction(
        function: PsiElement,
        res: SubmissionRecord,
        changesInFiles: Map<String, List<HunkJsonable>>,
        project: ProjectRecord
    ) {
        val functionFullName = (function as KtNamedFunction).getFullName()
        val functionRecord = dbFindAsync(FunctionRecord().apply { name = functionFullName })
        val functionFromDb: FunctionRecord
        when (functionRecord.size) {
            0 -> {
                log.info("Add new function=[${functionFullName}] in submission=[${res.id}]")
                functionFromDb = dbCreateAsync(FunctionRecord().apply { name = functionFullName })
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
        val document = function.containingFile.viewProvider.document
            ?: throw IllegalStateException("Function's=[${function.containingFile.name}] document is null")
        val fileChanges = changesInFiles[function.containingFile.name] ?: return
        val funStartLine = document.getLineNumber(function.startOffsetSkippingComments) + 1
        val funFinishLine = document.getLineNumber(function.endOffset) + 1
        for (change in fileChanges) {
            val range = change.to
            if (isNeedToRecomputeHash(funStartLine, funFinishLine, range)) {
                val hashesForLevels: List<List<Int>> = computeHashesForElement(function.bodyExpression as PsiElement)
                putHashesInTable(hashesForLevels, functionFromDb, res, project)
                break
            }
        }
    }

    private suspend fun putHashesInTable(
        hashesForLevels: List<List<Int>>,
        functionFromDb: FunctionRecord,
        res: SubmissionRecord,
        project: ProjectRecord
    ) {
        hashesForLevels.forEachIndexed { index, hashes ->
            dbBatchCreateAsync(hashes.map {
                FunctionPartHashRecord().apply {
                    functionid = functionFromDb.id
                    submissionid = res.id
                    denizenId = project.denizenId
                    hash = it
                    level = index
                }
            })
        }
    }

    fun computeHashesForElement(root: PsiElement): List<List<Int>> {
        val associativeArray = mutableListOf<MutableList<Int>>(mutableListOf())
        val treeVisitor = object : TreeVisitor<Int> {
            override fun getLeaf(root: PsiElement): Int {
                return getSelf(root)
            }

            override fun getSelf(element: PsiElement): Int {
                return element.javaClass.name.hashCode()
            }

            override fun getAccumulator(): BiFunction<Int, Int, Int> {
                return BiFunction { a, b -> a + b }
            }

            override fun getConsumers(): List<BiConsumer<Int, Int>> {
                return listOf(BiConsumer<Int, Int> { hash, level ->
                    if (level > associativeArray.size) {
                        throw IllegalStateException(
                            "Level=[${level}] more than associativeArray size=[${associativeArray.size}]"
                        )
                    }
                    if (level == associativeArray.size) {
                        associativeArray.add(mutableListOf())
                    }
                    associativeArray[level].add(hash)
                })
            }
        }
        treeVisitor.dfs(root)
        return associativeArray
    }

    private fun isNeedToRecomputeHash(funStartLine: Int, funFinishLine: Int, changesRange: RangeJsonable): Boolean {
        val start = changesRange.start
        val finish = start + changesRange.count
        val out = start > funFinishLine || finish < funStartLine
        return !out
    }


    private fun computeStatistics(list: List<PsiElement>) {
        val treeVisitor = object : TreeVisitor<Int> {
            override fun getLeaf(root: PsiElement): Int {
                return 0
            }

            override fun getSelf(element: PsiElement): Int {
                return 1
            }

            override fun getAccumulator(): BiFunction<Int, Int, Int> {
                return BiFunction { a, b -> a + b }
            }
        }
        val otherList = list.map {
            val levelsCount = treeVisitor.dfs(it.children.last()).first
            val linesCount = it.children[it.children.size - 1].text.count { chr -> chr == '\n' } + 1
            (it as KtNamedFunction).name to Pair(levelsCount, linesCount)
        }.toList()
        val sortedRatio = otherList
            .map { el -> el.second.first * 1.0 / el.second.second }
            .sorted()
        val median: Double =
            if (sortedRatio.size % 2 == 0) (sortedRatio[sortedRatio.size / 2 - 1] + sortedRatio[sortedRatio.size / 2]) / 2.0
            else sortedRatio[sortedRatio.size / 2] * 1.0
        val percentile95: Double = sortedRatio[(sortedRatio.size * 0.95).toInt()]

        File("InformationAboutCommit")
            .bufferedWriter()
            .use { out ->
                var treeLevelsSum = 0
                var linesSum = 0
                for (pair in otherList) {
                    treeLevelsSum += pair.second.first
                    val lineInFunctions = pair.second.second
                    linesSum += lineInFunctions
                    out.write("Name:${pair.first} ${pair.second.first} $lineInFunctions")
                    out.newLine()
                }
                out.write("All statistics: TreeLevels:${treeLevelsSum} FunctionsLines:${linesSum}")
                out.newLine()
                out.write("Median:${median} 95_Percentile:${percentile95}")
                out.flush()
            }
    }
}
