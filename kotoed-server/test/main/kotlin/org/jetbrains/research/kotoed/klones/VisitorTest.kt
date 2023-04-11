package org.jetbrains.research.kotoed.klones

import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import org.jetbrains.research.kotoed.util.TreeHashVisitor
import org.jetbrains.research.kotoed.util.VisitResult
import org.junit.Test
import java.util.function.Consumer
import javax.swing.Icon
import kotlin.test.assertEquals

class VisitorTest {
    private val treeHashVisitor = TreeHashVisitor()
    //           root
    //       0-5          6-7
    //    0-2   3-4  5;     6-6; 7;
    //   0,1,2;  3,4;           6;
    @Test
    fun visitTest() {
        val node0 = newPsiElement("node0")
        val node1 = newPsiElement("node1")
        val node2 = newPsiElement("node2")
        val node0_2 = newPsiElement(arrayOf(node0, node1, node2),"node0_2")
        val node3 = newPsiElement("node3")
        val node4 = newPsiElement("node4")
        val node3_4 = newPsiElement(arrayOf(node3, node4),"node3_4")
        val node5 = newPsiElement("node5")
        val node0_5 = newPsiElement(arrayOf(node0_2, node3_4, node5),"node0_5")
        val node6 = newPsiElement("node6")
        val node6_6 = newPsiElement(arrayOf(node6),"node6_6")
        val node7 = newPsiElement("node7")
        val node6_7 = newPsiElement(arrayOf(node6_6, node7), "node6_7")
        val root = newPsiElement(arrayOf(node0_5, node6_7), "root")
        val results = arrayListOf<VisitResult>()
        val consumers = listOf<Consumer<VisitResult>>(Consumer {
            results.add(it)
        })
        treeHashVisitor.visitTree(root, consumers)
        var result = results[0]
        assertEquals(0, result.leftBound)
        assertEquals(2, result.rightBound)
        assertEquals(3, result.leafNum)
        result = results[1]
        assertEquals(3, result.leftBound)
        assertEquals(4, result.rightBound)
        assertEquals(5, result.leafNum)
        result = results[2]
        assertEquals(0, result.leftBound)
        assertEquals(5, result.rightBound)
        assertEquals(6, result.leafNum)
        result = results[3]
        assertEquals(6, result.leftBound)
        assertEquals(7, result.rightBound)
        assertEquals(8, result.leafNum)
        result = results[4]
        assertEquals(0, result.leftBound)
        assertEquals(7, result.rightBound)
        assertEquals(8, result.leafNum)
    }
}

private fun newPsiElement(children: Array<PsiElement>, name: String): PsiElement {
    return psiElement(children, name)
}

private fun newPsiElement(name: String): PsiElement {
    return psiElement(emptyArray(), name)
}

private fun psiElement(children: Array<PsiElement>, name: String): PsiElement {

    return object : PsiElement {

        override fun getChildren(): Array<PsiElement> {
            return children
        }

        override fun getIcon(p0: Int): Icon {
            TODO("Not yet implemented")
        }

        override fun <T : Any?> getUserData(p0: Key<T>): T? {
            TODO("Not yet implemented")
        }

        override fun <T : Any?> putUserData(p0: Key<T>, p1: T?) {
            TODO("Not yet implemented")
        }

        override fun getProject(): Project {
            TODO("Not yet implemented")
        }

        override fun getLanguage(): Language {
            TODO("Not yet implemented")
        }

        override fun getManager(): PsiManager {
            TODO("Not yet implemented")
        }

        override fun getParent(): PsiElement {
            TODO("Not yet implemented")
        }

        override fun getFirstChild(): PsiElement {
            TODO("Not yet implemented")
        }

        override fun getLastChild(): PsiElement {
            TODO("Not yet implemented")
        }

        override fun getNextSibling(): PsiElement {
            TODO("Not yet implemented")
        }

        override fun getPrevSibling(): PsiElement {
            TODO("Not yet implemented")
        }

        override fun getContainingFile(): PsiFile {
            TODO("Not yet implemented")
        }

        override fun getTextRange(): TextRange {
            TODO("Not yet implemented")
        }

        override fun getStartOffsetInParent(): Int {
            TODO("Not yet implemented")
        }

        override fun getTextLength(): Int {
            TODO("Not yet implemented")
        }

        override fun findElementAt(p0: Int): PsiElement? {
            TODO("Not yet implemented")
        }

        override fun findReferenceAt(p0: Int): PsiReference? {
            TODO("Not yet implemented")
        }

        override fun getTextOffset(): Int {
            TODO("Not yet implemented")
        }

        override fun getText(): String {
            TODO("Not yet implemented")
        }

        override fun textToCharArray(): CharArray {
            TODO("Not yet implemented")
        }

        override fun getNavigationElement(): PsiElement {
            TODO("Not yet implemented")
        }

        override fun getOriginalElement(): PsiElement {
            TODO("Not yet implemented")
        }

        override fun textMatches(p0: CharSequence): Boolean {
            TODO("Not yet implemented")
        }

        override fun textMatches(p0: PsiElement): Boolean {
            TODO("Not yet implemented")
        }

        override fun textContains(p0: Char): Boolean {
            TODO("Not yet implemented")
        }

        override fun accept(p0: PsiElementVisitor) {
            TODO("Not yet implemented")
        }

        override fun acceptChildren(p0: PsiElementVisitor) {
            TODO("Not yet implemented")
        }

        override fun copy(): PsiElement {
            TODO("Not yet implemented")
        }

        override fun add(p0: PsiElement): PsiElement {
            TODO("Not yet implemented")
        }

        override fun addBefore(p0: PsiElement, p1: PsiElement?): PsiElement {
            TODO("Not yet implemented")
        }

        override fun addAfter(p0: PsiElement, p1: PsiElement?): PsiElement {
            TODO("Not yet implemented")
        }

        override fun checkAdd(p0: PsiElement) {
            TODO("Not yet implemented")
        }

        override fun addRange(p0: PsiElement?, p1: PsiElement?): PsiElement {
            TODO("Not yet implemented")
        }

        override fun addRangeBefore(p0: PsiElement, p1: PsiElement, p2: PsiElement?): PsiElement {
            TODO("Not yet implemented")
        }

        override fun addRangeAfter(p0: PsiElement?, p1: PsiElement?, p2: PsiElement?): PsiElement {
            TODO("Not yet implemented")
        }

        override fun delete() {
            TODO("Not yet implemented")
        }

        override fun checkDelete() {
            TODO("Not yet implemented")
        }

        override fun deleteChildRange(p0: PsiElement?, p1: PsiElement?) {
            TODO("Not yet implemented")
        }

        override fun replace(p0: PsiElement): PsiElement {
            TODO("Not yet implemented")
        }

        override fun isValid(): Boolean {
            TODO("Not yet implemented")
        }

        override fun isWritable(): Boolean {
            TODO("Not yet implemented")
        }

        override fun getReference(): PsiReference? {
            TODO("Not yet implemented")
        }

        override fun getReferences(): Array<PsiReference> {
            TODO("Not yet implemented")
        }

        override fun <T : Any?> getCopyableUserData(p0: Key<T>?): T? {
            TODO("Not yet implemented")
        }

        override fun <T : Any?> putCopyableUserData(p0: Key<T>?, p1: T?) {
            TODO("Not yet implemented")
        }

        override fun processDeclarations(
            p0: PsiScopeProcessor,
            p1: ResolveState,
            p2: PsiElement?,
            p3: PsiElement
        ): Boolean {
            TODO("Not yet implemented")
        }

        override fun getContext(): PsiElement? {
            TODO("Not yet implemented")
        }

        override fun isPhysical(): Boolean {
            TODO("Not yet implemented")
        }

        override fun getResolveScope(): GlobalSearchScope {
            TODO("Not yet implemented")
        }

        override fun getUseScope(): SearchScope {
            TODO("Not yet implemented")
        }

        override fun getNode(): ASTNode {
            TODO("Not yet implemented")
        }

        override fun isEquivalentTo(p0: PsiElement?): Boolean {
            TODO("Not yet implemented")
        }

        override fun toString(): String {
            return name
        }
    }
}