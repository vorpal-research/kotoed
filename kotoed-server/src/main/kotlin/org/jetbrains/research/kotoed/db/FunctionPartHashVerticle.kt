package org.jetbrains.research.kotoed.db

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.db.ComplexDatabaseQuery
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.FunctionPartHashRecord
import org.jetbrains.research.kotoed.database.tables.records.HashClonesRecord
import org.jetbrains.research.kotoed.database.tables.records.ProcessedProjectSubRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionResultRecord
import org.jetbrains.research.kotoed.util.AutoDeployable
import org.jetbrains.research.kotoed.util.dbBatchCreateAsync
import org.jetbrains.research.kotoed.util.dbFindAsync
import org.jooq.Record10
import java.io.BufferedWriter
import java.io.File
import java.util.Comparator
import java.util.function.Function
import kotlin.math.abs

@AutoDeployable
class FunctionPartHashVerticle : CrudDatabaseVerticleWithReferences<FunctionPartHashRecord>(Tables.FUNCTION_PART_HASH) {
    private val fph1 = Tables.FUNCTION_PART_HASH.`as`("fph1")
    private val fph2 = Tables.FUNCTION_PART_HASH.`as`("fph2")

    override suspend fun handleQuery(message_: ComplexDatabaseQuery): JsonArray {
        if (message_.filter == null) {
            compareKlones()
            return JsonArray()
        }
        val projectId = Integer.parseInt(message_.filter)

        val lastProcessedSub = dbFindAsync(ProcessedProjectSubRecord().apply {
            this.projectid = projectId
        })
        val lastProcessedSubId: Int = if (lastProcessedSub.isEmpty()) -1 else lastProcessedSub.first().submissionid
        val records = db {
            select(
                fph1.FUNCTIONID,
                fph1.SUBMISSIONID,
                fph1.PROJECTID,
                fph1.LEFTBOUND,
                fph1.RIGHTBOUND,
                fph2.FUNCTIONID,
                fph2.SUBMISSIONID,
                fph2.PROJECTID,
                fph2.LEFTBOUND,
                fph2.RIGHTBOUND
            )
                .from(fph1)
                .join(fph2)
                .on(fph1.HASH.eq(fph2.HASH))
                .where(
                    fph1.PROJECTID.eq(projectId)
                        .and(fph1.SUBMISSIONID.greaterThan(lastProcessedSubId))
                        .and(fph1.PROJECTID.notEqual(fph2.PROJECTID))
                )
                .orderBy(fph1.FUNCTIONID, fph1.SUBMISSIONID, fph2.FUNCTIONID, fph2.SUBMISSIONID)
                .fetch()
                .map { record -> intoHashCloneRecord(record) }
        }
        if (records.isEmpty()) {
            return JsonArray()
        }
        val comparingList: MutableList<HashClonesRecord> = mutableListOf(records[0])
        val segmentsMap: MutableMap<Pair<Int, Int>, MutableList<Pair<Int, Int>>> = hashMapOf()
        putClonesRecordsSegmentsIntoMap(segmentsMap, records[0])

        for (i in 1 until records.size) {
            if (isCloneRecordsFromDifferentFunctions(records[i], records[i - 1])) {
                processFunctionClones(comparingList, segmentsMap, records[i - 1])
            }
            comparingList.add(records[i])
            putClonesRecordsSegmentsIntoMap(segmentsMap, records[i])
        }
        processFunctionClones(comparingList, segmentsMap, records[records.size - 1])
        //TODO remember lastProcessedSub
        return JsonArray()
    }

    private suspend fun processFunctionClones(
        comparingList: MutableList<HashClonesRecord>,
        segmentsMap: MutableMap<Pair<Int, Int>, MutableList<Pair<Int, Int>>>,
        prevRecord: HashClonesRecord
    ) {
        val fFun = prevRecord.fFunctionid
        val fSub = prevRecord.fSubmissionid
        val fProj = prevRecord.fProjectid
        val sFun = prevRecord.sFunctionid
        val sSub = prevRecord.sSubmissionid
        val sProj = prevRecord.sProjectid

        val segments = comparingList.map { record ->
            record.fLeftbound to record.fRightbound
        }
        val nonAbsorbedSegments = absorbingSegments(segments)
        nonAbsorbedSegments.forEach { nonAbsorbedSegment ->
            val otherSegments = segmentsMap[nonAbsorbedSegment] ?: throw IllegalStateException(
                "After absorbing segments=${segments} for firstFunId=${fFun}, firstSubId=${fSub}, secondFunId=${sFun}," +
                        " secondSubId=${sSub} for nonAbsorbedSegment=${nonAbsorbedSegment} second functions segments are null"
            )
            dbBatchCreateAsync(otherSegments.map { otherSegment ->
                HashClonesRecord().apply {
                    fFunctionid = fFun
                    fSubmissionid = fSub
                    fProjectid = fProj
                    fLeftbound = nonAbsorbedSegment.first
                    fRightbound = nonAbsorbedSegment.second
                    sFunctionid = sFun
                    sSubmissionid = sSub
                    sProjectid = sProj
                    sLeftbound = otherSegment.first
                    sRightbound = otherSegment.second
                }
            })
        }
        comparingList.clear()
        segmentsMap.clear()
    }

    private suspend fun compareKlones() {
        val oldAlgoClones = JsonArray(dbFindAsync(SubmissionResultRecord().apply {
            id = 13
        }).first().body.toString())

        val newAlgoClones = dbFindAsync(
            HashClonesRecord().apply {
                fSubmissionid = 5
            })
        compareKlones(oldAlgoClones, newAlgoClones, null, "prodTest", setOf(5))
    }

    suspend fun compareKlones(
        oldAlgoClones: JsonArray,
        newAlgoClones: List<HashClonesRecord>,
        funIdProducer: (Function<JsonObject, Int>)?,
        fileName: String,
        subNums: (Set<Int>)?,
    ) {

        val oldAlgoMap = fillMapForOldAlgo(oldAlgoClones, funIdProducer, subNums)
        val newAlgoMap = fillNewAlgoMap(newAlgoClones)
        val sameRecords = mutableMapOf<Pair<Int, Int>, MutableSet<Pair<Int, Int>>>()
        File(fileName)
            .bufferedWriter()
            .use {
                it.write("Functions in Both maps")
                it.newLine()
                for (entry in oldAlgoMap.first.entries) {
                    val firstFun = entry.key
                    val sameOtherFunctionForFirstFun = mutableSetOf<Pair<Int, Int>>()
                    sameRecords[firstFun] = sameOtherFunctionForFirstFun
                    val newOtherFuns = newAlgoMap[firstFun] ?: continue
                    for (otherEntry in entry.value) {
                        val otherFun = otherEntry.key
                        val newCount = newOtherFuns[otherFun] ?: continue
                        sameOtherFunctionForFirstFun.add(otherFun)
                        val oldCount = otherEntry.value
                        val diff = abs(newCount - oldCount)
                        it.write(
                            "{funId=${firstFun.first} subId=${firstFun.second}} and {funId=${otherFun.first} subId=${otherFun.second}}:" +
                                    "newAlgo=$newCount oldAlgo=$oldCount diff=${diff}"
                        )
                        it.newLine()
                    }
                }
                it.write("Skipped functions count=${oldAlgoMap.second}")
                it.newLine()
                processFunctionsThatNotInBothMaps(oldAlgoMap.first, sameRecords, it, "OLD ALGO")
                processFunctionsThatNotInBothMaps(newAlgoMap, sameRecords, it, "NEW ALGO")
            }
    }

    private fun processFunctionsThatNotInBothMaps(
        map: MutableMap<Pair<Int, Int>, MutableMap<Pair<Int, Int>, Int>>,
        sameRecordsMap: MutableMap<Pair<Int, Int>, MutableSet<Pair<Int, Int>>>,
        writer: BufferedWriter,
        algoName: String
    ) {
        writer.write("For algo $algoName")
        writer.newLine()
        for (entry in map.entries) {
            val func = entry.key
            val sameOtherFunctions = sameRecordsMap[func]
            if (sameOtherFunctions == null) {
                for (notSameFunction in entry.value.entries) {
                    notInBothMapFunctionWrite(writer, func, notSameFunction.key, notSameFunction.value)
                }
                continue
            }
            for (otherEntry in entry.value.entries) {
                val otherFun = otherEntry.key
                if (!sameOtherFunctions.contains(otherFun)) {
                    notInBothMapFunctionWrite(writer, func, otherFun, otherEntry.value)
                }
            }
        }
        writer.write("END----------")
        writer.newLine()
    }

    private fun notInBothMapFunctionWrite(
        writer: BufferedWriter,
        func: Pair<Int, Int>,
        otherFun: Pair<Int, Int>,
        klonesCount: Int
    ) {
        writer.write(
            "Correlation between {funId=${func.first} subId=${func.second}} and {funId=${otherFun.first} subId=${otherFun.second}} " +
                    "with num=${klonesCount}"
        )
        writer.newLine()
    }

    private fun fillNewAlgoMap(allRecords: List<HashClonesRecord>): MutableMap<Pair<Int, Int>, MutableMap<Pair<Int, Int>, Int>> {
        val map = allRecords.groupBy {
            with(it) {
                listOf(fFunctionid, fSubmissionid, sFunctionid, sSubmissionid)
            }
        }
        val newAlgoMap = mutableMapOf<Pair<Int, Int>, MutableMap<Pair<Int, Int>, Int>>()
        for (entry in map.entries) {
            with(entry) {
                val fFunId = key[0] to key[1]
                val sFunId = key[2] to key[3]
                var fFunClones = newAlgoMap[fFunId]
                if (fFunClones == null) {
                    fFunClones = mutableMapOf()
                    newAlgoMap[fFunId] = fFunClones
                }
                if (fFunClones[sFunId] != null) {
                    throw IllegalStateException("fFunId=${fFunId} and sFunId=${sFunId} found again in groupBy")
                }
                fFunClones[sFunId] = value.size
            }
        }
        return newAlgoMap
    }

    private suspend fun fillMapForOldAlgo(
        clonesArray: JsonArray,
        funIdProducer: Function<JsonObject, Int>?,
        subNums: Set<Int>?,
    ):
            Pair<MutableMap<Pair<Int, Int>, MutableMap<Pair<Int, Int>, Int>>, Int> {
        var skippedFunctions = 0
        val countMap = mutableMapOf<Pair<Int, Int>, MutableMap<Pair<Int, Int>, Int>>()
        for (i in 0 until clonesArray.size()) {
            val sameTypeClones = clonesArray.getJsonArray(i)
            for (j in 0 until sameTypeClones.size()) {
                val firstFun = sameTypeClones.getJsonObject(j)
                if (subNums != null && !subNums.contains(firstFun.getInteger("submission_id"))) {
                    continue
                }
                val fFunUniqueId = getFunUniqueId(firstFun, funIdProducer)
                if (fFunUniqueId == null) {
                    skippedFunctions++
                    continue
                }
                if (countMap[fFunUniqueId] == null) {
                    countMap[fFunUniqueId] = mutableMapOf()
                }
                for (k in 0 until sameTypeClones.size()) {
                    val secondFun = sameTypeClones.getJsonObject(k)
                    if (secondFun.getInteger("submission_id") == firstFun.getInteger("submission_id")) {
                        continue
                    }
                    val sFunUniqueId = getFunUniqueId(secondFun, funIdProducer)
                    if (sFunUniqueId == null) {
                        skippedFunctions++
                        continue
                    }
                    val counter = countMap[fFunUniqueId]!!.getOrDefault(sFunUniqueId, 0)
                    countMap[fFunUniqueId]!![sFunUniqueId] = counter + 1
                }
            }
        }
        return countMap to skippedFunctions
    }

    private suspend fun getFunUniqueId(
        funObj: JsonObject,
        funIdProducer: (Function<JsonObject, Int>)?
    ): Pair<Int, Int>? {
        val subId = funObj.getInteger("submission_id")
        val funId: Int = funIdProducer?.apply(funObj) ?: getFunId(funObj) ?: return null
        return funId to subId
    }

    private suspend fun getFunId(firstFun: JsonObject): Int? {
        val funName = firstFun.getString("function_name")
        val path = firstFun.getJsonObject("file").getString("path")
        val funIds = db {
            select(Tables.FUNCTION.ID)
                .from(Tables.FUNCTION)
                .where(Tables.FUNCTION.NAME.like("${path}%${funName}%"))
                .fetch()
        }
        if (funIds.size != 1) {
            return null
        }
        return funIds.first().value1()
    }

    private fun intoHashCloneRecord(record: Record10<Int, Int, Int, Int, Int, Int, Int, Int, Int, Int>): HashClonesRecord {
        val hashClonesRecord = HashClonesRecord()
        hashClonesRecord.fFunctionid = record.value1()
        hashClonesRecord.fSubmissionid = record.value2()
        hashClonesRecord.fProjectid = record.value3()
        hashClonesRecord.fLeftbound = record.value4()
        hashClonesRecord.fRightbound = record.value5()
        hashClonesRecord.sFunctionid = record.value6()
        hashClonesRecord.sSubmissionid = record.value7()
        hashClonesRecord.sProjectid = record.value8()
        hashClonesRecord.sLeftbound = record.value9()
        hashClonesRecord.sRightbound = record.value10()
        return hashClonesRecord
    }

    private fun putClonesRecordsSegmentsIntoMap(
        segmentsMap: MutableMap<Pair<Int, Int>, MutableList<Pair<Int, Int>>>,
        record: HashClonesRecord
    ) {
        val recordFunctionSegment = record.fLeftbound to record.fRightbound
        val otherFunctionSegment = record.sLeftbound to record.sRightbound

        segmentsMap.computeIfAbsent(recordFunctionSegment) { mutableListOf() }.add(otherFunctionSegment)
    }

    private fun isCloneRecordsFromDifferentFunctions(
        firstRecord: HashClonesRecord,
        secondRecord: HashClonesRecord
    ): Boolean {
        return secondRecord.sSubmissionid != firstRecord.sSubmissionid ||
                secondRecord.sFunctionid != firstRecord.sFunctionid ||
                secondRecord.fSubmissionid != firstRecord.fSubmissionid ||
                secondRecord.fFunctionid != firstRecord.fFunctionid

    }

    /**
     * This method is intended to absorption by large ranges of smaller ranges
     *
     * @param segments segments where bigger ranges absorb smaller ranges, it must not contain pairs like 'pairA'
     * and 'pairB' where pairA.second==pairB.first==true, because there is no guarantee what result it will produce.
     * @return
     */
    fun absorbingSegments(segments: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
        val res: MutableList<Pair<Int, Int>> = mutableListOf()
        val segmentPoints: MutableList<SegmentPoint> = mutableListOf()
        for (segment in segments) {
            segmentPoints.add(SegmentPoint(segment.first, false))
            segmentPoints.add(SegmentPoint(segment.second, true))
        }
        segmentPoints.sortWith(Comparator.comparingInt(SegmentPoint::value))

        var segmentLength = 0
        var counter = 0
        for (i in segmentPoints.indices) {
            if (counter != 0) {
                segmentLength += (segmentPoints[i].value - segmentPoints[i - 1].value)
            }
            if (segmentPoints[i].isRightBound) {
                counter--
                if (counter < 0) {
                    throw IllegalStateException("Counter is $counter for segments = $segments")
                }
                if (counter == 0) {
                    val x = segmentPoints[i].value
                    res.add(Pair(x - segmentLength, x))
                    segmentLength = 0
                }
                continue
            }
            counter++
        }
        return res
    }
}

private data class SegmentPoint(val value: Int, val isRightBound: Boolean)