package org.jetbrains.research.kotoed.code

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.RemovalNotification
import io.vertx.core.Future
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.run
import org.jetbrains.research.kotoed.code.diff.toJson
import org.jetbrains.research.kotoed.code.vcs.Git
import org.jetbrains.research.kotoed.code.vcs.Mercurial
import org.jetbrains.research.kotoed.code.vcs.VcsResult
import org.jetbrains.research.kotoed.code.vcs.VcsRoot
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.data.vcs.*
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
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
        newFixedThreadPoolContext(Config.VCS.PoolSize, "codeVerticle.${deploymentID()}.dispatcher")
    }
    private val procs by lazy<Cache<RemoteRequest, RepositoryInfo>> {
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
    }

    fun onCacheRemove(removalNotification: RemovalNotification<RemoteRequest, RepositoryInfo>) =
            launch {
                if (removalNotification.value.status == CloneStatus.pending) return@launch

                vertx.goToEventLoop()
                val fs = vertx.fileSystem()
                val uid = removalNotification.value.uid
                info.remove(uid)
                val cat = File(dir, uid)
                log.info("The entry for repository $uid expired, deleting the files...")
                if (cat.exists()) fs.deleteRecursiveAsync(cat.absolutePath)
                log.info("Repository $uid deleted")
            }

    override fun start(startFuture: Future<Void>) = launch {
        val fs = vertx.fileSystem()
        if (dir.exists()) fs.deleteRecursiveAsync(dir.absolutePath)
        dir.mkdir()
        super.start(startFuture)
    }

    override fun stop(stopFuture: Future<Void>) = launch {
        procs.cleanUp()
        val fs = vertx.fileSystem()
        if (dir.exists()) fs.deleteRecursiveAsync(dir.absolutePath)
        super.stop(stopFuture)
    }

    private val<T> VcsResult<T>.result
        get() = when(this) {
            is VcsResult.Success -> v
            is VcsResult.Failure -> throw VcsException(output.toList())
        }

    @JsonableEventBusConsumerFor(Address.Code.Read)
    suspend fun handleRead(message: ReadRequest): ReadResponse {
        log.info("Requested read: $message")

        UUID.fromString(message.uid)

        val path = message.path

        val inf = expectNotNull(info[message.uid], "Repository not found")
        val root = expectNotNull(procs[inf], "Inconsistent repo state").root

        val catRes = run(ee) {
            val rev = message.revision?.let { VcsRoot.Revision.Id(it) } ?: VcsRoot.Revision.Trunk
            root.cat(path, rev)
        }.result

        return ReadResponse(contents = catRes.joinToString("\n"))
    }

    @JsonableEventBusConsumerFor(Address.Code.List)
    suspend fun handleList(message: ListRequest): ListResponse {
        log.info("Requested list: $message")

        UUID.fromString(message.uid)

        val inf = expectNotNull(info[message.uid], "Repository not found")

        val root = expectNotNull(procs[inf], "Inconsistent repo state").root

        val lsRes = run(ee) {
            val rev = message.revision?.let { VcsRoot.Revision.Id(it) } ?: VcsRoot.Revision.Trunk
            root.ls(rev)
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
                vcs = message.vcs
        )

        val vcsRes = run(ee) { pendingResp.root.ping() }

        return PingResponse(vcsRes is VcsResult.Success)
    }

    @EventBusConsumerFor(Address.Code.Download)
    suspend fun handleClone(mes: Message<JsonObject>) {
        val message: RemoteRequest = fromJson(mes.body())

        log.info("Requested clone: $message")

        val url = message.url
        if (message in procs) {
            log.info("Cloning request for $url: repository already cloned")
            mes.reply(procs[message]?.toJson())
            return
        }

        val uid = UUID.randomUUID()
        val randomName = File(dir, "$uid")
        log.info("Generated uid: $uid")
        log.info("Using directory: $randomName")

        run(ee) {
            randomName.mkdirs()
        }

        val pendingResp = RepositoryInfo(
                status = CloneStatus.pending,
                uid = "$uid",
                url = url,
                vcs = message.vcs
        )
        procs[message] = pendingResp
        info["$uid"] = message

        val root = pendingResp.root

        vertx.timedOut(Config.VCS.PendingTimeout, timeoutCtx = UnconfinedWithExceptions(mes)) {
            run {
                val res = run(ee) {
                    root.clone().also {
                        log.info("Cloning finished for $url in directory $randomName")
                    }
                }
                vertx.goToEventLoop()

                log.info("Cloning request for $url successful")

                val resp =
                        pendingResp.copy(
                                status = if(res is VcsResult.Success) CloneStatus.done else CloneStatus.failed,
                                errors = (res as? VcsResult.Failure)?.run { output.toList() }.orEmpty()
                        )

                procs[message] = resp
            }

            onTimeout {
                mes.reply(procs[message]?.toJson())
                log.info("Cloning request for $url timed out: sending 'pending' reply")
            }

            onSuccess {
                mes.reply(procs[message]?.toJson())
                log.info("Cloning request for $url timed out: sending 'successful' reply")
            }
        }

    }

    private fun parseDiff(diffOutput: Sequence<String>): Sequence<Diff> {

        log.info(diffOutput.joinToString("\n"))

        val divided = diffOutput.splitBy { it.startsWith("diff") }.filterNot { it.isEmpty() }

        val res = divided.map {
            log.info("Parsing:")
            log.info(it.joinToString("\n"))
            val parser = UnifiedDiffParser()
            val pres = parser.parse(it.asSequence().linesAsCharSequence().asInputStream())
            log.trace(pres.map { it.toJson() })
            pres.firstOrNull()
        }

        return res.filterNotNull()
    }

    @JsonableEventBusConsumerFor(Address.Code.Diff)
    suspend fun handleDiff(message: DiffRequest): DiffResponse {
        log.info("Requested diff: $message")

        UUID.fromString(message.uid)

        val inf = expectNotNull(info[message.uid], "Repository not found")
        val root = expectNotNull(procs[inf], "Inconsistent repo state").root

        val diffRes = run(ee) {
            val from = message.from.let { VcsRoot.Revision.Id(it) }
            val to = message.to?.let { VcsRoot.Revision.Id(it) } ?: VcsRoot.Revision.Trunk
            if (message.path != null) root.diff(message.path, from, to)
            else root.diffAll(from, to)
        }.result

        return DiffResponse(contents = parseDiff(diffRes).map { it.toJson() }.toList())
    }

    @JsonableEventBusConsumerFor(Address.Code.LocationDiff)
    suspend fun handleLocation(message: LocationRequest): LocationResponse {
        log.info("Requested location adjustment: $message")

        UUID.fromString(message.uid)

        val inf = expectNotNull(info[message.uid], "Repository not found")

        val root = expectNotNull(procs[inf], "Inconsistent repo state").root

        val diffRes = run(ee) {
            val from = message.fromRevision.let { VcsRoot.Revision.Id(it) }
            val to = message.toRevision.let { VcsRoot.Revision.Id(it) }
            root.diffAll(from, to)
        }.result

        return LocationResponse(location = message.location.applyDiffs(parseDiff(diffRes)))
    }
}
