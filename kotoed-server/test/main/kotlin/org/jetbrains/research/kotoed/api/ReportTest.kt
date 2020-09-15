package org.jetbrains.research.kotoed.api

import org.jetbrains.research.kotoed.data.buildSystem.KotoedRunnerStatus
import org.jetbrains.research.kotoed.data.buildSystem.KotoedRunnerTestMethodRun
import org.jetbrains.research.kotoed.data.buildSystem.KotoedRunnerTestResult
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ReportTest {
    private val reportVerticle = ReportVerticle()

    @Test
    fun calcHighestGradesLessonsTest() {
        assertEquals(listOf(), reportVerticle.calcHighestGradeLessons(listOf()))
        assertEquals(listOf("lesson1" to 1), reportVerticle.calcHighestGradeLessons(listOf("lesson1" to 1)))
        assertEquals(listOf("lesson2" to 5, "lesson3" to 4, "lesson1" to 1),
                reportVerticle.calcHighestGradeLessons(listOf("lesson1" to 1, "lesson2" to 5, "lesson3" to 4)))
        assertEquals(listOf(
                "lesson2" to 5, "lesson3" to 4,
                "lesson4" to 4, "lesson5" to 3, "lesson6" to 2),
                reportVerticle.calcHighestGradeLessons(listOf(
                        "lesson1" to 1, "lesson2" to 5, "lesson3" to 4,
                        "lesson4" to 4, "lesson5" to 3, "lesson6" to 2)))
    }

    @Test
    fun calcHighestGradeTasksTest() {
        val res1 = KotoedRunnerTestMethodRun(
                tags = listOf("1"),
                results = listOf(KotoedRunnerTestResult(KotoedRunnerStatus.SUCCESSFUL, failure = null)),
                methodName = "task1",
                packageName = "lesson1.Tests")
        val res2 = res1.copy(methodName = "task2", tags = listOf("2"))
        assertEquals(listOf("task1" to 1), reportVerticle.calcHighestGradeTasks(mapOf("task1" to listOf(res1))))
        assertEquals(listOf("task2" to 2), reportVerticle.calcHighestGradeTasks(mapOf("task2" to listOf(res2))))
        assertEquals(listOf("task2" to 2, "task1" to 1),
                reportVerticle.calcHighestGradeTasks(mapOf("task1" to listOf(res1), "task2" to listOf(res2))))

        val res22 = res2.copy(tags = listOf())
        assertEquals(listOf("task2" to 2),
                reportVerticle.calcHighestGradeTasks(mapOf("task2" to listOf(res2, res22))))

        val res23 = res2.copy(tags = listOf("3"))
        assertFails {
            reportVerticle.calcHighestGradeTasks(mapOf("task2" to listOf(res2, res23)))
        }

        val res24 = res2.copy(packageName = "lesson1.RandomTests")
        assertEquals(listOf("task2" to 2),
                reportVerticle.calcHighestGradeTasks(mapOf("task2" to listOf(res2, res24))))

        val res25 = res24.copy(results = listOf(KotoedRunnerTestResult(KotoedRunnerStatus.FAILED, failure = null)))
        assertEquals(listOf(),
                reportVerticle.calcHighestGradeTasks(mapOf("task2" to listOf(res2, res25))))

        val res3 = res1.copy(methodName = "task3", tags = listOf("3"))
        val res4 = res1.copy(methodName = "task4", tags = listOf("4"))
        assertEquals(listOf("task4" to 4, "task3" to 3),
                reportVerticle.calcHighestGradeTasks(mapOf(
                        "task1" to listOf(res1), "task2" to listOf(res2),
                        "task3" to listOf(res3), "task4" to listOf(res4))))
    }
}