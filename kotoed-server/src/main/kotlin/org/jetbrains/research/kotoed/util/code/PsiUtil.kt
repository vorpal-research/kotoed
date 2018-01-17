package org.jetbrains.research.kotoed.util.code

import com.intellij.psi.PsiElement
import org.jetbrains.kootstrap.FooBarCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.research.kotoed.code.Filename
import org.jetbrains.research.kotoed.code.Location
import org.jetbrains.research.kotoed.util.contains

val KtFile.namedFunctions get() = collectDescendantsOfType<KtNamedFunction>()

val PsiElement.location: ClosedRange<Location>
    get() {
        val file = containingFile
        val document = file.viewProvider.document!!
        val fromLine = document.getLineNumber(startOffset)
        val toLine = document.getLineNumber(endOffset)
        val fromCol = startOffset - document.getLineStartOffset(fromLine)
        val toCol = endOffset - document.getLineStartOffset(toLine)

        val filename = Filename(path = file.name)
        return Location(filename, fromLine, fromCol)..Location(filename, toLine, toCol)
    }

fun Location.nextLine() = if(col == 0) this else copy(line = line + 1, col = 0)
fun Location.thisLine(): Location = copy(col = 0)
fun ClosedRange<Location>.alignToLines() =
        start.thisLine()..endInclusive.nextLine()

fun KtFile.blockByLocation(loc: Location): PsiElement {
    val blocks = collectDescendantsOfType<KtBlockExpression> { loc in it.location.alignToLines() }

    if(blocks.size == 1) return blocks.first()

    var innerMost: PsiElement? = null
    for(block in blocks) {
        if(innerMost == null
                || block.location in innerMost.location.alignToLines()) innerMost = block
    }
    if(innerMost != null) return innerMost

    innerMost = collectDescendantsOfType<KtNamedFunction> { loc in it.location.alignToLines() }.firstOrNull()
    if(innerMost != null) return innerMost

    innerMost = collectDescendantsOfType<KtClass> { loc in it.location.alignToLines() }.firstOrNull()
    if(innerMost != null) return innerMost

    return this
}

@Synchronized
inline fun<R> FooBarCompiler.useEnv(body: (KotlinCoreEnvironment) -> R): R {
    val env = this.setupMyEnv(org.jetbrains.kotlin.config.CompilerConfiguration())
    return try { body(env) } finally { tearDownMyEnv(env) }
}
