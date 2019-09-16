package org.jetbrains.research.kotoed.data.vcs

import org.jetbrains.research.kotoed.code.Location
import org.jetbrains.research.kotoed.code.diff.DiffJsonable
import org.jetbrains.research.kotoed.util.Jsonable
import java.time.Instant

data class VcsException(val messages: String): Jsonable, Exception(messages)

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
data class ReadResponse(val contents: CharSequence) : Jsonable

data class ListRequest(val uid: String, val revision: String?) : Jsonable
data class ListResponse(val files: List<String>) : Jsonable

data class CheckoutRequest(val uid: String, val targetDirectory: String, val revision: String? = null): Jsonable

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
data class DiffResponse(val contents: List<DiffJsonable>) : Jsonable

data class LocationRequest(
        val uid: String,
        val location: Location,
        val fromRevision: String,
        val toRevision: String
) : Jsonable
data class LocationResponse(val location: Location): Jsonable

data class BlameRequest(
        val uid: String,
        val path: String,
        val fromLine: Int?,
        val toLine: Int?
): Jsonable
data class BlameResponse(val time: Instant): Jsonable
