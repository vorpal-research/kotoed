package org.jetbrains.research.kotoed.util

import io.vertx.core.json.JsonObject
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

data class ExampleMessage(val p1: Int, val p2: String, val p3: Int?) : Message()

data class ExampleMessage2(val payload: List<ExampleMessage>) : Message()

data class ExampleMessage3(val payload: Map<Int, ExampleMessage?>) : Message()

data class Tangled(val data: List<Int>, val next: Map<Int, List<Tangled>>) : Message()

class JsonUtilTest {
    @Test
    fun testToJson() {
        assertEquals(
                JsonObject("""
                {
                    "payload" : [
                        {
                            "p1": 2,
                            "p2": "Hello",
                            "p3": null
                        }
                    ]
                }
            """),
                ExampleMessage2(listOf(ExampleMessage(2, "Hello", null))).toJson()
        )

        assertEquals(
                JsonObject("""
                {
                    "payload" : [
                        [1, {
                            "p1": 2,
                            "p2": "Hello",
                            "p3": null
                        }]
                    ]
                }
            """),
                ExampleMessage3(mapOf(1 to ExampleMessage(2, "Hello", null))).toJson()
        )

        assertEquals(
                JsonObject("""
                    {
                        "data" : [1, 2, 3, 4],
                        "next" : [
                            [1, [ { "data": [], "next": [] } ] ],
                            [2, [] ]
                        ]
                    }
                """),
                Tangled(listOf(1, 2, 3, 4), mapOf(1 to listOf(Tangled(listOf(), mapOf())), 2 to listOf())).toJson()
        )
    }

    @Test
    fun testFromJson() {
        assertEquals(
                fromJson(JsonObject("""
                    {
                        "payload" : [
                            {
                                "p1": 2,
                                "p2": "Hello"
                            }
                        ]
                    }
                """)),
                ExampleMessage2(listOf(ExampleMessage(2, "Hello", null)))
        )

        assertEquals(
                fromJson(JsonObject("""
                    {
                        "payload" : [
                            {
                                "p1": 2,
                                "p2": "Hello",
                                "p3": null
                            }
                        ]
                    }
                """)),
                ExampleMessage2(listOf(ExampleMessage(2, "Hello", null)))
        )

        assertEquals(
                fromJson(JsonObject("""
                    {
                        "payload" : [
                            [1, {
                                "p1": 2,
                                "p2": "Hello",
                                "p3": null
                            }]
                        ]
                    }
                """)),
                ExampleMessage3(mapOf(1 to ExampleMessage(2, "Hello", null)))
        )

        assertFailsWith(IllegalArgumentException::class) {
            fromJson<ExampleMessage2>(JsonObject("""
                    {
                        "payload" : [
                            {
                                "p1": 2,
                                "p3": 43
                            }
                        ]
                    }
                """))
        }

        assertFailsWith(IllegalArgumentException::class) {
            fromJson<ExampleMessage2>(JsonObject("""
                    {
                        "payload" : null
                    }
                """))
        }

        assertFailsWith(IllegalArgumentException::class) {
            fromJson<ExampleMessage2>(JsonObject("""
                    {
                        "payload" : 40
                    }
                """))
        }

        assertFailsWith(IllegalArgumentException::class) {
            fromJson<ExampleMessage2>(JsonObject("""
                    {}
                """))
        }
    }
}
