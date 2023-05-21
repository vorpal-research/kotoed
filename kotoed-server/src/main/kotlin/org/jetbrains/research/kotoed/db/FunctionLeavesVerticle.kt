package org.jetbrains.research.kotoed.db

import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.FunctionLeavesRecord
import org.jetbrains.research.kotoed.util.AutoDeployable

@AutoDeployable
class FunctionLeavesVerticle : CrudDatabaseVerticleWithReferences<FunctionLeavesRecord>(Tables.FUNCTION_LEAVES)