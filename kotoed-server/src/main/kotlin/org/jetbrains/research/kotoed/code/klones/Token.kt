package org.jetbrains.research.kotoed.code.klones

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.parser.KDocElementTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.research.kotoed.code.Filename
import org.jetbrains.research.kotoed.code.Location

fun makeLiteralKtToken(type: Any, submissionId: Int, denizenId: Int, origin: PsiElement) =
        makeToken(type, submissionId, denizenId, origin) { if (children.isEmpty()) text else "${node.elementType}" }

fun makeAnonimizedKtToken(type: Any, submissionId: Int, denizenId: Int, origin: PsiElement) =
        makeToken(type, submissionId, denizenId, origin) { "${node.elementType}" }

private inline fun makeToken(type: Any, submissionId: Int, denizenId: Int, origin: PsiElement, converter: PsiElement.() -> String): Token {
    val text = converter(origin)

    val file = origin.containingFile
    val document = file.viewProvider.document!!
    val fromLine = document.getLineNumber(origin.startOffset)
    val from = Location(
            filename = Filename(path = file.name),
            line = fromLine + 1,
            col = origin.startOffset - document.getLineStartOffset(fromLine)
    )
    val toLine = document.getLineNumber(origin.endOffset)
    val to = Location(
            filename = Filename(path = file.name),
            line = toLine + 1,
            col = origin.endOffset - document.getLineStartOffset(toLine)
    )
    val fname = origin.parentsWithSelf.filterIsInstance<KtNamedFunction>().first().name
    return Token(type, submissionId, denizenId, text, from, to, fname.orEmpty())
}

data class Token(
        val type: Any,
        val submissionId: Int,
        val denizenId: Int,
        val text: String,
        val from: Location,
        val to: Location,
        val functionName: String) : Comparable<Token> {
    override fun compareTo(other: Token): Int = text.compareTo(other.text)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Token) return false
        if (text != other.text) return false
        return true
    }

    override fun hashCode(): Int = text.hashCode()

    override fun toString(): String = "T($text)"

    companion object {
        private val filteredTokenTypes = TokenSet.orSet(
                KtTokens.WHITESPACES,
                KtTokens.COMMENTS,
                TokenSet.create(
                        KtTokens.LBRACE,
                        KtTokens.RBRACE,
                        KtTokens.LPAR,
                        KtTokens.RPAR
                ),
                KDocTokens.KDOC_HIGHLIGHT_TOKENS,
                TokenSet.create(
                        KDocElementTypes.KDOC_NAME,
                        KDocElementTypes.KDOC_SECTION,
                        KDocElementTypes.KDOC_TAG
                )
        )

        val DefaultFilter = { e: PsiElement ->
            e.node.elementType !in filteredTokenTypes
        }
    }

}
