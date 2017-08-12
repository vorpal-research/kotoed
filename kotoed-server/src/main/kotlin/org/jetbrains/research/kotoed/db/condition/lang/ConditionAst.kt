package org.jetbrains.research.kotoed.db.condition.lang

sealed class Expression

enum class CompareOp(val rep: String) {
    EQ("=="),
    NE("!="),
    GT(">"),
    GE(">="),
    LT("<"),
    LE("<=")
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

object ExpressionParsers {
    val path = identifier().joinedBy(constant(".")).map(::Path)
    val const = integer().map(::IntConstant) or
            doubleQuotedString().map(::StringConstant) or
            constant("null").map { NullConstant }

    val cmpOperators = CompareOp.values().map { op ->
        binaryOperator<Expression>(lexeme(op.rep)){ l, r -> CompareExpression(op, l, r) }
    }

    val binaryOperators = BinaryOp.values().map {  op ->
        binaryOperator<Expression>(lexeme(op.rep)){ l, r -> BinaryExpression(op, l, r) }
    }

    val root = recursive<Expression> { binop ->
        val primitive =
                binop.between(lexeme("("), lexeme(")")) or
                        const.between(spaces(), spaces()) or
                        path.between(spaces(), spaces())

        val cmp = operators<Expression> {
            cmpOperators.forEach { infixl(it) }
        }.build(primitive)

        operators<Expression> {
            prefix(unaryOperator(lexeme("!"), ::NotExpression))
            binaryOperators.forEach { infixl(it) }
        }.build(cmp)
    }
}
