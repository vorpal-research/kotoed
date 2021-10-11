package org.jetbrains.research.kotoed.util.code

import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.research.kotoed.code.Filename
import org.jetbrains.research.kotoed.code.Location
import org.jetbrains.research.kotoed.util.Loggable
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

inline fun <R> Loggable.temporaryEnv(body: (KotlinCoreEnvironment) -> R): R {
    val disposable = Disposer.newDisposable()
    val env = KotlinCoreEnvironment.createForProduction(
            disposable,
            CompilerConfiguration().apply {
                  put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, object : MessageCollector {
                      override fun clear() {}
                      override fun hasErrors(): Boolean = false
                      override fun report(
                          severity: CompilerMessageSeverity,
                          message: String,
                          location: CompilerMessageSourceLocation?
                      ) {
                          log.info("Kotlin compiler message: [${severity.presentableName}]: $message")
                      }

                  })
            },
            EnvironmentConfigFiles.JVM_CONFIG_FILES
    )
    try {
        return body(env)
    } finally {
        disposable.dispose()
    }
}
