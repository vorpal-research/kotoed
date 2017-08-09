package org.jetbrains.research.kotoed.db

import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.util.AutoDeployable

@AutoDeployable
class SubmissionVerticle : CrudDatabaseVerticleWithReferences<SubmissionRecord>(Tables.SUBMISSION) {
    override suspend fun handleDelete(message: SubmissionRecord) =
            throw IllegalArgumentException("Submissions are not deletable")
}
