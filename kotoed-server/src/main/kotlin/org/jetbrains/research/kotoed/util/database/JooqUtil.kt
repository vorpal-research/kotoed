package org.jetbrains.research.kotoed.util.database

import org.jetbrains.research.kotoed.database.Public
import org.jetbrains.research.kotoed.database.tables.records.NotificationRecord
import org.jooq.Field
import org.jooq.Table

fun tableByName(name: String) =  Public.PUBLIC.tables.find { it.name == name }
val Table<*>.primaryKeyField get() =
    primaryKey?.fieldsArray?.firstOrNull()?.let{ field(it) } ?: field("id")

fun Table<*>.tableReferencedBy(f: Field<*>) =
        references.find { setOf(f.name) == it.fieldsArray.map { it.name }.toSet() }?.key?.table

fun NotificationRecord.fixTitle(): NotificationRecord =
        if (title != null) this else this.apply { title = type }
