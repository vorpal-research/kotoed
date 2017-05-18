package org.jetbrains.research.kotoed.data.vcs

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.code.Location
import org.jetbrains.research.kotoed.util.Jsonable

data class VcsException(val messages: List<String>): Jsonable, Exception(messages.joinToString("\n"))

enum class VCS { git, mercurial }
enum class CloneStatus { done, failed, pending }

data class PingResponse(val status: Boolean): Jsonable

data class RemoteRequest(val vcs: VCS?, val url: String) : Jsonable
data class RepositoryInfo(
        val status: CloneStatus,
        val uid: String,
        val url: String,
        val vcs: VCS?,
        val errors: List<String> = listOf()
) : Jsonable

data class ReadRequest(val uid: String, val path: String, val revision: String? = null) : Jsonable
data class ReadResponse(val contents: String) : Jsonable

data class ListRequest(val uid: String, val revision: String?) : Jsonable
data class ListResponse(val files: List<String>) : Jsonable

data class InfoFormat(
        val uid: String,
        val revision: String? = null,
        val branch: String? = null
) : Jsonable

data class DiffRequest(
        val uid: String,
        val from: String,
        val to: String? = null, // null means "tip"
        val path: String? = null // null means the whole repository
) : Jsonable
data class DiffResponse(val contents: List<JsonObject>) : Jsonable

data class LocationRequest(
        val uid: String,
        val location: Location,
        val fromRevision: String,
        val toRevision: String
) : Jsonable
data class LocationResponse(val location: Location): Jsonable
