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
                        "Splendidness" : 9001,
                        "Stupidity" : ${0xDEADBEEF},
                        "Sub" : {
                            "MaidenName" : "Hertz",
                            "Play" : {
                                "Author" : "Shakespeare",
                                "Name" : "Romeo and Juliet",
                                "ScreenTitle" : "\"Romeo and Juliet\", by Shakespeare"
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
                    put("Stupidity", 0) // me no stupid
                },
                loadConfiguration(BigConfig(), JsonObject("Stupidity" to 0)).toJson()
        )

        assertEquals(
                default.copy().apply {
                    getJsonObject("Sub")
                            .getJsonObject("Play")
                            .put("Author", "Some William")
                            .put("ScreenTitle", "\"Romeo and Juliet\", by Some William")
                },
                loadConfiguration(BigConfig(),
                        JsonObject("""{"Sub":{"Play":{"Author":"Some William"}}}""")).toJson()
        )

    }
}
