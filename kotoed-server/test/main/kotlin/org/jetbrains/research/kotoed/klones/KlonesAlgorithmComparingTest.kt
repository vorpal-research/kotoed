package org.jetbrains.research.kotoed.klones

import io.vertx.core.json.JsonArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.research.kotoed.database.tables.records.HashClonesRecord
//import org.jetbrains.research.kotoed.db.FunctionPartHashVerticle
import org.jetbrains.research.kotoed.util.Jsonable
import org.jetbrains.research.kotoed.util.tryToJson
import java.io.FileReader

class KlonesAlgorithmComparingTest {

//    private val service = FunctionPartHashVerticle()

    private val id = 1L
    private val projId = 1
    private val FIRST_FUN_ID = 1
    private val SECOND_FUN_ID = 2
    private val THIRD_FUN_ID = 3
    private val FIRST_SUB_ID = 1
    private val SECOND_SUB_ID = 2


    suspend fun comparingTest() {
        val newAlgoRecords = mutableListOf<HashClonesRecord>()
        addClonesBetween(newAlgoRecords, FIRST_FUN_ID, FIRST_SUB_ID, SECOND_FUN_ID, SECOND_SUB_ID, 2)
        addClonesBetween(newAlgoRecords, FIRST_FUN_ID, FIRST_SUB_ID, THIRD_FUN_ID, SECOND_SUB_ID, 2)

        addClonesBetween(newAlgoRecords, SECOND_FUN_ID, FIRST_SUB_ID, THIRD_FUN_ID, SECOND_SUB_ID, 5)
        addClonesBetween(newAlgoRecords, SECOND_FUN_ID, FIRST_SUB_ID, FIRST_FUN_ID, SECOND_SUB_ID, 1)

        val oldAlgoClones = JsonArray()
        createOldCloneBetween(oldAlgoClones, FIRST_FUN_ID, FIRST_SUB_ID, SECOND_FUN_ID, SECOND_SUB_ID, 1)
        createOldCloneBetween(oldAlgoClones, FIRST_FUN_ID, FIRST_SUB_ID, THIRD_FUN_ID, SECOND_SUB_ID, 3)

        createOldCloneBetween(oldAlgoClones, SECOND_FUN_ID, FIRST_SUB_ID, THIRD_FUN_ID, SECOND_SUB_ID, 5)

        createOldCloneBetween(oldAlgoClones, THIRD_FUN_ID, FIRST_SUB_ID, THIRD_FUN_ID, SECOND_SUB_ID, 5)

//        service.compareKlones(oldAlgoClones, newAlgoRecords, { t -> t.getInteger("fun_id") },"testFile", setOf(FIRST_SUB_ID))
    }

    private fun addClonesBetween(
        newAlgoRecords: MutableList<HashClonesRecord>,
        fFunId: Int,
        fSubId: Int,
        sFunId: Int,
        sSubId: Int,
        count: Int
    ) {
        var start = 0
        val length = 2
        for (i in 0 until count) {
            newAlgoRecords.add(
                HashClonesRecord(
                    id,
                    fFunId,
                    fSubId,
                    projId,
                    start,
                    start + length - 1,
                    sFunId,
                    sSubId,
                    projId,
                    start,
                    start + length - 1
                )
            )
            start += length
        }
    }

    private fun createOldCloneBetween(
        oldAlgoClones: JsonArray,
        fFunId: Int,
        fSubId: Int,
        sFunId: Int,
        sSubId: Int,
        count: Int
    ) {
        val res = JsonArray()
        res.add(CloneInfo(fSubId, fFunId).toJson())
        for (i in 0 until count) {
            res.add(CloneInfo(sSubId, sFunId).toJson())
        }
        oldAlgoClones.add(res)
    }

    data class CloneInfo(
        val submissionId: Int,
        val funId: Int
    ) : Jsonable


}

suspend fun main() {
    KlonesAlgorithmComparingTest().comparingTest()

}