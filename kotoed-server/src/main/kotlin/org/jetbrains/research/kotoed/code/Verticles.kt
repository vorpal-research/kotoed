package org.jetbrains.research.kotoed.code

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.RemovalNotification
import com.intellij.psi.PsiElement
import io.vertx.core.Future
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.run
import org.jetbrains.kootstrap.FooBarCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.research.kotoed.code.diff.asJsonable
import org.jetbrains.research.kotoed.code.diff.parseGitDiff
import org.jetbrains.research.kotoed.code.vcs.*
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.vcs.*
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.util.code.*
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
        newFixedThreadPoolContext(Config.VCS.PoolSize, "codeVerticle.dispatcher")
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

    private val RepositoryInfo.root get() = when (vcs) {
        VCS.git -> Git(url, File(dir, uid).absolutePath)
        VCS.mercurial -> Mercurial(url, File(dir, uid).absolutePath)
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

    override fun start(startFuture: Future<Void>) = spawn {
        val fs = vertx.fileSystem()
        if (dir.exists()) fs.deleteRecursiveAsync(dir.absolutePath)
        dir.mkdir()
        super.start(startFuture)
    }

    override fun stop(stopFuture: Future<Void>) = spawn {
        procs.cleanUp()
        val fs = vertx.fileSystem()
        if (dir.exists()) fs.deleteRecursiveAsync(dir.absolutePath)
        super.stop(stopFuture)
    }

    private val <T> VcsResult<T>.result
        get() = when (this) {
            is VcsResult.Success -> v
            is VcsResult.Failure -> throw VcsException(output.toList())
        }

    @JsonableEventBusConsumerFor(Address.Code.Read)
    suspend fun handleRead(message: ReadRequest): ReadResponse {
        log.info("Requested read: $message")

        UUID.fromString(message.uid)

        val path = message.path

        val inf = expectNotNull(info[message.uid], "Repository not found")
        val root = expectNotNull(procs[inf.url], "Inconsistent repo state").root

        val catRes = run(ee) {
            val rev = message.revision?.let { VcsRoot.Revision(it) } ?: VcsRoot.Revision.Trunk
            if(rev == VcsRoot.Revision.Trunk) root.update()
            root.cat(path, rev).recover {
                root.update()
                root.cat(path, rev)
            }
        }.result

        return ReadResponse(contents = catRes.joinToString("\n"))
    }

    @JsonableEventBusConsumerFor(Address.Code.List)
    suspend fun handleList(message: ListRequest): ListResponse {
        log.info("Requested list: $message")

        UUID.fromString(message.uid)

        val inf = expectNotNull(info[message.uid], "Repository not found")

        val root = expectNotNull(procs[inf.url], "Inconsistent repo state").root

        val lsRes = run(ee) {
            val rev = message.revision?.let { VcsRoot.Revision(it) } ?: VcsRoot.Revision.Trunk
            if(rev == VcsRoot.Revision.Trunk) root.update()
            root.ls(rev).recover {
                root.update()
                root.ls(rev)
            }
        }.result

        return ListResponse(files = lsRes.toList())
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

        val vcsRes = run(ee) { pendingResp.root.ping() }

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

        val (revRes, brRes) = run(ee) {
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

        run(ee) {
            randomName.mkdirs()
        }

        vertx.timedOut(Config.VCS.PendingTimeout, timeoutCtx = LogExceptions() + VertxContext(vertx) + currentCoroutineName()) {
            run {
                var resp = pendingResp
                try {
                    val res = run(ee) {
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
                                    errors = (res as? VcsResult.Failure)?.run { output.toList() }.orEmpty()
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

        val diffRes = run(ee) {
            val from = message.from.let { VcsRoot.Revision(it) }
            val to = message.to?.let { VcsRoot.Revision(it) } ?: VcsRoot.Revision.Trunk
            if (message.path != null) root.diff(message.path, from, to)
            else root.diffAll(from, to)
        }.result

        return DiffResponse(contents = parseGitDiff(diffRes).map { it.asJsonable() })
    }

    fun getCompilerEnv() = FooBarCompiler.setupMyEnv(CompilerConfiguration())

    suspend fun getPsi(compiler: KotlinCoreEnvironment, request: ReadRequest): KtFile {
        val read = handleRead(request)
        val psiFactory = KtPsiFactory(compiler.project)

        return psiFactory.createFile(request.path, read.contents)
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

        val res = FooBarCompiler.useEnv { compiler ->
            val fromFile =
                    getPsi(compiler, ReadRequest(message.uid, message.location.filename.path, message.fromRevision))
            val toFile =
                    getPsi(compiler, ReadRequest(message.uid, message.location.filename.path, message.toRevision))

            val (function, resFunction) = findCorrespondingFunction(message.location, fromFile, toFile)
                    ?: return null

            (resFunction.location.start.thisLine() +
                    (message.location - function.location.start.thisLine()))
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

        val diffRes = run(ee) {
            val from = message.fromRevision.let { VcsRoot.Revision(it) }
            val to = message.toRevision.let { VcsRoot.Revision(it) }
            root.diff(message.location.filename.path, from, to)
        }.result

        return LocationResponse(location = message.location.applyDiffs(parseGitDiff(diffRes)))
    }

    @JsonableEventBusConsumerFor(Address.Code.LocationDiff)
    suspend fun handleLocation(message: LocationRequest): LocationResponse =
            // FIXME: this is generally, um, not good
            when {
                message.location.filename.path.endsWith("kt") -> handleLocationKotlin(message)
                        ?: handleLocationGeneral(message)
                else -> handleLocationGeneral(message)
            }
}
