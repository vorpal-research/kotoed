package org.jetbrains.research.kotoed.db

import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.FunctionLeavesRecord
import org.jetbrains.research.kotoed.database.tables.records.HashClonesRecord
import org.jetbrains.research.kotoed.util.AutoDeployable

@AutoDeployable
class HashClonesVerticle : CrudDatabaseVerticleWithReferences<HashClonesRecord>(Tables.HASH_CLONES)