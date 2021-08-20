package org.jetbrains.research.kotoed.code.vcs

import kotlinx.coroutines.future.await
import org.jetbrains.research.kotoed.util.DelegateLoggable
import java.io.File
import java.time.Instant
import java.util.*

sealed class VcsResult<out T> {
    data class Success<out T>(val v: T) : VcsResult<T>()
    data class Failure(val output: CharSequence) : VcsResult<Nothing>()
}

inline fun <T, U> VcsResult<T>.map(f: (T) -> U): VcsResult<U> =
        when (this) {
            is VcsResult.Success -> VcsResult.Success(f(v))
            is VcsResult.Failure -> VcsResult.Failure(output)
        }

inline fun <T> VcsResult<T>.recover(f: (CharSequence) -> VcsResult<T>): VcsResult<T> =
        when (this) {
            is VcsResult.Success -> this
            is VcsResult.Failure -> f(output)
        }

abstract class VcsRoot(val remote: String, val local: String) {
    sealed class Revision {
        data class Id(val rep: String) : Revision()
        object Trunk : Revision()

        companion object {
            fun valueOf(rep: String) = when (rep.lowercase(Locale.getDefault())) {
                "tip", "head", "current", "" -> Trunk
                else -> Id(rep)
            }

            @Suppress("NOTHING_TO_INLINE")
            inline operator fun invoke(rep: String) = valueOf(rep)
        }
    }

    abstract suspend fun clone(): VcsResult<Unit>
    abstract suspend fun update(): VcsResult<Unit>
    abstract suspend fun checkoutTo(revision: Revision, targetDirectory: String): VcsResult<Unit>
    abstract suspend fun cat(path: String, revision: Revision): VcsResult<CharSequence>
    abstract suspend fun diff(path: String, from: Revision, to: Revision): VcsResult<CharSequence>
    abstract suspend fun diffAll(from: Revision, to: Revision): VcsResult<CharSequence>
    abstract suspend fun ls(rev: Revision): VcsResult<CharSequence>
    abstract suspend fun info(rev: Revision, branch: String?): VcsResult<Pair<String, String>>
    abstract suspend fun ping(): VcsResult<Unit>
    abstract suspend fun date(path: String, fromLine: Int?, toLine: Int?): VcsResult<Instant>
}

class Git(remote: String, local: String, val defaultEnv: Map<String, String>) : VcsRoot(remote, local) {
    private val git = "git"

    private val Revision.rep
        get() =
            when (this) {
                is Revision.Trunk -> "HEAD"
                is Revision.Id -> rep
            }

    override suspend fun clone(): VcsResult<Unit> {
        File(local).mkdirs()
        val res = CommandLine(git, "clone", "--bare", remote, local).execute(File(local), defaultEnv).complete()
        if (res.rcode.await() == 0) return VcsResult.Success(Unit)
        else return VcsResult.Failure(res.cerr)
    }

    override suspend fun update(): VcsResult<Unit> {
        val res = CommandLine(git, "fetch", "origin", "*:*", "--force").execute(File(local), defaultEnv).complete()

        val die = { out: CommandLine.Output ->
            VcsResult.Failure(out.cerr)
                    .also { DelegateLoggable(Git::class.java).log.error("Cmd failed with: $out") }
        }

        if (res.rcode.await() != 0) return die(res)

        return VcsResult.Success(Unit)
    }

    override suspend fun checkoutTo(revision: Revision, targetDirectory: String): VcsResult<Unit> {
        val target = File(targetDirectory)
        target.mkdirs()
        val init = CommandLine(git, "init")
                .execute(target, defaultEnv).complete()
        if(init.rcode.await() != 0) return VcsResult.Failure(init.cerr)

        val fetch = CommandLine(git, "fetch", local, revision.rep, "--depth", "1")
                .execute(target, defaultEnv).complete()
        if(fetch.rcode.await() != 0) return VcsResult.Failure(fetch.cerr)

        val reset = CommandLine(git, "reset", "--hard", "FETCH_HEAD")
                .execute(target, defaultEnv).complete()

        if(fetch.rcode.await() != 0) return VcsResult.Failure(reset.cerr)

        return VcsResult.Success(Unit)
    }

    override suspend fun cat(path: String, revision: Revision): VcsResult<CharSequence> {
        val res = CommandLine(git, "show", "${revision.rep}:$path").execute(File(local), defaultEnv).complete()
        if (res.rcode.await() == 0) return VcsResult.Success(res.cout)
        else return VcsResult.Failure(res.cerr)
    }

    override suspend fun diff(path: String, from: Revision, to: Revision): VcsResult<CharSequence> {
        val res = CommandLine(git, "diff", "--minimal", "--ignore-space-at-eol",
                "${from.rep}..${to.rep}", "--", path).execute(File(local), defaultEnv).complete()
        if (res.rcode.await() == 0) return VcsResult.Success(res.cout)
        else return VcsResult.Failure(res.cerr)
    }

    override suspend fun diffAll(from: Revision, to: Revision): VcsResult<CharSequence> {
        val res = CommandLine(git, "diff", "--minimal", "--ignore-space-at-eol",
                "${from.rep}..${to.rep}").execute(File(local), defaultEnv).complete()
        if (res.rcode.await() == 0) return VcsResult.Success(res.cout)
        else return VcsResult.Failure(res.cerr)
    }

    override suspend fun ls(rev: Revision): VcsResult<CharSequence> {
        val res = CommandLine(git, "ls-tree", rev.rep, "-r", "--name-only").execute(File(local), defaultEnv).complete()
        if (res.rcode.await() == 0) return VcsResult.Success(res.cout)
        else return VcsResult.Failure(res.cerr)
    }

    override suspend fun info(rev: Revision, branch: String?): VcsResult<Pair<String, String>> {
        val commands = mutableListOf(git, "log", "-1", "--color=never", "--pretty=format:%H+%D")
        if (rev != Revision.Trunk) {
            commands += rev.rep
        }

        if (branch != null) {
            if (rev == Revision.Trunk) commands += branch
        }

        val res = CommandLine(commands).execute(File(local), defaultEnv).complete()

        if (res.rcode.await() == 0) {
            val rr = res.cout.lineSequence().first()
            val (frev, fbranch) = rr.split("+")
            val (fbranchName) = fbranch.split(", ")
            return VcsResult.Success(Pair(frev, fbranchName))
        } else return VcsResult.Failure(res.cerr)
    }

    override suspend fun ping(): VcsResult<Unit> {
        val res = CommandLine(git, "ls-remote", "-h", remote).execute(File(local), defaultEnv).complete()
        if (res.rcode.await() == 0) return VcsResult.Success(Unit)
        else return VcsResult.Failure("")
    }

    override suspend fun date(path: String, fromLine: Int?, toLine: Int?): VcsResult<Instant> {
        val from = fromLine ?: ""
        val to = toLine ?: ""
        val res = CommandLine(git, "blame", "--date", "unix", "-L", "$from,$to", "--", path)
                .execute(File(local), defaultEnv).complete()
        if(res.rcode.await() == 0) {
            val output = res.cout.lineSequence().map { it.split(' ').filter { it.isNotEmpty() }[2].toLong() }
                    .minOrNull()?.let { Instant.ofEpochSecond(it) }
            return VcsResult.Success(output!!)
        } else return VcsResult.Failure(res.cerr)
    }
}

class Mercurial(remote: String, local: String, val defaultEnv: Map<String, String>) : VcsRoot(remote, local) {
    private val mercurial = "hg"

    private val Revision.rep
        get() =
            when (this) {
                is Revision.Trunk -> "tip"
                is Revision.Id -> rep
            }

    override suspend fun clone(): VcsResult<Unit> {
        File(local).mkdirs()
        val res = CommandLine(mercurial, "clone", "-U", remote, local).execute(File(local), defaultEnv).complete()
        if (res.rcode.await() == 0) return VcsResult.Success(Unit)
        else return VcsResult.Failure(res.cerr)
    }

    override suspend fun info(rev: Revision, branch: String?): VcsResult<Pair<String, String>> {
        val commands = mutableListOf(mercurial, "identify", "--id", "--branch", "--debug")
        if (rev != Revision.Trunk) {
            commands += "-r"
            commands += rev.rep
        }

        if (branch != null) {
            commands += "-b"
            commands += branch
        }

        val res = CommandLine(commands).execute(File(local), defaultEnv).complete()

        if (res.rcode.await() == 0) return VcsResult.Success(res.cout.lines().last().split(" ").let { Pair(it[0], it[1]) })
        else return VcsResult.Failure(res.cerr)
    }

    override suspend fun update(): VcsResult<Unit> {
        val res = CommandLine(mercurial, "pull").execute(File(local), defaultEnv).complete()
        if (res.rcode.await() == 0) return VcsResult.Success(Unit)
        else return VcsResult.Failure(res.cerr)
    }

    override suspend fun checkoutTo(revision: Revision, targetDirectory: String): VcsResult<Unit> {
        val target = File(targetDirectory)
        target.mkdirs()
        val res = CommandLine(mercurial, "clone", "-r", revision.rep, local, targetDirectory)
                .execute(File(targetDirectory), defaultEnv).complete()

        if(res.rcode.await() != 0) return VcsResult.Failure(res.cerr)

        return VcsResult.Success(Unit)
    }

    override suspend fun cat(path: String, revision: Revision): VcsResult<CharSequence> {
        val res = CommandLine(mercurial, "cat", "-r", revision.rep, path).execute(File(local), defaultEnv).complete()
        if (res.rcode.await() == 0) return VcsResult.Success(res.cout)
        else return VcsResult.Failure(res.cerr)
    }

    override suspend fun diff(path: String, from: Revision, to: Revision): VcsResult<CharSequence> {
        val res = CommandLine(
                mercurial, "diff",
                "--git",
                "-r", from.rep, "-r", to.rep, path).execute(File(local), defaultEnv).complete()
        if (res.rcode.await() == 0) return VcsResult.Success(res.cout)
        else return VcsResult.Failure(res.cerr)
    }

    override suspend fun diffAll(from: Revision, to: Revision): VcsResult<CharSequence> {
        val res = CommandLine(
                mercurial, "diff",
                "--git",
                "-r", from.rep, "-r", to.rep).execute(File(local), defaultEnv).complete()
        if (res.rcode.await() == 0) return VcsResult.Success(res.cout)
        else return VcsResult.Failure(res.cerr)
    }

    override suspend fun ls(rev: Revision): VcsResult<CharSequence> {
        val res = CommandLine(mercurial, "files", "-q", "-r", rev.rep).execute(File(local), defaultEnv).complete()
        if (res.rcode.await() == 0) return VcsResult.Success(res.cout)
        else return VcsResult.Failure(res.cerr)
    }

    override suspend fun ping(): VcsResult<Unit> {
        val res = CommandLine(mercurial, "identify", remote).execute(File(local), defaultEnv).complete()
        if (res.rcode.await() == 0) return VcsResult.Success(Unit)
        else return VcsResult.Failure(res.cerr)
    }

    override suspend fun date(path: String, fromLine: Int?, toLine: Int?): VcsResult<Instant> {
        TODO("not implemented yet")
    }
}
