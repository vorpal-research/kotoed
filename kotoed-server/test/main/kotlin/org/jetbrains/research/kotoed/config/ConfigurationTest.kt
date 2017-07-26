package org.jetbrains.research.kotoed.config

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.util.JsonObject
import org.junit.Test
import kotlin.test.assertEquals

class BigConfig : Configuration() {
    val Splendidness: Int by 9001
    val Stupidity: Long by 0xDEADBEEF

    class SubConfig0 : Configuration() {
        val MaidenName: String by "Hertz"

        class PlayInfo : Configuration() {
            val Author: String by "Shakespeare"
            val Name: String by "Romeo and Juliet"
            val ScreenTitle by { "\"$Name\", by $Author" }
        }

        val Play by PlayInfo()
    }

    val Sub by SubConfig0()

}

class ConfigurationTest {
    @Test
    fun testSimple() {

        val default = JsonObject("""
                    {
                        "splendidness" : 9001,
                        "stupidity" : ${0xDEADBEEF},
                        "sub" : {
                            "maiden_name" : "Hertz",
                            "play" : {
                                "author" : "Shakespeare",
                                "name" : "Romeo and Juliet",
                                "screen_title" : "\"Romeo and Juliet\", by Shakespeare"
                            }
                        }
                    }
                """)

        assertEquals(
                default,
                BigConfig().toJson()
        )

        assertEquals(
                default,
                loadConfiguration(BigConfig(), default).toJson()
        )

        assertEquals(
                default.copy().apply {
                    put("stupidity", 0) // me no stupid
                },
                loadConfiguration(BigConfig(), JsonObject("stupidity" to 0)).toJson()
        )

        assertEquals(
                default.copy().apply {
                    getJsonObject("sub")
                            .getJsonObject("play")
                            .put("author", "Some William")
                            .put("screen_title", "\"Romeo and Juliet\", by Some William")
                },
                loadConfiguration(BigConfig(),
                        JsonObject("""{"sub":{"play":{"author":"Some William"}}}""")).toJson()
        )

    }
}
