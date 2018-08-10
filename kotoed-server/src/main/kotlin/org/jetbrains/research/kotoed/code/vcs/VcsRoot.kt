package org.jetbrains.research.kotoed.code.vcs

import org.jetbrains.research.kotoed.rootLog
import org.jetbrains.research.kotoed.util.DelegateLoggable
import java.io.File
import java.time.Instant
import java.time.OffsetDateTime

sealed class VcsResult<out T> {
    data class Success<out T>(val v: T) : VcsResult<T>()
    data class Failure(val output: Sequence<String>) : VcsResult<Nothing>()
}

inline fun <T, U> VcsResult<T>.map(f: (T) -> U): VcsResult<U> =
        when (this) {
            is VcsResult.Success -> VcsResult.Success(f(v))
            is VcsResult.Failure -> VcsResult.Failure(output)
        }

inline fun <T> VcsResult<T>.recover(f: (Sequence<String>) -> VcsResult<T>): VcsResult<T> =
        when (this) {
            is VcsResult.Success -> this
            is VcsResult.Failure -> f(output)
        }

abstract class VcsRoot(val remote: String, val local: String) {
    sealed class Revision {
        data class Id(val rep: String) : Revision()
        object Trunk : Revision()

        companion object {
            fun valueOf(rep: String) = when (rep.toLowerCase()) {
                "tip", "head", "current", "" -> Trunk
                else -> Id(rep)
            }

            @Suppress("NOTHING_TO_INLINE")
            inline operator fun invoke(rep: String) = valueOf(rep)
        }
    }

    abstract fun clone(): VcsResult<Unit>
    abstract fun update(): VcsResult<Unit>
    abstract fun checkoutTo(revision: Revision, targetDirectory: String): VcsResult<Unit>
    abstract fun cat(path: String, revision: Revision): VcsResult<Sequence<String>>
    abstract fun diff(path: String, from: Revision, to: Revision): VcsResult<Sequence<String>>
    abstract fun diffAll(from: Revision, to: Revision): VcsResult<Sequence<String>>
    abstract fun ls(rev: Revision): VcsResult<Sequence<String>>
    abstract fun info(rev: Revision, branch: String?): VcsResult<Pair<String, String>>
    abstract fun ping(): VcsResult<Unit>
    abstract fun date(path: String, fromLine: Int?, toLine: Int?): VcsResult<Instant>
}

class Git(remote: String, local: String) : VcsRoot(remote, local) {
    private val git = "git"

    private val Revision.rep
        get() =
            when (this) {
                is Revision.Trunk -> "HEAD"
                is Revision.Id -> rep
            }

    override fun clone(): VcsResult<Unit> {
        File(local).mkdirs()
        val res = CommandLine(git, "clone", "--bare", remote, local).execute(File(local)).complete()
        if (res.rcode.get() == 0) return VcsResult.Success(Unit)
        else return VcsResult.Failure(res.cerr)
    }

    override fun update(): VcsResult<Unit> {
        val res = CommandLine(git, "fetch", "origin", "*:*", "--force").execute(File(local)).complete()

        val die = { res: CommandLine.Output ->
            VcsResult.Failure(res.cerr)
                    .also { DelegateLoggable(Git::class.java).log.error("Cmd failed with: $res") }
        }

        if (res.rcode.get() != 0) return die(res)

        return VcsResult.Success(Unit)
    }

    override fun checkoutTo(revision: Revision, targetDirectory: String): VcsResult<Unit> {
        val target = File(targetDirectory)
        target.mkdirs()
        val init = CommandLine(git, "init")
                .execute(target).complete()
        if(init.rcode.get() != 0) return VcsResult.Failure(init.cerr)

        val fetch = CommandLine(git, "fetch", local, revision.rep, "--depth", "1")
                .execute(target).complete()
        if(fetch.rcode.get() != 0) return VcsResult.Failure(fetch.cerr)

        val reset = CommandLine(git, "reset", "--hard", "FETCH_HEAD")
                .execute(target).complete()

        if(fetch.rcode.get() != 0) return VcsResult.Failure(reset.cerr)

        return VcsResult.Success(Unit)
    }

    override fun cat(path: String, revision: Revision): VcsResult<Sequence<String>> {
        val res = CommandLine(git, "show", "${revision.rep}:$path").execute(File(local)).complete()
        if (res.rcode.get() == 0) return VcsResult.Success(res.cout)
        else return VcsResult.Failure(res.cerr)
    }

    override fun diff(path: String, from: Revision, to: Revision): VcsResult<Sequence<String>> {
        val res = CommandLine(git, "diff", "--minimal", "--ignore-space-at-eol",
                "${from.rep}..${to.rep}", "--", path).execute(File(local)).complete()
        if (res.rcode.get() == 0) return VcsResult.Success(res.cout)
        else return VcsResult.Failure(res.cerr)
    }

    override fun diffAll(from: Revision, to: Revision): VcsResult<Sequence<String>> {
        val res = CommandLine(git, "diff", "--minimal", "--ignore-space-at-eol",
                "${from.rep}..${to.rep}").execute(File(local)).complete()
        if (res.rcode.get() == 0) return VcsResult.Success(res.cout)
        else return VcsResult.Failure(res.cerr)
    }

    override fun ls(rev: Revision): VcsResult<Sequence<String>> {
        val res = CommandLine(git, "ls-tree", rev.rep, "-r", "--name-only").execute(File(local)).complete()
        if (res.rcode.get() == 0) return VcsResult.Success(res.cout)
        else return VcsResult.Failure(res.cerr)
    }

    override fun info(rev: Revision, branch: String?): VcsResult<Pair<String, String>> {
        val commands = mutableListOf(git, "log", "-1", "--color=never", "--pretty=format:%H+%D")
        if (rev != Revision.Trunk) {
            commands += rev.rep
        }

        if (branch != null) {
            if (rev == Revision.Trunk) commands += branch
        }

        val res = CommandLine(commands).execute(File(local)).complete()

        if (res.rcode.get() == 0) {
            val rr = res.cout.first()
            val (frev, fbranch) = rr.split("+")
            val (fbranchName) = fbranch.split(", ")
            return VcsResult.Success(Pair(frev, fbranchName))
        } else return VcsResult.Failure(res.cerr)
    }

    override fun ping(): VcsResult<Unit> {
        val res = CommandLine(git, "ls-remote", "-h", remote).execute(File(local)).complete()
        if (res.rcode.get() == 0) return VcsResult.Success(Unit)
        else return VcsResult.Failure(sequenceOf())
    }

    override fun date(path: String, fromLine: Int?, toLine: Int?): VcsResult<Instant> {
        val from = fromLine ?: ""
        val to = toLine ?: ""
        val res = CommandLine(git, "blame", "--date", "unix", "-L", "$from,$to", "--", path)
                .execute(File(local)).complete()
        if(res.rcode.get() == 0) {
            val output = res.cout.map { it.split(' ').filter { it.isNotEmpty() }[2].toLong() }
                    .min()?.let { Instant.ofEpochSecond(it) }
            return VcsResult.Success(output!!)
        } else return VcsResult.Failure(res.cerr)
    }
}

class Mercurial(remote: String, local: String) : VcsRoot(remote, local) {
    private val mercurial = "hg"

    private val Revision.rep
        get() =
            when (this) {
                is Revision.Trunk -> "tip"
                is Revision.Id -> rep
            }

    override fun clone(): VcsResult<Unit> {
        File(local).mkdirs()
        val res = CommandLine(mercurial, "clone", "-U", remote, local).execute(File(local)).complete()
        if (res.rcode.get() == 0) return VcsResult.Success(Unit)
        else return VcsResult.Failure(res.cerr)
    }

    override fun info(rev: Revision, branch: String?): VcsResult<Pair<String, String>> {
        val commands = mutableListOf(mercurial, "identify", "--id", "--branch", "--debug")
        if (rev != Revision.Trunk) {
            commands += "-r"
            commands += rev.rep
        }

        if (branch != null) {
            commands += "-b"
            commands += branch
        }

        val res = CommandLine(commands).execute(File(local)).complete()

        if (res.rcode.get() == 0) return VcsResult.Success(res.cout.last().split(" ").let { Pair(it[0], it[1]) })
        else return VcsResult.Failure(res.cerr)
    }

    override fun update(): VcsResult<Unit> {
        val res = CommandLine(mercurial, "pull").execute(File(local)).complete()
        if (res.rcode.get() == 0) return VcsResult.Success(Unit)
        else return VcsResult.Failure(res.cerr)
    }

    override fun checkoutTo(revision: Revision, targetDirectory: String): VcsResult<Unit> {
        val target = File(targetDirectory)
        target.mkdirs()
        val res = CommandLine(mercurial, "clone", "-r", revision.rep, local, targetDirectory)
                .execute(File(targetDirectory)).complete()

        if(res.rcode.get() != 0) return VcsResult.Failure(res.cerr)

        return VcsResult.Success(Unit)
    }

    override fun cat(path: String, revision: Revision): VcsResult<Sequence<String>> {
        val res = CommandLine(mercurial, "cat", "-r", revision.rep, path).execute(File(local)).complete()
        if (res.rcode.get() == 0) return VcsResult.Success(res.cout)
        else return VcsResult.Failure(res.cerr)
    }

    override fun diff(path: String, from: Revision, to: Revision): VcsResult<Sequence<String>> {
        val res = CommandLine(
                mercurial, "diff",
                "--git",
                "-r", from.rep, "-r", to.rep, path).execute(File(local)).complete()
        if (res.rcode.get() == 0) return VcsResult.Success(res.cout)
        else return VcsResult.Failure(res.cerr)
    }

    override fun diffAll(from: Revision, to: Revision): VcsResult<Sequence<String>> {
        val res = CommandLine(
                mercurial, "diff",
                "--git",
                "-r", from.rep, "-r", to.rep).execute(File(local)).complete()
        if (res.rcode.get() == 0) return VcsResult.Success(res.cout)
        else return VcsResult.Failure(res.cerr)
    }

    override fun ls(rev: Revision): VcsResult<Sequence<String>> {
        val res = CommandLine(mercurial, "files", "-q", "-r", rev.rep).execute(File(local)).complete()
        if (res.rcode.get() == 0) return VcsResult.Success(res.cout)
        else return VcsResult.Failure(res.cerr)
    }

    override fun ping(): VcsResult<Unit> {
        val res = CommandLine(mercurial, "identify", remote).execute(File(local)).complete()
        if (res.rcode.get() == 0) return VcsResult.Success(Unit)
        else return VcsResult.Failure(res.cerr)
    }

    override fun date(path: String, fromLine: Int?, toLine: Int?): VcsResult<Instant> {
        TODO("not implemented yet")
    }
}
