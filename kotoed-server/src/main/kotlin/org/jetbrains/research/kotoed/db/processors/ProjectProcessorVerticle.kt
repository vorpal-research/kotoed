package org.jetbrains.research.kotoed.db.processors

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.api.VerificationData
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.util.AutoDeployable

@AutoDeployable
class ProjectProcessorVerticle : ProcessorVerticle<ProjectRecord>(Tables.PROJECT) {

    suspend override fun verify(data: JsonObject?): VerificationData {
        return VerificationData.Processed
    }

    suspend override fun doProcess(data: JsonObject): VerificationData {
        return VerificationData.Processed
    }

}
