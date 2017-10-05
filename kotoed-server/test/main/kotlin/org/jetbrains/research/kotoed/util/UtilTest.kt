package org.jetbrains.research.kotoed.util

import org.junit.Test
import kotlin.test.assertEquals

class UtilTest {

    @Test
    fun `test chunksBy()`() {
        assertEquals(listOf(), listOf<Int>().chunksBy{ it })
        assertEquals(listOf(1 to listOf(1,1,1),
                            2 to listOf(2,2),
                            3 to listOf(3),
                            4 to listOf(4,4),
                            2 to listOf(2,2),
                            1 to listOf(1,1)),
                listOf(1,1,1,2,2,3,4,4,2,2,1,1).chunksBy { it })

        assertEquals(listOf(1 to listOf(1)), listOf(1).chunksBy { it })
    }

}