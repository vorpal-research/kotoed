package org.jetbrains.research.kotoed.util.database

import org.jetbrains.research.kotoed.database.Public
import org.jooq.Field
import org.jooq.Table

fun tableByName(name: String) =  Public.PUBLIC.tables.find { it.name == name }
val Table<*>.primaryKeyField get() = field(primaryKey.fieldsArray.first())
fun Table<*>.tableReferencedBy(f: Field<*>) =
        references.find { setOf(f.name) == it.fieldsArray.map { it.name }.toSet() }?.key?.table
