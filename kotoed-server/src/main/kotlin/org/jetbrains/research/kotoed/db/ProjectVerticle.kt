package org.jetbrains.research.kotoed.db

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.AutoDeployable
import org.jetbrains.research.kotoed.util.JsonableEventBusConsumerForDynamic
import org.jetbrains.research.kotoed.util.database.FunctionCall
import org.jetbrains.research.kotoed.util.database.PostgresDataTypeEx
import org.jetbrains.research.kotoed.util.database.equal
import org.jetbrains.research.kotoed.util.database.toJson
import org.jetbrains.research.kotoed.util.expecting
import org.jooq.QueryPart

@AutoDeployable
class ProjectVerticle : CrudDatabaseVerticleWithReferences<ProjectRecord>(Tables.PROJECT) {

    override suspend fun handleRead(message: ProjectRecord): ProjectRecord {
        return super.handleRead(message).apply {
            reset("document")
            reset("empty")
        }
    }
    val fullAddress = Address.DB.full(table.name)

    @JsonableEventBusConsumerForDynamic(addressProperty = "fullAddress")
    suspend fun handleFull(query: ProjectRecord): JsonObject {
        val id = query.id
        val table = Tables.PROJECT

        val innerSubTable = Tables.SUBMISSION.`as`("project.submissions")
        val courseTable = Tables.COURSE.`as`("project.course")
        val ownerTable = Tables.DENIZEN.`as`("project.owner")

        fun to_json(p: QueryPart) = FunctionCall<Any>("to_jsonb", p).coerce(PostgresDataTypeEx.JSONB)
        fun array(p: QueryPart) = FunctionCall<Any>("array", p)

        val fields = table.fields().asList() - table.field("document")
        val overRecords = db {
            select(*fields.toTypedArray(),
                    *courseTable.fields(),
                    *ownerTable.fields(),
                    to_json(array(
                            select(to_json(innerSubTable))
                                    .from(innerSubTable)
                                    .where(innerSubTable.PROJECT_ID equal id)
                    )).`as`("submissions")
            )
                    .from(table)
                    .join(courseTable).onKey(table.COURSE_ID)
                    .join(ownerTable).onKey(table.DENIZEN_ID)
                    .where(table.ID equal id)
                    .fetch()
        }
        val overRecord = overRecords.expecting { it.size == 1 }.first()

        val project = overRecord.into(table)
        val owner = overRecord.into(ownerTable)
        val course = overRecord.into(courseTable)

        val ret = project.toJson().apply {
            put("owner", owner.toJson())
            put("course", course.toJson())
            put("submissions", overRecord["submissions"])
        }
        return ret
    }

}
