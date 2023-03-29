package org.jetbrains.research.kotoed.db

import io.vertx.core.json.JsonArray
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.ProcessedProjectSubRecord
import org.jetbrains.research.kotoed.util.AutoDeployable

@AutoDeployable
class ProcessedProjectSubVerticle : CrudDatabaseVerticleWithReferences<ProcessedProjectSubRecord>(Tables.PROCESSED_PROJECT_SUB){

}
