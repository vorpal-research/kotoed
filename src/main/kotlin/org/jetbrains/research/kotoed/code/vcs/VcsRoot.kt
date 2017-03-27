package org.jetbrains.research.kotoed.code.vcs

import java.io.File

sealed class VcsResult<out T> {
    data class Success<out T>(val v: T): VcsResult<T>()
    data class Failure(val output: Sequence<String>): VcsResult<Nothing>()
}

abstract class VcsRoot(val remote: String, val local: String) {
    sealed class Revision {
        data class Id(val rep: String): Revision()
        object Trunk: Revision()
    }

    abstract fun clone(): VcsResult<Unit>
    abstract fun update(): VcsResult<Unit>
    abstract fun cat(path: String, revision: Revision): VcsResult<Sequence<String>>
    abstract fun diff(path: String, from: Revision, to: Revision): VcsResult<Sequence<String>>
    abstract fun ls(rev: Revision): VcsResult<Sequence<String>>
}

class Git(remote: String, local: String): VcsRoot(remote, local) {
    private val git = "git"

    private val Revision.rep get() =
        when(this) {
            is Revision.Trunk -> "HEAD"
            is Revision.Id -> rep
        }

    override fun clone(): VcsResult<Unit> {
        File(local).mkdirs()
        val res = CommandLine(git, "clone", remote, local).execute(File(local)).complete()
        if(res.rcode.get() == 0) return VcsResult.Success(Unit)
        else return VcsResult.Failure(res.cerr)
    }

    override fun update(): VcsResult<Unit> {
        val res = CommandLine(git, "pull").execute(File(local)).complete()
        if(res.rcode.get() == 0) return VcsResult.Success(Unit)
        else return VcsResult.Failure(res.cerr)
    }

    override fun cat(path: String, revision: Revision): VcsResult<Sequence<String>> {
        val res = CommandLine(git, "show", "${revision.rep}:path").execute(File(local)).complete()
        if(res.rcode.get() == 0) return VcsResult.Success(res.cout)
        else return VcsResult.Failure(res.cerr)
    }

    override fun diff(path: String, from: Revision, to: Revision): VcsResult<Sequence<String>> {
        val res = CommandLine(git, "diff", "${from.rep}..${to.rep}", path).execute(File(local)).complete()
        if(res.rcode.get() == 0) return VcsResult.Success(res.cout)
        else return VcsResult.Failure(res.cerr)
    }

    override fun ls(rev: Revision): VcsResult<Sequence<String>> {
        val res = CommandLine(git, "ls-tree", rev.rep, "-r", "--name-only").execute(File(local)).complete()
        if(res.rcode.get() == 0) return VcsResult.Success(res.cout)
        else return VcsResult.Failure(res.cerr)
    }
}

class Mercurial(remote: String, local: String): VcsRoot(remote, local) {
    private val mercurial = "hg"

    private val Revision.rep get() =
    when(this) {
        is Revision.Trunk -> "tip"
        is Revision.Id -> rep
    }

    override fun clone(): VcsResult<Unit> {
        File(local).mkdirs()
        val res = CommandLine(mercurial, "clone", remote, local).execute(File(local)).complete()
        if(res.rcode.get() == 0) return VcsResult.Success(Unit)
        else return VcsResult.Failure(res.cerr)
    }

    override fun update(): VcsResult<Unit> {
        val res = CommandLine(mercurial, "pull").execute(File(local)).complete()
        if(res.rcode.get() == 0) return VcsResult.Success(Unit)
        else return VcsResult.Failure(res.cerr)
    }

    override fun cat(path: String, revision: Revision): VcsResult<Sequence<String>> {
        val res = CommandLine(mercurial, "cat", "-r", revision.rep, path).execute(File(local)).complete()
        if(res.rcode.get() == 0) return VcsResult.Success(res.cout)
        else return VcsResult.Failure(res.cerr)
    }

    override fun diff(path: String, from: Revision, to: Revision): VcsResult<Sequence<String>> {
        val res = CommandLine(mercurial, "diff", "-r", from.rep, "-r", to.rep, path).execute(File(local)).complete()
        if(res.rcode.get() == 0) return VcsResult.Success(res.cout)
        else return VcsResult.Failure(res.cerr)
    }

    override fun ls(rev: Revision): VcsResult<Sequence<String>> {
        val res = CommandLine(mercurial, "files", "-q", "-r", rev.rep).execute(File(local)).complete()
        if(res.rcode.get() == 0) return VcsResult.Success(res.cout)
        else return VcsResult.Failure(res.cerr)
    }

}






