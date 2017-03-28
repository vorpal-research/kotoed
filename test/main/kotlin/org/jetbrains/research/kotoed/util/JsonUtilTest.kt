package org.jetbrains.research.kotoed.util

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.teamcity.project.BuildConfig
import org.jetbrains.research.kotoed.data.teamcity.project.CreateProject
import org.jetbrains.research.kotoed.data.teamcity.project.Project
import org.jetbrains.research.kotoed.data.teamcity.project.VcsRoot
import org.junit.Test
import java.io.File
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

data class ExampleMessage(val p1: Int, val p2: String, val p3: Int?) : Jsonable
data class ExampleMessage2(val payload: List<ExampleMessage>) : Jsonable
data class ExampleMessage3(val payload: Map<Int, ExampleMessage?>) : Jsonable
data class ExampleMessage4(val p1: Boolean, val p2: Boolean?) : Jsonable
data class Tangled(val data: List<Int>, val next: Map<Int, List<Tangled>>) : Jsonable

data class Custom(val contents: List<File>): Jsonable {
    override fun toJson(): JsonObject {
        return JsonObject("contents" to contents.map { it.name })
    }

    companion object: JsonableCompanion<Custom> {
        override val dataklass: KClass<Custom> = klassOf()
        override fun fromJson(json: JsonObject): Custom? {
            try {
                return Custom(json.getJsonArray("contents").map { File(it as String) })
            } catch (ex: Exception){
                return null
            }
        }
    }
}

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
                    "p1": true,
                    "p2": null
                }
            """),
                ExampleMessage4(true, null).toJson()
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

        // ad-hoc objects (not-fromJson-able)

        val Root = object : Jsonable {
            val Branch = object : Jsonable {
                val Leaf = object : Jsonable {
                    val Data = listOf(3, 4, "Hello")
                }
            }
        }

        assertEquals(
                JsonObject("""
                    {
                        "Branch" : {
                            "Leaf" : {
                                "Data" : [ 3, 4, "Hello" ]
                            }
                        }
                    }
                """),
                Root.toJson()
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

        assertEquals(
                fromJson(JsonObject("""
                    {
                        "p1": false,
                        "p2": false
                    }
                """)),
                ExampleMessage4(false, false)
        )

        assertEquals(
                fromJson(JsonObject("""
                    {
                        "project": {
                            "id": "Test",
                            "name": "Test project",
                            "rootProjectId": "_Root"
                        },
                        "vcsRoot": {
                            "id": "Test_VCS",
                            "name": "Test project VCS",
                            "type": "mercurial",
                            "url": "https://bitbucket.org/vorpal-research/kotoed",
                            "projectId": "Test"
                        },
                        "buildConfig": {
                            "id": "Test_Build",
                            "name": "Test project build config",
                            "templateId": "Test_Default_Build_Template"
                        }
                    }
                """)),
                CreateProject(
                        Project("Test", "Test project", "_Root"),
                        VcsRoot("Test_VCS", "Test project VCS", "mercurial", "https://bitbucket.org/vorpal-research/kotoed", "Test"),
                        BuildConfig("Test_Build", "Test project build config", "Test_Default_Build_Template")
                )
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

    @Test
    fun testCustom() {
        assertEquals(Custom(listOf(File("a"), File("b"), File("c"))), fromJson(JsonObject("""
            { "contents" : ["a", "b", "c"] }
        """)))

        assertEquals(Custom(listOf(File("a"), File("b"), File("c"))).toJson(), JsonObject("""
            { "contents" : ["a", "b", "c"] }
        """))

        data class Custom2(val inner: List<Custom>): Jsonable

        val custom = Custom2(listOf(Custom(listOf(File("a"), File("b"), File("c"), File("d")))))

        assertEquals(custom, fromJson(custom.toJson()))

    }

}
