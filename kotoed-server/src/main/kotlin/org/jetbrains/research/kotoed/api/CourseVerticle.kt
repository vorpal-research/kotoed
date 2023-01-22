package org.jetbrains.research.kotoed.api

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.research.kotoed.data.api.*
import org.jetbrains.research.kotoed.data.db.ComplexDatabaseQuery
import org.jetbrains.research.kotoed.data.db.setPageForQuery
import org.jetbrains.research.kotoed.data.db.textSearch
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.Tables.COURSE_TEXT_SEARCH
import org.jetbrains.research.kotoed.database.tables.records.BuildTemplateRecord
import org.jetbrains.research.kotoed.database.tables.records.CourseRecord
import org.jetbrains.research.kotoed.database.tables.records.CourseStatusRecord
import org.jetbrains.research.kotoed.database.tables.records.FunctionRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.code.getPsi
import org.jetbrains.research.kotoed.util.code.temporaryKotlinEnv
import org.jetbrains.research.kotoed.util.database.toRecord
import java.lang.StringBuilder

fun KtNamedFunction.getFullName(): String {
    val resultName = StringBuilder(this.containingFile.name)
    resultName.append(this.name)
    for (valueParameter in this.valueParameters) {
        resultName.append(valueParameter.typeReference!!.text)
    }
    return resultName.toString()
}

@AutoDeployable
class CourseVerticle : AbstractKotoedVerticle(), Loggable {
    private val ee by lazy { betterSingleThreadContext("courseVerticle.executor") }

    @JsonableEventBusConsumerFor(Address.Api.Course.Create)
    suspend fun handleCreate(course: CourseRecord): DbRecordWrapper<CourseRecord> {
        course.name = course.name.truncateAt(1024)

        if(course.buildTemplateId == null) {
            // first, create a new build template
            val build = dbCreateAsync(BuildTemplateRecord().apply {
                commandLine = JsonArray()
                environment = JsonObject()
            })
            course.buildTemplateId = build.id
        }

        val res: CourseRecord = dbCreateAsync(course)
        dbProcessAsync(res)
        return DbRecordWrapper(res)
    }

    @JsonableEventBusConsumerFor(Address.Api.Course.Read)
    suspend fun handleRead(course: CourseRecord): DbRecordWrapper<CourseRecord> {
        val res: CourseRecord = dbFetchAsync(course)
        val status: VerificationData = dbProcessAsync(res)
        return DbRecordWrapper(res, status)
    }

    @JsonableEventBusConsumerFor(Address.Api.Course.Update)
    suspend fun handleUpdate(course: CourseRecord): DbRecordWrapper<CourseRecord> {
        val res: CourseRecord = dbUpdateAsync(course)
        val status: VerificationData = dbProcessAsync(res)

        val files: Code.ListResponse = sendJsonableAsync(
            Address.Api.Course.Code.List,
            Code.Course.ListRequest(course.id)
        )
        temporaryKotlinEnv {//FIXME COPYPASTE
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
                    .forEach {
                        val resultName = it.getFullName()
                        val functionRecord = FunctionRecord().apply { name = resultName }
                        if (dbFindAsync(functionRecord).isEmpty()) {
                            dbCreateAsync(functionRecord)
                        }
                    }
            }
        }
        return DbRecordWrapper(res, status)
    }

    @JsonableEventBusConsumerFor(Address.Api.Course.Error)
    suspend fun handleError(verificationData: VerificationData): List<CourseStatusRecord> =
            verificationData.errors
                    .map { fetchByIdAsync(Tables.COURSE_STATUS, it) }

    @JsonableEventBusConsumerFor(Address.Api.Course.Verification.Data)
    suspend fun handleVerificationData(course: CourseRecord): VerificationData =
            dbVerifyAsync(course)

    @JsonableEventBusConsumerFor(Address.Api.Course.Search)
    suspend fun handleSearch(query: SearchQuery): JsonArray {
        val req: List<JsonObject> = dbQueryAsync(COURSE_TEXT_SEARCH) {
            setPageForQuery(query)
            sortBy("-id")
            textSearch(query.text)
        }

        val reqWithVerificationData = if (query.withVerificationData ?: false) {
            req.map { json ->
                val record: CourseRecord = json.toRecord()
                val vd = dbProcessAsync(record)
                json["verificationData"] = vd.toJson()
                json
            }
        } else req

        return JsonArray(reqWithVerificationData)
    }

    @JsonableEventBusConsumerFor(Address.Api.Course.SearchCount)
    suspend fun handleSearchCount(query: SearchQuery): CountResponse {
        val q = ComplexDatabaseQuery("course_text_search").textSearch(query.text)

        return sendJsonableAsync(Address.DB.count("course_text_search"), q)
    }
}
