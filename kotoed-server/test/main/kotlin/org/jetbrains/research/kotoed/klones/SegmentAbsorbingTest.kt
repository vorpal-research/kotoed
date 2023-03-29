package org.jetbrains.research.kotoed.klones

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.db.FunctionPartHashVerticle
import org.jetbrains.research.kotoed.util.tryToJson
import org.junit.Test
import kotlin.test.assertEquals

class SegmentAbsorbingTest {
    private val service = FunctionPartHashVerticle()

    @Test
    fun simpleTest() {
        val segment = 1 to 3
        val segments = listOf(segment)
        val unionSegments = service.absorbingSegments(segments)
        assertEquals(segments, unionSegments)
    }

    @Test
    fun nonIntersectingSegmentsTest() {
        val segment = 1 to 3
        val segment1 = 4 to 5
        val segment2 = 6 to 6
        val segment3 = 7 to 10
        val segment4 = 11 to 11
        val segments = listOf(segment, segment1, segment2, segment3, segment4).shuffled()
        val unionSegments = service.absorbingSegments(segments)
        assertEquals(segments.sortedBy(Pair<Int, Int>::first), unionSegments)
    }

    @Test
    fun absorbingTest() {
        val segment = 5 to 7
        val segment1 = 8 to 11
        val segment2 = 12 to 13
        val segment3 = 5 to 13

        val segments = listOf(segment, segment1, segment2, segment3).shuffled()
        val unionSegments = service.absorbingSegments(segments)
        assertEquals(listOf(segment3), unionSegments)
    }

    @Test
    fun intersectingWithAbsorbingTest() {
        val segment = 1 to 3
        val segment1 = 1 to 3
        val segment3 = 4 to 8
        val segment4 = 4 to 9
        val segment5 = 10 to 11
        val segment6 = 12 to 13
        val segment7 = 10 to 13
        val segments = listOf(segment, segment1, segment3,
            segment4, segment5, segment6, segment7)
        val unionSegments = service.absorbingSegments(segments)
        assertEquals(listOf(1 to 3, 4 to 9, 10 to 13), unionSegments)
    }
}