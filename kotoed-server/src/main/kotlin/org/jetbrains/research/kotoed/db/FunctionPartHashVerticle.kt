package org.jetbrains.research.kotoed.db

import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.FunctionPartHashRecord
import org.jetbrains.research.kotoed.util.AutoDeployable

@AutoDeployable
class FunctionPartHashVerticle : CrudDatabaseVerticleWithReferences<FunctionPartHashRecord>(Tables.FUNCTION_PART_HASH){

}