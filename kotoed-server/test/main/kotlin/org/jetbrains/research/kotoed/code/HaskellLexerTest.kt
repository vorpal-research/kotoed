package org.jetbrains.research.kotoed.code

import com.suhininalex.suffixtree.SuffixTree
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.intellij.lang.annotations.Language
import org.jetbrains.research.kotoed.code.klones.CloneClass
import org.jetbrains.research.kotoed.code.klones.KloneVerticle
import org.jetbrains.research.kotoed.code.klones.Token
import org.jetbrains.research.kotoed.code.klones.parentEdges
import org.jetbrains.research.kotoed.parsers.HaskellLexer
import org.jetbrains.research.kotoed.util.dfs
import org.jetbrains.research.kotoed.util.splitBy
import org.junit.Test
import java.util.concurrent.ThreadLocalRandom

class HaskellLexerTest {

    fun org.antlr.v4.runtime.Token.location() =
            org.jetbrains.research.kotoed.code.Location(
                    filename = Filename(path = "file.hs"), col = charPositionInLine, line = line
            )

    @Test
    fun amIInsane() {
        @Language("Haskell")
        val input = """
module Todo(todo) where

import Control.Exception

data NotImplementedException = NotImplementedException

instance Exception NotImplementedException where

instance Show NotImplementedException where
    show _ = "Not implemented yet"

todo :: a
todo = throw NotImplementedException

data TodoType = TodoType
        """.trimIndent()


        val tokenListList =
            CommonTokenStream(HaskellLexer(CharStreams.fromString(input, "File1.hs")))
                .apply { fill() }
                .tokens
                .asSequence()
                .splitBy { it.type == HaskellLexer.SPACES && it.text.contains(Regex("""\n.*\n""")) }
                .map { it.filter {
                    it.type !in setOf(
                        HaskellLexer.SPACES,
                        HaskellLexer.EOF,
                        HaskellLexer.LEFT_CURLY,
                        HaskellLexer.RIGHT_CURLY,
                        HaskellLexer.LEFT_PAREN,
                        HaskellLexer.RIGHT_PAREN,
                        HaskellLexer.SEMICOLON
                    )}
                }.map {
                    it.map {
                        Token(it.type,
                                ThreadLocalRandom.current().nextInt(),
                                ThreadLocalRandom.current().nextInt(),
                                HaskellLexer.VOCABULARY.getSymbolicName(it.type),
                                it.location(),
                                it.location().run { copy(col = col + it.text.length) },
                                ThreadLocalRandom.current().nextInt().toString()
                        )
                    }
                }.map {
                        listOf(it.first().copy(text = "BEGIN")) + it + listOf(it.last().copy(text = "END"))
                }

        tokenListList.forEach(::println)

        val sf = SuffixTree<Token>()

        tokenListList.forEach {
            sf.addSequence(it.map {it.copy(submissionId = 1, denizenId = 1) })
            sf.addSequence(it.map {it.copy(submissionId = 2, denizenId = 2) })
        }

        val clones =
                sf.root.dfs {
                    edges
                            .asSequence()
                            .mapNotNull { it.terminal }
                }.filter { node ->
                    node.edges
                            .asSequence()
                            .all { it.begin == it.end && it.begin == it.sequence.size - 1 }
                }.filter { node ->
                    0 == node.parentEdges.lastOrNull()?.begin
                }

        val filtered = clones
                .map(::CloneClass)
                .filter { cc -> cc.clones.isNotEmpty() }
                .filter { cc -> cc.clones.map { it.submissionId }.toSet().size != 1 }
                .toList()

        filtered.forEachIndexed { i, cloneClass ->
            val builder = StringBuilder()
            val fname = cloneClass.clones
                    .map { clone -> clone.functionName }
                    .distinct()
                    .joinToString()
            builder.appendLine("($fname) Clone class $i:")
            cloneClass.clones.forEach { c ->
                builder.appendLine("${c.submissionId}/${c.functionName}/${c.file.path}:${c.fromLine}:${c.toLine}")
            }
            builder.appendLine()
            println(builder)
        }

    }

    @Test
    fun testSimple() {
        @Language("Haskell")
        val input1 =
                """
module Task1_1 where

import Todo(todo)

data Operation = Plus | Minus | Mult deriving (Show, Eq)

data Term = IntConstant{ intValue :: Int }           -- числовая константа
            | Variable{ varName :: String }          -- переменная
            | BinaryTerm{ operation :: Operation, lhv :: Term, rhv :: Term } -- бинарная операция
            deriving(Show,Eq)

-- Для бинарных операций необходима не только реализация, но и адекватные
-- ассоциативность и приоритет
(|+|) :: Term -> Term -> Term
(|+|) l r = BinaryTerm Plus l r

(|-|) :: Term -> Term -> Term
(|-|) l r = BinaryTerm Minus l r

(|*|) :: Term -> Term -> Term
(|*|) l r = BinaryTerm Mult l r

infixl 6 |+|
infixl 6 |-|
infixl 7 |*|


-- Заменить переменную `varName` на `replacement`
-- во всём выражении `expression`
replaceVar :: String -> Term -> Term -> Term
replaceVar varName replacement expression =
  let replace hv = replaceVar varName replacement hv in
      case expression of
        Variable variable | variable == varName -> replacement
        BinaryTerm operartion lhv rhv -> BinaryTerm operartion (replace lhv) (replace rhv)
        _ -> expression

-- Посчитать значение выражения `Term`
-- если оно состоит только из констант
evaluate :: Term -> Term
evaluate expression = case expression of
  BinaryTerm operartion lhs rhs ->
      case (operartion, left, right) of
        (Plus, IntConstant left, IntConstant right) -> IntConstant (left + right)
        (Minus, IntConstant left, IntConstant right) -> IntConstant (left - right)
        (Mult, IntConstant left, IntConstant right) -> IntConstant (left * right)
        (Plus, IntConstant 0, right) -> right
        (Mult, IntConstant 1, right) -> right
        (Mult, IntConstant 0, right) -> IntConstant 0
        (Plus, left, IntConstant 0) -> left
        (Minus, left, IntConstant 0) -> left
        (Mult, left, IntConstant 0) -> IntConstant 0
        (Mult, left, IntConstant 1) -> left
        _ -> BinaryTerm operartion left right
        where
          left  = evaluate lhs
          right = evaluate rhs
  _ -> expression
"""

        val tokens1 = CommonTokenStream(HaskellLexer(CharStreams.fromString(input1, "File1.hs"))).apply{ fill() }.tokens


        println(tokens1
                .filter { it.type !in setOf(HaskellLexer.SPACES, HaskellLexer.EOF, HaskellLexer.LEFT_CURLY, HaskellLexer.RIGHT_CURLY) }
                .map { HaskellLexer.VOCABULARY.getSymbolicName(it.type) }
                .joinToString("\n", ">>>", "<<<")
        )


        @Language("Haskell")
        val input2 =
                """
module Task1_1 where

import Todo(todo)

data Operation = Plus | Minus | Times deriving (Show, Eq)

data Term = IntConstant{ intValue :: Int }           -- числовая константа
            | Variable{ varName :: String }          -- переменная
            | BinaryTerm{ operation :: Operation, lhv :: Term, rhv :: Term } -- бинарная операция
            deriving(Show,Eq)

-- Для бинарных операций необходима не только реализация, но и адекватные
-- ассоциативность и приоритет
(|+|) :: Term -> Term -> Term
(|+|) l r = BinaryTerm Plus l r
infixl 6 |+|

(|-|) :: Term -> Term -> Term
(|-|) l r = BinaryTerm Minus l r
infixl 6 |-|

(|*|) :: Term -> Term -> Term
(|*|) l r = BinaryTerm Times l r
infixl 7 |*|

-- Заменить переменную `varName` на `replacement`
-- во всём выражении `expression`
replaceVar :: String -> Term -> Term -> Term
replaceVar varName replacement expression =
    let replace hv = replaceVar varName replacement hv in
        case expression of
            Variable variable | variable == varName -> replacement
            BinaryTerm operartion lhv rhv -> BinaryTerm operartion (replace lhv) (replace rhv)
            _ -> expression

-- Посчитать значение выражения `Term`
-- если оно состоит только из констант
evaluate :: Term -> Term
evaluate expression = case expression of
    BinaryTerm operartion lhs rhs ->
        case (operartion, left, right) of
            (Plus, IntConstant left, IntConstant right) -> IntConstant (left + right)
            (Minus, IntConstant left, IntConstant right) -> IntConstant (left - right)
            (Times, IntConstant left, IntConstant right) -> IntConstant (left * right)
            (Plus, IntConstant 0, right) -> right
            (Times, IntConstant 1, right) -> right
            (Times, IntConstant 0, right) -> IntConstant 0
            (Plus, left, IntConstant 0) -> left
            (Minus, left, IntConstant 0) -> left
            (Times, left, IntConstant 0) -> IntConstant 0
            (Times, left, IntConstant 1) -> left
            _ -> BinaryTerm operartion left right
        where
            left  = evaluate lhs
            right = evaluate rhs
    _ -> expression
"""

        val tokens2 = CommonTokenStream(HaskellLexer(CharStreams.fromString(input2, "File2.hs"))).apply{ fill() }.tokens

        println(tokens2
                .filter { it.type !in setOf(HaskellLexer.SPACES, HaskellLexer.EOF, HaskellLexer.LEFT_CURLY, HaskellLexer.RIGHT_CURLY) }
                .map { HaskellLexer.VOCABULARY.getSymbolicName(it.type) }
                .joinToString("\n", ">>>", "<<<")
        )

    }

}