package org.jetbrains.research.kotoed.code

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.RemovalNotification
import io.vertx.core.Promise
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.research.kotoed.code.diff.asJsonable
import org.jetbrains.research.kotoed.code.diff.parseGitDiff
import org.jetbrains.research.kotoed.code.vcs.*
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.vcs.*
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.code.alignToLines
import org.jetbrains.research.kotoed.util.code.location
import org.jetbrains.research.kotoed.util.code.temporaryEnv
import org.jetbrains.research.kotoed.util.code.thisLine
import org.jline.utils.Levenshtein
import org.wickedsource.diffparser.api.UnifiedDiffParser
import org.wickedsource.diffparser.api.model.Diff
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

@AutoDeployable
class CodeVerticle : AbstractKotoedVerticle(), Loggable {
    private val dir by lazy {
        File(System.getProperty("user.dir"), Config.VCS.StoragePath)
    }
    private val ee by lazy {
        betterFixedThreadPoolContext(Config.VCS.PoolSize, "codeVerticle.dispatcher")
    }
    private val procs by lazy<Cache<String, RepositoryInfo>> {
        CacheBuilder
                .newBuilder()
                .maximumSize(Config.VCS.CloneCapacity)
                .expireAfterAccess(Config.VCS.CloneExpire, TimeUnit.MILLISECONDS)
                .removalListener(this::onCacheRemove)
                .build()
    }
    private val info = mutableMapOf<String, RemoteRequest>()

    private val defaultEnv by lazy { Config.VCS.DefaultEnvironment }

    private val RepositoryInfo.root get() = when (vcs) {
        VCS.git -> Git(url, File(dir, uid).absolutePath, defaultEnv)
        VCS.mercurial -> Mercurial(url, File(dir, uid).absolutePath, defaultEnv)
        null -> throw IllegalArgumentException("No supported vcs found at $url")
    }

    fun onCacheRemove(removalNotification: RemovalNotification<String, RepositoryInfo>) =
            spawn {
                if (removalNotification.value.status == CloneStatus.pending) return@spawn

                vertx.goToEventLoop()
                val fs = vertx.fileSystem()
                val uid = removalNotification.value.uid
                info.remove(uid)
                val cat = File(dir, uid)
                log.info("The entry for repository $uid expired, deleting the files...")
                if (cat.exists()) fs.deleteRecursiveAsync(cat.absolutePath)
                log.info("Repository $uid deleted")
            }

    override fun start(startPromise: Promise<Void>) = spawn {
        val fs = vertx.fileSystem()
        if (dir.exists()) fs.deleteRecursiveAsync(dir.absolutePath)
        dir.mkdir()
        super.start(startPromise)
    }

    override fun stop(stopPromise: Promise<Void>) = spawn {
        procs.cleanUp()
        val fs = vertx.fileSystem()
        if (dir.exists()) fs.deleteRecursiveAsync(dir.absolutePath)
        super.stop(stopPromise)
    }

    private val <T> VcsResult<T>.result
        get() = when (this) {
            is VcsResult.Success -> v
            is VcsResult.Failure -> throw VcsException("$output")
        }

    @JsonableEventBusConsumerFor(Address.Code.Checkout)
    suspend fun handleCheckout(message: CheckoutRequest) {
        log.info("Requested checkout: $message")

        UUID.fromString(message.uid)

        val inf = expectNotNull(info[message.uid], "Repository not found")
        val root = expectNotNull(procs[inf.url], "Inconsistent repo state").root

        val checkoutRes = withContext(ee) {
            val rev = message.revision?.let { VcsRoot.Revision(it) } ?: VcsRoot.Revision.Trunk
            if(rev == VcsRoot.Revision.Trunk) root.update()
            root.update()
        }.result
        checkoutRes.ignore()
    }

    @JsonableEventBusConsumerFor(Address.Code.Read)
    suspend fun handleRead(message: ReadRequest): ReadResponse {
        log.info("Requested read: $message")

        UUID.fromString(message.uid)

        val path = message.path

        val inf = expectNotNull(info[message.uid], "Repository not found")
        val root = expectNotNull(procs[inf.url], "Inconsistent repo state").root

        val catRes = withContext(ee) {
            val rev = message.revision?.let { VcsRoot.Revision(it) } ?: VcsRoot.Revision.Trunk
            if(rev == VcsRoot.Revision.Trunk) root.update()
            root.cat(path, rev).recover {
                root.update()
                root.cat(path, rev)
            }
        }.result

        return ReadResponse(contents = catRes)
    }

    @JsonableEventBusConsumerFor(Address.Code.List)
    suspend fun handleList(message: ListRequest): ListResponse {
        log.info("Requested list: $message")

        UUID.fromString(message.uid)

        val inf = expectNotNull(info[message.uid], "Repository not found")

        val root = expectNotNull(procs[inf.url], "Inconsistent repo state").root

        val lsRes = withContext(ee) {
            val rev = message.revision?.let { VcsRoot.Revision(it) } ?: VcsRoot.Revision.Trunk
            if(rev == VcsRoot.Revision.Trunk) root.update()
            root.ls(rev).recover {
                root.update()
                root.ls(rev)
            }
        }.result

        return ListResponse(files = lsRes.lines().filter { it.isNotEmpty() })
    }

    @JsonableEventBusConsumerFor(Address.Code.Ping)
    suspend fun handlePing(message: RemoteRequest): PingResponse {
        log.info("Pinging repository: $message")

        val url = message.url
        val pendingResp = RepositoryInfo(
                status = CloneStatus.pending,
                uid = "",
                url = url,
                vcs = message.vcs ?: VCS.git
        )

        val vcsRes = withContext(ee) { pendingResp.root.ping() }

        return PingResponse(vcsRes is VcsResult.Success)
    }

    suspend fun guessVcsType(message: RemoteRequest): VCS? {
        val tryHg = try {
            handlePing(message.copy(vcs = VCS.mercurial))
        } catch (ex: Exception) {
            null
        }

        if (tryHg != null && tryHg.status) return VCS.mercurial

        val tryGit = try {
            handlePing(message.copy(vcs = VCS.git))
        } catch (ex: Exception) {
            null
        }

        if (tryGit != null && tryGit.status) return VCS.git

        throw IllegalArgumentException("No supported vcs type found: ${message.url}")
    }

    @JsonableEventBusConsumerFor(Address.Code.Info)
    suspend fun handleInfo(request: InfoFormat): InfoFormat {
        log.info("Requested info: $request")

        UUID.fromString(request.uid)
        val inf = expectNotNull(info[request.uid], "Repository not found")
        val root = expectNotNull(procs[inf.url], "Inconsistent repo state").root

        val (revRes, brRes) = withContext(ee) {
            val rev = request.revision?.let { VcsRoot.Revision(it) } ?: VcsRoot.Revision.Trunk
            if(rev == VcsRoot.Revision.Trunk) root.update()
            root.info(rev, request.branch).recover {
                root.update()
                root.info(rev, request.branch)
            }
        }.result

        return request.copy(revision = revRes, branch = brRes)
    }

    @JsonableEventBusConsumerFor(Address.Code.PurgeCache)
    suspend fun handlePurgeCache(message: RemoteRequest) {
        log.trace("Requested cache purge: $message")

        procs.invalidate(message.url)
    }

    @JsonableEventBusConsumerFor(Address.Code.Fetch)
    suspend fun handleFetch(request: FetchRequest) {
        log.info("Requested fetch: $request")

        UUID.fromString(request.uid)

        val inf1 = expectNotNull(info[request.uid], "Repository ${request.uid} not found")
        val root1 = expectNotNull(procs[inf1.url], "Inconsistent repo state").root

        val inf2 = expectNotNull(info[request.externalUid], "Repository ${request.externalUid} not found")
        val root2 = expectNotNull(procs[inf2.url], "Inconsistent repo state").root

        withContext(ee) {
            root1.fetch(root2.local)
        }
    }

    @EventBusConsumerFor(Address.Code.Download)
    suspend fun handleClone(mes: Message<JsonObject>) {
        val message: RemoteRequest = mes.body().toJsonable()

        log.trace("Requested clone: $message")

        val url = message.url
        val existing = procs[url]
        if (existing != null) {
            log.trace("Cloning request for $url: repository already cloned")
            mes.reply(existing.toJson())
            return
        }

        val uid = UUID.randomUUID()
        val randomName = File(dir, "$uid")
        log.trace("Generated uid: $uid")
        log.trace("Using directory: $randomName")

        val pendingResp = RepositoryInfo(
                status = CloneStatus.pending,
                uid = "$uid",
                url = url,
                vcs = message.vcs
        )
        procs[url] = pendingResp
        info["$uid"] = message

        withContext(ee) {
            randomName.mkdirs()
        }


        timedOut(Config.VCS.PendingTimeout, timeoutCtx = coroutineContext + LogExceptions() + currentCoroutineName()) {
            run {
                var resp = pendingResp
                try {
                    val res = withContext(ee) {
                        if (pendingResp.vcs == null) {
                            resp = resp.copy(vcs = guessVcsType(message))
                        }

                        val root = resp.root
                        root.clone().also {
                            log.trace("Cloning finished for $url in directory $randomName")
                        }
                    }
                    vertx.goToEventLoop()

                    log.trace("Cloning request for $url finished")

                    resp =
                            resp.copy(
                                    status =
                                    when (res) {
                                        is VcsResult.Success -> CloneStatus.done
                                        else -> CloneStatus.failed
                                    },
                                    errors = (res as? VcsResult.Failure)?.run { output.lines() }.orEmpty()
                            )

                    log.trace("Cloning request for $url status: ${resp.status}")

                    procs[url] = resp
                } catch (ex: Exception) {
                    procs[url] = resp.copy(status = CloneStatus.failed, errors = listOf(ex.message.toString()))
                    throw ex
                }
            }

            onTimeout {
                mes.reply(procs[url]?.toJson())
                log.trace("Cloning request for $url timed out: sending 'pending' reply")
            }

            onSuccess {
                mes.reply(procs[url]?.toJson())
                log.trace("Cloning request for $url timed out: sending 'successful' reply")
            }
        }

    }

    private fun parseDiff(diffOutput: Sequence<String>): Sequence<Diff> {

        log.info(diffOutput.joinToString("\n"))

        val divided = diffOutput.splitBy { it.startsWith("diff") }.filterNot { it.isEmpty() }

        val res = divided.map {
            log.trace("Parsing:")
            log.trace(it.joinToString("\n"))
            val parser = UnifiedDiffParser()
            val pres = parser.parse(it.asSequence().linesAsCharSequence().asInputStream())
            log.trace(pres.map { it.asJsonable() })
            pres.firstOrNull()
        }

        return res.filterNotNull()
    }

    @JsonableEventBusConsumerFor(Address.Code.Diff)
    suspend fun handleDiff(message: DiffRequest): DiffResponse {
        log.trace("Requested diff: $message")

        UUID.fromString(message.uid)

        val inf = expectNotNull(info[message.uid], "Repository not found")
        val root = expectNotNull(procs[inf.url], "Inconsistent repo state").root

        val diffContents = withContext(ee) {
            val from = message.from.let { VcsRoot.Revision(it) }
            val to = message.to?.let { VcsRoot.Revision(it) }
                    ?: VcsRoot.Revision.Trunk

            val vcsRes = if (message.path != null) root.diff(message.path, from, to)
            else root.diffAll(from, to)

            if(vcsRes.result.length > Config.VCS.MaxDiffSize) listOf()
            else parseGitDiff(vcsRes.result.lineSequence())
        }

        return DiffResponse(contents = diffContents
                // FIXME Do smth when diff does not provide these file names
                .filter { it.fromFileName != null && it.toFileName != null }
                .map { diff ->
                    if (diff.hunks.any { it.lines.size > Config.VCS.MaxDiffHunkLines })
                        diff.apply { hunks.clear() }
                    else diff
                }
                .map { it.asJsonable() })
    }


    suspend fun getPsi(compiler: KotlinCoreEnvironment, request: ReadRequest): KtFile? {
        val read = try { handleRead(request) } catch(ex: VcsException) { return null }
        val psiFactory = KtPsiFactory(compiler.project)

        return psiFactory.createFile(request.path, read.contents.toString())
    }

    fun findCorrespondingFunction(location: Location, fromFile: KtFile, toFile: KtFile): Pair<KtNamedFunction, KtNamedFunction>? {
        val function = fromFile
                .collectDescendantsOfType<KtNamedFunction> { location in it.location.alignToLines() }
                .firstOrNull()
                ?: return null
        val resFunction = toFile.
                collectDescendantsOfType<KtNamedFunction> {
                    val lhv = it.fqName?.asString() ?: it.name
                    val rhv = function.fqName?.asString() ?: function.name
                    lhv == rhv
                }.firstOrNull()
                ?: return null
        return function to resFunction
    }

    suspend fun handleLocationKotlin(message: LocationRequest): LocationResponse? {
        log.trace("Requested location adjustment: $message")

        UUID.fromString(message.uid)

        val inf = expectNotNull(info[message.uid], "Repository not found")

        val root = expectNotNull(procs[inf.url], "Inconsistent repo state").root
        root.ignore()

        val res = temporaryEnv { compiler ->
            val fromFile =
                    getPsi(compiler, ReadRequest(message.uid, message.location.filename.path, message.fromRevision)) ?: return null
            val toFile =
                    getPsi(compiler, ReadRequest(message.uid, message.location.filename.path, message.toRevision)) ?: return null

            val (function, resFunction) = findCorrespondingFunction(message.location, fromFile, toFile)
                    ?: return null

            val messageIndex = message.location - function.location.start.thisLine()
            val messageLine = function.text.lines().getOrNull(messageIndex)
            checkNotNull(messageLine)

            val resLine = resFunction.text.lines().withIndex().minByOrNull { (_, text) ->
                Levenshtein.distance(text.trim(), messageLine.trim())
            }?.index ?: messageIndex

            (resFunction.location.start.thisLine() + resLine)
                    .coerceIn(resFunction.location.alignToLines())
        }

        return LocationResponse(location = res)
    }

    //@JsonableEventBusConsumerFor(Address.Code.LocationDiff)
    suspend fun handleLocationGeneral(message: LocationRequest): LocationResponse {
        log.trace("Requested location adjustment: $message")

        UUID.fromString(message.uid)

        val inf = expectNotNull(info[message.uid], "Repository not found")

        val root = expectNotNull(procs[inf.url], "Inconsistent repo state").root

        val diffRes = withContext(ee) {
            val from = message.fromRevision.let { VcsRoot.Revision(it) }
            val to = message.toRevision.let { VcsRoot.Revision(it) }
            root.diff(message.location.filename.path, from, to)
        }.result

        return LocationResponse(location = message.location.applyDiffs(parseGitDiff(diffRes.lineSequence())))
    }

    @JsonableEventBusConsumerFor(Address.Code.LocationDiff)
    suspend fun handleLocation(message: LocationRequest): LocationResponse =
            // FIXME: this is generally, um, not good
            when {
                message.location.filename.path.endsWith("kt") -> handleLocationKotlin(message)
                        ?: handleLocationGeneral(message)
                else -> handleLocationGeneral(message)
            }

    @JsonableEventBusConsumerFor(Address.Code.Date)
    suspend fun handleBlame(message: BlameRequest): BlameResponse {
        UUID.fromString(message.uid)

        val inf = expectNotNull(info[message.uid], "Repository not found")

        val root = expectNotNull(procs[inf.url], "Inconsistent repo state").root

        val res = withContext(ee) {
            root.date(message.path, message.fromLine, message.toLine)
        }.result

        return BlameResponse(res)
    }

}
