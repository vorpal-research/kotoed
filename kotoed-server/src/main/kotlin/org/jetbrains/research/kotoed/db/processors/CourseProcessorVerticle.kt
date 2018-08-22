package org.jetbrains.research.kotoed.db.processors

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.api.VerificationData
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.CourseRecord
import org.jetbrains.research.kotoed.util.AutoDeployable

@AutoDeployable
class CourseProcessorVerticle : ProcessorVerticle<CourseRecord>(Tables.COURSE) {

    suspend override fun doProcess(data: JsonObject): VerificationData =
            verify(data)

    suspend override fun verify(data: JsonObject?): VerificationData {
        return VerificationData.Processed
    }
}
