package org.jetbrains.research.kotoed.db.condition.lang

import org.jetbrains.research.kotoed.util.uncheckedCast
import ru.spbstu.kparsec.Parser
import ru.spbstu.kparsec.parsers.*
import ru.spbstu.kparsec.parsers.Literals.lexeme
import ru.spbstu.ktuples.EitherOf2
import ru.spbstu.ktuples.Variant0
import ru.spbstu.ktuples.Variant1

sealed class Expression

enum class CompareOp(val rep: String) {
    EQ("=="),
    NE("!="),
    GT(">"),
    GE(">="),
    LT("<"),
    LE("<="),
    MATCH("matches")
}

data class CompareExpression(val op: CompareOp, val lhv: Expression, val rhv: Expression) : Expression()

enum class BinaryOp(val rep: String) {
    AND("and"),
    OR("or")
}

data class BinaryExpression(val op: BinaryOp, val lhv: Expression, val rhv: Expression) : Expression()

data class NotExpression(val rhv: Expression): Expression()

sealed class Constant : Expression() {
    abstract val value: Any?
}

data class IntConstant(override val value: Int) : Constant()

data class StringConstant(override val value: String) : Constant()

object NullConstant : Constant() {
    override val value: Nothing? = null
    override fun toString() = "NullConstant"
}

data class Path(val path: List<String>) : Expression()
data class JsonPath(val base: Path, val path: List<EitherOf2<Int, String>>) : Expression()

enum class Sorting{ ASC, DESC }
data class SortCriterion(val path: JsonPath, val sorting: Sorting)

// TODO Inclusion operators are now a special case.
// TODO maybe we should make PrimitiveSubquery a primitive and allow in everywhere sometimes

enum class InclusionOp(val rep: String) {
    IN("in"),
    NOT_IN("!in")
}

// TODO should we allow Path here?
data class PrimitiveSubquery(val value: List<Constant>): Expression()

data class InclusionExpression(val op: InclusionOp, val lhv: Expression, val rhv: Expression): Expression()

object ExpressionParsers: StringsAsParsers {
    val identifier = regex("""[a-zA-Z_][a-zA-Z_0-9]*""".toRegex())
    val integer = Literals.CINTEGER.map { it.toInt() }
    val string = Literals.JSTRING

    val path = (identifier joinedBy -".").map(::Path)
    val jsonIndex = identifier.map(::Variant1) or integer.map(::Variant0)
    val jsonPath = zip(path, (-"->" + jsonIndex).many()).map { JsonPath(it.first, it.second) }

    val const = integer.map(::IntConstant) or
            string.map(::StringConstant) or
            (+"null").map { NullConstant }

    val primitive =
            (-"(" + defer{ expr } + -")") or
                    lexeme(const) or
                    lexeme(jsonPath)

    val primitiveSubquery =
            (-"[" + (const joinedBy -",").orElse(emptyList()) + -"]").map {
                lst -> PrimitiveSubquery(lst.uncheckedCast())
            }


    val expr: Parser<Char, Expression> = operatorTable(primitive or primitiveSubquery) {
        for (op in CompareOp.values()) {
            (-op.rep)(priority = 7) { a, b -> CompareExpression(op, a, b) }
        }

        for (op in InclusionOp.values()) {
            (-op.rep)(priority = 7) { a, b -> InclusionExpression(op, a, b) }
        }

        for (op in BinaryOp.values()) {
            (-op.rep)(priority = 5) { a, b -> BinaryExpression(op, a, b) }
        }

        (-"!")(assoc = Assoc.PREFIX, priority = 8){ a -> NotExpression(a) }
    }

    val sortCriterion = zip(+"-" or +"+" or +"", jsonPath) { sign, path ->
        when(sign) {
            "-" -> SortCriterion(path, Sorting.DESC)
            else -> SortCriterion(path, Sorting.ASC)
        }
    }

    val root = expr
}
