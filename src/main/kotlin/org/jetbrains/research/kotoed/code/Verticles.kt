package org.jetbrains.research.kotoed.code

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.RemovalNotification
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.launch
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

class CodeVerticle : AbstractVerticle(), Loggable {
    val dir by lazy {
        File(System.getProperty("user.dir"), Config.VCS.StoragePath)
    }
    val ee by lazy {
        newFixedThreadPoolContext(Config.VCS.PoolSize, "codeVerticle.${deploymentID()}.dispatcher")
    }
    val procs by lazy<Cache<RemoteRequest, RepositoryInfo>> {
        CacheBuilder
                .newBuilder()
                .maximumSize(Config.VCS.CloneCapacity)
                .expireAfterAccess(Config.VCS.CloneExpire, TimeUnit.MILLISECONDS)
                .removalListener(this::onCacheRemove)
                .build()
    }
    val info = mutableMapOf<String, RemoteRequest>()

    val RepositoryInfo.root get() = when (vcs) {
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

        val eb = vertx.eventBus()

        eb.consumer<JsonObject>(Address.Code.Ping, this@CodeVerticle::handlePing)
        eb.consumer<JsonObject>(Address.Code.Download, this@CodeVerticle::handleClone)
        eb.consumer<JsonObject>(Address.Code.Read, this@CodeVerticle::handleRead)
        eb.consumer<JsonObject>(Address.Code.List, this@CodeVerticle::handleList)
        eb.consumer<JsonObject>(Address.Code.Diff, this@CodeVerticle::handleDiff)
        eb.consumer<JsonObject>(Address.Code.LocationDiff, this@CodeVerticle::handleLocation)

        startFuture.complete(null)
    }

    override fun stop() = launch {
        procs.cleanUp()
        val fs = vertx.fileSystem()
        if (dir.exists()) fs.deleteRecursiveAsync(dir.absolutePath)
    }

    fun handleRead(mes: Message<JsonObject>): Unit =
            launch(UnconfinedWithExceptions(mes)) {
                val message: ReadRequest = fromJson(mes.body())

                log.info("Requested read: $message")

                UUID.fromString(message.uid)

                val path = message.path

                val inf = info[message.uid] ?: throw IllegalArgumentException("Repository not found")

                val root = procs[inf]?.root ?: throw IllegalArgumentException("Inconsistent repo state")

                val catRes = run(ee) {
                    val rev = message.revision?.let { VcsRoot.Revision.Id(it) } ?: VcsRoot.Revision.Trunk
                    root.cat(path, rev)
                }

                val response = ReadResponse(
                        success = catRes is VcsResult.Success,
                        errors = (catRes as? VcsResult.Failure)?.run { output.toList() }.orEmpty(),
                        contents = (catRes as? VcsResult.Success)?.run { v.joinToString("\n") }.orEmpty()
                )

                mes.reply(response.toJson())

            }.ignore()

    fun handleList(mes: Message<JsonObject>): Unit =
            launch(UnconfinedWithExceptions(mes)) {
                val message: ListRequest = fromJson(mes.body())

                log.info("Requested list: $message")

                UUID.fromString(message.uid)

                val inf = info[message.uid] ?: throw IllegalArgumentException("Repository not found")

                val root = procs[inf]?.root ?: throw IllegalArgumentException("Inconsistent repo state")

                val catRes = run(ee) {
                    val rev = message.revision?.let { VcsRoot.Revision.Id(it) } ?: VcsRoot.Revision.Trunk
                    root.ls(rev)
                }

                val response = ListResponse(
                        success = catRes is VcsResult.Success,
                        errors = (catRes as? VcsResult.Failure)?.run { output.toList() }.orEmpty(),
                        files = (catRes as? VcsResult.Success)?.run { v.toList() }.orEmpty()
                )

                mes.reply(response.toJson())

            }.ignore()

    fun handlePing(mes: Message<JsonObject>): Unit =
            launch(UnconfinedWithExceptions(mes)) {
                val message: RemoteRequest = fromJson(mes.body())
                log.info("Pinging repository: $message")

                val url = message.url
                val pendingResp = RepositoryInfo(
                        status = CloneStatus.pending,
                        uid = "",
                        url = url,
                        vcs = message.vcs
                )

                val exists = run(ee) { pendingResp.root.ping() }

                val response = PingResponse(success = exists is VcsResult.Success)

                mes.reply(response.toJson())
            }.ignore()

    fun handleClone(mes: Message<JsonObject>): Unit =
            launch(UnconfinedWithExceptions(mes)) {
                val message: RemoteRequest = fromJson(mes.body())

                log.info("Requested clone: $message")

                val url = message.url
                if (message in procs) {
                    log.info("Cloning request for $url: repository already cloned")
                    mes.reply(procs[message]?.toJson())
                    return@launch
                }

                val uid = UUID.randomUUID()
                val randomName = File(dir, "$uid")
                log.info("Generated uid: $uid")
                log.info("Using directory: $randomName")

                vertx.executeBlockingAsync(ordered = false) {
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

                vertx.timedOut(Config.VCS.PendingTimeout) {

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
                                        status = CloneStatus.done,
                                        success = res is VcsResult.Success,
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

            }.ignore()

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

    fun handleDiff(mes: Message<JsonObject>): Unit =
            launch(UnconfinedWithExceptions(mes)) {
                val message: DiffRequest = fromJson(mes.body())

                log.info("Requested diff: $message")

                UUID.fromString(message.uid)

                val inf = info[message.uid] ?: throw IllegalArgumentException("Repository not found")

                val root = procs[inf]?.root ?: throw IllegalArgumentException("Inconsistent repo state")

                val diffRes = run(ee) {
                    val from = message.from.let { VcsRoot.Revision.Id(it) }
                    val to = message.to?.let { VcsRoot.Revision.Id(it) } ?: VcsRoot.Revision.Trunk
                    if (message.path != null) root.diff(message.path, from, to)
                    else root.diffAll(from, to)
                }

                val response = DiffResponse(
                        success = diffRes is VcsResult.Success,
                        errors = (diffRes as? VcsResult.Failure)?.run { output.toList() }.orEmpty(),
                        contents = (diffRes as? VcsResult.Success)?.run {
                            parseDiff(v).map { it.toJson() }.toList()
                        }.orEmpty()
                )

                mes.reply(response.toJson())

            }.ignore()

    fun handleLocation(mes: Message<JsonObject>): Unit =
            launch(UnconfinedWithExceptions(mes)) {
                val message: LocationRequest = mes.body().toJsonable()

                log.info("Requested location adjustment: $message")

                UUID.fromString(message.uid)

                val inf = info[message.uid] ?: throw IllegalArgumentException("Repository not found")

                val root = procs[inf]?.root ?: throw IllegalArgumentException("Inconsistent repo state")

                val diffRes = run(ee) {
                    val from = message.from.let { VcsRoot.Revision.Id(it) }
                    val to = message.to.let { VcsRoot.Revision.Id(it) }
                    root.diffAll(from, to)
                }

                val response = LocationResponse(
                        success = diffRes is VcsResult.Success,
                        errors = (diffRes as? VcsResult.Failure)?.run { output.toList() }.orEmpty(),
                        location = (diffRes as? VcsResult.Success)?.run {
                            message.loc.applyDiffs(parseDiff(v))
                        } ?: Location.Unknown
                )

                mes.reply(response.toJson())

            }.ignore()

}
