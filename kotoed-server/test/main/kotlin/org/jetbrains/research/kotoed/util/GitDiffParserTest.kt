package org.jetbrains.research.kotoed.util

import org.jetbrains.research.kotoed.code.diff.parseGitDiff
import org.junit.Test

class GitDiffParserTest {
    @Test
    fun tryout() {
        // this is not really a test, it just asserts that this does not crash

        // acquired from hg --git diff
        val mercurialDiff = parseGitDiff(
                """
diff --git a/kotoed-server/pom.xml b/kotoed-server/pom.xml
--- a/kotoed-server/pom.xml
+++ b/kotoed-server/pom.xml
@@ -276,6 +276,12 @@
             <version>${'$'}{icu.version}</version>
         </dependency>

+        <dependency>
+            <groupId>com.sksamuel.diff</groupId>
+            <artifactId>diff</artifactId>
+            <version>1.1.11</version>
+        </dependency>
+
     </dependencies>

     <build>
diff --git a/kotoed-server/src/main/kotlin/org/jetbrains/research/kotoed/code/diff/DiffModelEx.kt b/kotoed-server/src/main/kotlin/org/jetbrains/research/kotoed/code/diff/DiffModelEx.kt
--- a/kotoed-server/src/main/kotlin/org/jetbrains/research/kotoed/code/diff/DiffModelEx.kt
+++ b/kotoed-server/src/main/kotlin/org/jetbrains/research/kotoed/code/diff/DiffModelEx.kt
@@ -25,3 +25,19 @@
         },
         "changes" to hunks.map { it.toJson() }
 )
+
+data class OurHunk(val fromLine: Int, val fromCount: Int, val toLine: Int, val toCount: Int)
diff --git a/kotoed-server/pom.xml b/kotoed-server/pom.xml
--- a/kotoed-server/pom.xml
+++ b/kotoed-server/pom.xml
@@ -276,6 +276,12 @@ asdasdsadsadsadsadas
             <version>${'$'}{icu.version}</version>
         </dependency>

+        <dependency>
+            <groupId>com.sksamuel.diff</groupId>
+            <artifactId>diff</artifactId>
+            <version>1.1.11</version>
+        </dependency>
+
     </dependencies>

     <build>
diff --git a/kotoed-server/src/main/kotlin/org/jetbrains/research/kotoed/code/diff/DiffModelEx.kt b/kotoed-server/src/main/kotlin/org/jetbrains/research/kotoed/code/diff/DiffModelEx.kt
--- a/kotoed-server/src/main/kotlin/org/jetbrains/research/kotoed/code/diff/DiffModelEx.kt
+++ b/kotoed-server/src/main/kotlin/org/jetbrains/research/kotoed/code/diff/DiffModelEx.kt
@@ -25,3 +25,19 @@
         },
         "changes" to hunks.map { it.toJson() }
 )
+
+data class OurHunk(val fromLine: Int, val fromCount: Int, val toLine: Int, val toCount: Int)
+data class OurDiff(val fromFile: String, val toFile: String, val hunks: List<Hunk>)
+
+fun parseDiff(lines: Sequence<String>): List<Diff> {
+    val diffs = mutableListOf<Diff>()
+    fun currentDiff() = diffs.last()
+    fun currentHunk() = currentDiff().latestHunk
+
+    for(line in lines) {
+        if(line.startsWith("---")) {
+            currentDiff().fromFileName = line.removePrefix("---").trim()
+        }
+    }
+
+}
diff --git a/kotoed-server/src/main/kotlin/org/jetbrains/research/kotoed/util/Util.kt b/kotoed-server/src/main/kotlin/org/jetbrains/research/kotoed/util/Util.kt
--- a/kotoed-server/src/main/kotlin/org/jetbrains/research/kotoed/util/Util.kt
+++ b/kotoed-server/src/main/kotlin/org/jetbrains/research/kotoed/util/Util.kt
@@ -176,3 +176,17 @@
 fun String.truncateAt(index: Int) =
         if(index < length) take(index - 3) + "..."
         else this
+
+data class ComputableMap<K, V>(
+        val storage: MutableMap<K, V> = hashMapOf(),
+        val compute: (K) -> V
+): Map<K, V> by storage {
+    override fun get(key: K): V? = when {
+        key in storage -> storage[key]
+        else -> compute(key).also{ storage[key] = it }
""".lineSequence()
        )


        // acqured from git diff
        val gitDiff = parseGitDiff("""
diff --git a/src/lesson4/task1/List.kt b/src/lesson4/task1/List.kt
index c7dee32..b9163b9 100644
--- a/src/lesson4/task1/List.kt
+++ b/src/lesson4/task1/List.kt
@@ -118,6 +118,8 @@ fun mean(list: List<Double>): Double = TODO()
  *
  * Центрировать заданный список list, уменьшив каждый элемент на среднее арифметическое всех элементов.
  * Если список пуст, не делать ничего. Вернуть изменённый список.
+ *
+ * Обратите внимание, что данная функция должна изменять содержание списка list, а не его копии.
  */
 fun center(list: MutableList<Double>): MutableList<Double> = TODO()

@@ -147,6 +149,8 @@ fun polynom(p: List<Double>, x: Double): Double = TODO()
  * суммой данного элемента и всех предыдущих.
  * Например: 1, 2, 3, 4 -> 1, 3, 6, 10.
  * Пустой список не следует изменять. Вернуть изменённый список.
+ *
+ * Обратите внимание, что данная функция должна изменять содержание списка list, а не его копии.
  */
 fun accumulate(list: MutableList<Double>): MutableList<Double> = TODO()

diff --git a/src/lesson8/task1/Files.kt b/src/lesson8/task1/Files.kt
index fdaba4d..234abdf 100644
--- a/src/lesson8/task1/Files.kt
+++ b/src/lesson8/task1/Files.kt
@@ -110,13 +110,18 @@ fun centerFile(inputName: String, outputName: String) {
  * 1) Каждая строка входного и выходного файла не должна начинаться или заканчиваться пробелом.
  * 2) Пустые строки или строки из пробелов трансформируются в пустые строки без пробелов.
  * 3) Строки из одного слова выводятся без пробелов.
- * 3) Число строк в выходном файле должно быть равно числу строк во входном (в т. ч. пустых).
+ * 4) Число строк в выходном файле должно быть равно числу строк во входном (в т. ч. пустых).
  *
  * Равномерность определяется следующими формальными правилами:
- * 1) Число пробелов между каждыми двумя парами соседних слов не должно отличаться более, чем на 1.
- * 2) Число пробелов между более левой парой соседних слов должно быть больше или равно числу пробелов
+ * 5) Число пробелов между каждыми двумя парами соседних слов не должно отличаться более, чем на 1.
+ * 6) Число пробелов между более левой парой соседних слов должно быть больше или равно числу пробелов
  *    между более правой парой соседних слов.
  *
+ * Следует учесть, что входной файл может содержать последовательности из нескольких пробелов  между слвоами. Такие
+ * последовательности следует учитывать при выравнивании и при необходимости избавляться от лишних пробелов.
+ * Из этого следуют следующие правила:
+ * 7) В самой длинной строке каждая пара соседних слов должна быть отделена В ТОЧНОСТИ одним пробелом
+ * 8) Если входной файл удовлетворяет требованиям 1-7, то он должен быть в точности идентичен выходному файлу
  */
 fun alignFileByWidth(inputName: String, outputName: String) {
     TODO()
diff --git a/test/lesson4/task1/Tests.kt b/test/lesson4/task1/Tests.kt
index d1ed767..e120f7c 100644
--- a/test/lesson4/task1/Tests.kt
+++ b/test/lesson4/task1/Tests.kt
@@ -9,8 +9,8 @@ class Tests {
     @Tag("Example")
     fun sqRoots() {
         assertEquals(listOf<Double>(), sqRoots(-1.0))
-        assertEquals(listOf(0.0), sqRoots(0.0))
-        assertEquals(listOf(-5.0, 5.0), sqRoots(25.0))
+        assertArrayEquals(listOf(0.0).toDoubleArray(), sqRoots(0.0).toDoubleArray(), 1e-5)
+        assertArrayEquals(listOf(-5.0, 5.0).toDoubleArray(), sqRoots(25.0).toDoubleArray(), 1e-5)
     }

     @Test
@@ -18,11 +18,20 @@ class Tests {
     fun biRoots() {
         assertEquals(listOf<Double>(), biRoots(0.0, 0.0, 1.0))
         assertEquals(listOf<Double>(), biRoots(0.0, 1.0, 2.0))
-        assertEquals(listOf(-2.0, 2.0), biRoots(0.0, 1.0, -4.0))
+        assertArrayEquals(
+                listOf(-2.0, 2.0).toDoubleArray(),
+                biRoots(0.0, 1.0, -4.0).toDoubleArray(),
+                1e-5)
         assertEquals(listOf<Double>(), biRoots(1.0, -2.0, 4.0))
-        assertEquals(listOf(-1.0, 1.0), biRoots(1.0, -2.0, 1.0))
+        assertArrayEquals(
+                listOf(-1.0, 1.0).toDoubleArray(),
+                biRoots(1.0, -2.0, 1.0).toDoubleArray(),
+                1e-5)
         assertEquals(listOf<Double>(), biRoots(1.0, 3.0, 2.0))
-        assertEquals(listOf(-2.0, -1.0, 1.0, 2.0), biRoots(1.0, -5.0, 4.0).sorted())
+        assertArrayEquals(
+                listOf(-2.0, -1.0, 1.0, 2.0).toDoubleArray(),
+                biRoots(1.0, -5.0, 4.0).sorted().toDoubleArray(),
+                1e-5)
     }

     @Test
@@ -87,9 +96,20 @@ class Tests {
     @Tag("Normal")
     fun center() {
         assertEquals(listOf<Double>(), center(mutableListOf()))
-        assertEquals(listOf(0.0), center(mutableListOf(3.14)))
-        assertEquals(listOf(1.0, -1.0, 0.0), center(mutableListOf(3.0, 1.0, 2.0)))
-        assertEquals(listOf(-3.0, -1.0, 4.0, 5.0, -5.0), center(mutableListOf(0.0, 2.0, 7.0, 8.0, -2.0)))
+        assertArrayEquals(
+                listOf(0.0).toDoubleArray(),
+                center(mutableListOf(3.14)).toDoubleArray(),
+                1e-5)
+        assertArrayEquals(
+                listOf(1.0, -1.0, 0.0).toDoubleArray(),
+                center(mutableListOf(3.0, 1.0, 2.0)).toDoubleArray(),
+                1e-5)
+        assertArrayEquals(
+                listOf(-3.0, -1.0, 4.0, 5.0, -5.0).toDoubleArray(),
+                center(mutableListOf(0.0, 2.0, 7.0, 8.0, -2.0)).toDoubleArray(),
+                1e-5)
+        val toMutate = mutableListOf(-3.0, -1.0, 4.0, 5.0, -5.0)
+        assertTrue(toMutate === center(toMutate)) { "You should mutate an input list, not create a copy" }
     }

     @Test
@@ -113,9 +133,17 @@ class Tests {
     @Test
     @Tag("Normal")
     fun accumulate() {
-        assertEquals(listOf<Double>(), accumulate(mutableListOf()))
-        assertEquals(listOf(3.14), accumulate(mutableListOf(3.14)))
-        assertEquals(listOf(1.0, 3.0, 6.0, 10.0), accumulate(mutableListOf(1.0, 2.0, 3.0, 4.0)))
+        assertEquals(listOf<Double>(), accumulate(arrayListOf()))
+        assertArrayEquals(
+                listOf(3.14).toDoubleArray(),
+                accumulate(arrayListOf(3.14)).toDoubleArray(),
+                1e-5)
+        assertArrayEquals(
+                listOf(1.0, 3.0, 6.0, 10.0).toDoubleArray(),
+                accumulate(arrayListOf(1.0, 2.0, 3.0, 4.0)).toDoubleArray(),
+                1e-5)
+        val toMutate = mutableListOf(-3.0, -1.0, 4.0, 5.0, -5.0)
+        assertTrue(toMutate === accumulate(toMutate)) { "You should mutate an input list, not create a copy" }
     }

     @Test
""".lineSequence())

    }
}
