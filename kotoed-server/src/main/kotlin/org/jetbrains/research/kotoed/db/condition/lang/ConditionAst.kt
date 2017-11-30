package org.jetbrains.research.kotoed.db.condition.lang

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

// TODO Inclusion operators are now a special case.
// TODO maybe we should make PrimitiveSubquery a primitive and allow in everywhere sometimes

enum class InclusionOp(val rep: String) {
    IN("in"),
    NOT_IN("!in")
}

// TODO should we allow Path here?
data class PrimitiveSubquery(val value: List<Constant>): Expression()

data class InclusionExpression(val op: InclusionOp, val lhv: Expression, val rhv: Expression): Expression()

object ExpressionParsers {
    val path = identifier().joinedBy(constant(".")).map(::Path)
    val jsonIndex = identifier().map(::Variant1) or integer().map(::Variant0)
    val jsonPath = path.zip((constant("->").ignore() + jsonIndex).many()).map { JsonPath(it.first, it.second) }

    val const = integer().map(::IntConstant) or
            doubleQuotedString().map(::StringConstant) or
            constant("null").map { NullConstant }

    val cmpOperators = CompareOp.values().map { op ->
        binaryOperator<Expression>(lexeme(op.rep)){ l, r -> CompareExpression(op, l, r) }
    }

    val binaryOperators = BinaryOp.values().map {  op ->
        binaryOperator<Expression>(lexeme(op.rep)){ l, r -> BinaryExpression(op, l, r) }
    }

    val inclusionOperators = InclusionOp.values().map {  op ->
        binaryOperator<Expression>(lexeme(op.rep)){ l, r -> InclusionExpression(op, l, r) }
    }

    val primitiveSubquery =
            ((const.between(spaces(), spaces())
                    .joinedBy(constant(","))) or spaces().map { listOf<Constant>() })
                    .between(lexeme("["), lexeme("]"))
                    .between(spaces(), spaces())
                    .map(::PrimitiveSubquery)

    val root = recursive<Expression> { binop ->
        val primitive =
                binop.between(lexeme("("), lexeme(")")) or
                        const.between(spaces(), spaces()) or
                        jsonPath.between(spaces(), spaces())

        val cmp = operators<Expression> {
            cmpOperators.forEach { infixl(it) }
            inclusionOperators.forEach{ infixl(it) }
        }.build(primitive or primitiveSubquery) // TODO primitiveSubquery should only be allowed on RHS

        operators<Expression> {
            prefix(unaryOperator(lexeme("!"), ::NotExpression))
            binaryOperators.forEach { infixl(it) }
        }.build(cmp)
    }
}
