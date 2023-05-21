package org.jetbrains.research.kotoed.db

import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.FunctionRecord
import org.jetbrains.research.kotoed.util.AutoDeployable

@AutoDeployable
class FunctionVerticle : CrudDatabaseVerticleWithReferences<FunctionRecord>(Tables.FUNCTION)  {

}