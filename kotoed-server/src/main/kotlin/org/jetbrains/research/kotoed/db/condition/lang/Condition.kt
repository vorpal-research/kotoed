package org.jetbrains.research.kotoed.db.condition.lang

import org.apache.commons.text.StringEscapeUtils
import org.jetbrains.research.kotoed.util.database.FunctionCall
import org.jetbrains.research.kotoed.util.database.documentMatch
import org.jetbrains.research.kotoed.util.database.jsonGet
import org.jetbrains.research.kotoed.util.database.toPlainTSQuery
import org.jetbrains.research.kotoed.util.uncheckedCast
import org.jooq.Condition
import org.jooq.Field
import org.jooq.SortField
import org.jooq.Table
import org.jooq.impl.DSL
import ru.spbstu.kparsec.NoSuccess
import ru.spbstu.kparsec.Success
import ru.spbstu.kparsec.parse
import ru.spbstu.ktuples.converge
import ru.spbstu.ktuples.map0
import ru.spbstu.ktuples.map1

private fun<T> convertConstant(e: Expression): Field<T> = when (e) {
    is IntConstant -> DSL.inline(e.value).uncheckedCast()
    is StringConstant -> DSL.inline(StringEscapeUtils.unescapeJava(e.value)).uncheckedCast()
    is NullConstant -> DSL.field("NULL").uncheckedCast()
    else -> error("convertConstant() is for constants only!")
}

private fun<T> convertPrimitive(e: Expression, tables: (String) -> Table<*>): Field<T> = when(e){
    is JsonPath -> {
        val base = e.base.path
        val field: Field<T> = tables(base.dropLast(1).joinToString(".")).field(base.last()).uncheckedCast()
        if(e.path.isEmpty()) field
        else e.path.fold(field.uncheckedCast<Field<Any>>()) { f, p ->
            p
                    .map0 { f.jsonGet(it) }
                    .map1 { f.jsonGet(it) }
                    .converge()
        }.uncheckedCast()
    }

    is Constant -> convertConstant(e)
    else -> error("convertPrimitive() is for primitives only!")
}

private fun<T> convertPrimitiveSubquery(e: Expression): Array<Field<T>> = when (e) {
    is PrimitiveSubquery -> e.value.map { convertConstant<T>(it) }.toTypedArray()
    else -> error("convertPrimitiveSubquery() is for primitive subqueries only!")
}

private fun<T> convertCompareExpression(cmp: CompareExpression, tables: (String) -> Table<*>) = when(cmp.op) {
    CompareOp.EQ -> convertPrimitive<T>(cmp.lhv, tables).eq(convertPrimitive<T>(cmp.rhv, tables))
    CompareOp.NE -> convertPrimitive<T>(cmp.lhv, tables).ne(convertPrimitive<T>(cmp.rhv, tables))
    CompareOp.GT -> convertPrimitive<T>(cmp.lhv, tables).gt(convertPrimitive<T>(cmp.rhv, tables))
    CompareOp.GE -> convertPrimitive<T>(cmp.lhv, tables).ge(convertPrimitive<T>(cmp.rhv, tables))
    CompareOp.LT -> convertPrimitive<T>(cmp.lhv, tables).lt(convertPrimitive<T>(cmp.rhv, tables))
    CompareOp.LE -> convertPrimitive<T>(cmp.lhv, tables).le(convertPrimitive<T>(cmp.rhv, tables))
    CompareOp.MATCH -> {
        var lhv = convertPrimitive<T>(cmp.lhv, tables).uncheckedCast<Field<Any>>()
        var rhv = convertPrimitive<T>(cmp.rhv, tables).uncheckedCast<Field<Any>>()
        if(lhv.dataType.typeName !in listOf("tsvector", "\"pg_catalog\".\"tsvector\"")) lhv = FunctionCall("to_tsvector", lhv)
        if(rhv.dataType.typeName !in listOf("tsquery", "\"pg_catalog\".\"tsquery\"")) rhv = toPlainTSQuery(rhv.uncheckedCast())
        DSL.condition(lhv documentMatch rhv)
    }
    CompareOp.CONTAINS -> convertPrimitive<T>(cmp.lhv, tables).contains(convertPrimitive<T>(cmp.rhv, tables))
    CompareOp.STARTS_WITH -> convertPrimitive<T>(cmp.lhv, tables).startsWith(convertPrimitive<T>(cmp.rhv, tables))
    CompareOp.ENDS_WITH -> convertPrimitive<T>(cmp.lhv, tables).endsWith(convertPrimitive<T>(cmp.rhv, tables))
    CompareOp.I_CONTAINS -> convertPrimitive<T>(cmp.lhv, tables).containsIgnoreCase(convertPrimitive<T>(cmp.rhv, tables))
//    CompareOp.I_STARTS_WITH -> convertPrimitive<T>(cmp.lhv, tables).startsWithIgnoreCase(convertPrimitive<T>(cmp.rhv, tables))
//    CompareOp.I_ENDS_WITH -> convertPrimitive<T>(cmp.lhv, tables).endsWithIgnoreCase(convertPrimitive<T>(cmp.rhv, tables))

}

private fun convertBinaryExpression(bin: BinaryExpression, tables: (String) -> Table<*>) = when(bin.op) {
    BinaryOp.AND -> convertAst(bin.lhv, tables).and(convertAst(bin.rhv, tables))
    BinaryOp.OR -> convertAst(bin.lhv, tables).or(convertAst(bin.rhv, tables))
}

private fun <T> convertInclusionExpression(incl: InclusionExpression, tables: (String) -> Table<*>) = when(incl.op) {
    InclusionOp.IN -> convertPrimitive<T>(incl.lhv, tables).`in`(*convertPrimitiveSubquery<T>(incl.rhv))
    InclusionOp.NOT_IN -> convertPrimitive<T>(incl.lhv, tables).notIn(*convertPrimitiveSubquery<T>(incl.rhv))
}

private fun convertAst(e: Expression, tables: (String) -> Table<*>): Condition = when(e) {
    is CompareExpression -> convertCompareExpression<Any>(e, tables)
    is NotExpression -> DSL.not(convertAst(e.rhv, tables))
    is BinaryExpression -> convertBinaryExpression(e, tables)
    is InclusionExpression -> convertInclusionExpression<Any>(e, tables)
    else -> DSL.condition(convertPrimitive<Any>(e, tables).uncheckedCast<Field<Boolean>>())
}

fun parseCondition(input: String, tables: (String) -> Table<*>): Condition {
    val e = ExpressionParsers.root.parse(input)
    when(e) {
        is NoSuccess -> throw IllegalArgumentException("Failed to parse expression \"$input\": " +
                "expected ${e.expected} at ${e.location}")
        is Success -> return convertAst(e.result, tables)
    }
}

fun parseSortCriterion(input: String, tables: (String) -> Table<*>): SortField<*> {
    val e = ExpressionParsers.sortCriterion.parse(input)
    when(e) {
        is NoSuccess -> throw IllegalArgumentException("Failed to parse expression \"$input\": " +
                "expected ${e.expected} at ${e.location}")
        is Success -> {
            val expr = convertPrimitive<Any?>(e.result.path, tables)
            return when(e.result.sorting) {
                Sorting.ASC -> expr.asc()
                Sorting.DESC -> expr.desc()
            }
        }
    }
}
