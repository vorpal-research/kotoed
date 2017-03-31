package org.jetbrains.research.kotoed.data.vcs

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.util.Jsonable

enum class VCS { git, mercurial }
enum class CloneStatus { done, pending }

data class CloneRequest(val vcs: VCS, val url: String) : Jsonable
data class RepositoryInfo(
        val status: CloneStatus,
        val uid: String,
        val url: String,
        val type: VCS,
        val success: Boolean = true,
        val errors: List<String> = listOf()
) : Jsonable

data class ReadRequest(val uid: String, val path: String, val revision: String?) : Jsonable
data class ReadResponse(
        val success: Boolean = true,
        val contents: String,
        val errors: List<String>
) : Jsonable

data class ListRequest(val uid: String, val revision: String?) : Jsonable
data class ListResponse(
        val success: Boolean = true,
        val files: List<String>,
        val errors: List<String>
) : Jsonable

data class DiffRequest(val uid: String, val from: String, val to: String? = null, val path: String? = null) : Jsonable
data class DiffResponse(
        val success: Boolean = true,
        val contents: List<JsonObject>,
        val errors: List<String>
) : Jsonable
