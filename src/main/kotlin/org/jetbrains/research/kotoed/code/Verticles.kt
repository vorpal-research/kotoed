package org.jetbrains.research.kotoed.code

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.RemovalNotification
import io.vertx.core.AbstractVerticle
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
import org.jetbrains.research.kotoed.coroutines.launch
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.wickedsource.diffparser.api.UnifiedDiffParser
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class CodeVerticle: AbstractVerticle(), Loggable {
    val dir by lazy {
        File(System.getProperty("user.dir"), Config.VCS.StoragePath)
    }
    val ee by lazy {
        newFixedThreadPoolContext(Config.VCS.PoolSize, "codeVerticle.${deploymentID()}.dispatcher")
    }
    val procs by lazy<Cache<CloneRequest, RepositoryInfo>> {
        CacheBuilder
                .newBuilder()
                .maximumSize(Config.VCS.CloneCapacity.toLong())
                .expireAfterAccess(Config.VCS.CloneExpire, TimeUnit.MILLISECONDS)
                .removalListener(this::onCacheRemove)
                .build()
    }
    val info = mutableMapOf<String, CloneRequest>()

    fun onCacheRemove(removalNotification: RemovalNotification<CloneRequest, RepositoryInfo>) =
            launch {
                if(removalNotification.value.status == CloneStatus.pending) return@launch

                vertx.goToEventLoop()
                val fs = vertx.fileSystem()
                val uid = removalNotification.value.uid
                info.remove(uid)
                val cat = File(dir, uid)
                log.info("The entry for repository $uid expired, deleting the files...")
                if(cat.exists()) fs.deleteRecursiveAsync(cat.absolutePath)
                log.info("Repository $uid deleted")
            }

    override fun start() = launch {

        procs.cleanUp()
        val fs = vertx.fileSystem()
        if(dir.exists()) fs.deleteRecursiveAsync(dir.absolutePath)

        val eb = vertx.eventBus()

        eb.consumer<JsonObject>(Address.Code.Download, this@CodeVerticle::handleClone)
        eb.consumer<JsonObject>(Address.Code.Read, this@CodeVerticle::handleRead)
        eb.consumer<JsonObject>(Address.Code.List, this@CodeVerticle::handleList)
        eb.consumer<JsonObject>(Address.Code.Diff, this@CodeVerticle::handleDiff)
    }

    override fun stop() = launch {
        procs.cleanUp()
        val fs = vertx.fileSystem()
        fs.deleteRecursiveAsync(dir.absolutePath)

        super.stop()
    }

    enum class VCS{ git, mercurial }
    enum class CloneStatus{ done, pending }

    data class CloneRequest(val vcs: VCS, val url: String): Jsonable
    data class RepositoryInfo(
        val status: CloneStatus,
        val uid: String,
        val url: String,
        val type: VCS,
        val success: Boolean = true,
        val errors: List<String> = listOf()
    ): Jsonable

    val RepositoryInfo.root get() = when(type) {
        VCS.git -> Git(url, File(dir, uid).absolutePath)
        VCS.mercurial -> Mercurial(url, File(dir, uid).absolutePath)
    }

    data class ReadRequest(val uid: String, val path: String, val revision: String?): Jsonable
    data class ReadResponse(
            val success: Boolean = true,
            val contents: String,
            val errors: List<String>
    ): Jsonable

    data class ListRequest(val uid: String, val revision: String?): Jsonable
    data class ListResponse(
            val success: Boolean = true,
            val files: List<String>,
            val errors: List<String>
    ): Jsonable

    fun handleRead(mes: Message<JsonObject>) =
            launch {
                val message: ReadRequest = fromJson(mes.body())

                log.info("Requested read: $message")

                try{ UUID.fromString(message.uid) }
                catch (ex: Exception) {
                    mes.reply(ReadResponse(false, "", listOf("Illegal path")).toJson())
                    return@launch
                }

                val path = message.path

                val inf = info[message.uid]
                if(inf == null) {
                    mes.reply(ReadResponse(false, "", listOf("Repository not found")).toJson())
                    return@launch
                }

                val root = procs[inf]?.root

                if(root == null) {
                    mes.reply(ReadResponse(false, "", listOf("Unknown error")).toJson())
                    return@launch
                }

                val catRes = run(ee) {
                    val rev = message.revision?.let { VcsRoot.Revision.Id(it) } ?: VcsRoot.Revision.Trunk
                    root.cat(path, rev)
                }

                val response = ReadResponse(
                        success = catRes is VcsResult.Success,
                        errors = (catRes as? VcsResult.Failure)?.output?.toList().orEmpty(),
                        contents = (catRes as? VcsResult.Success)?.v?.joinToString("\n").orEmpty()
                )

                mes.reply(response.toJson())
            }

    fun handleList(mes: Message<JsonObject>) =
            launch {
                val message: ListRequest = fromJson(mes.body())

                log.info("Requested list: $message")

                try{ UUID.fromString(message.uid) }
                catch (ex: Exception) {
                    mes.reply(ReadResponse(false, "", listOf("Illegal path")).toJson())
                    return@launch
                }

                val inf = info[message.uid]
                if(inf == null) {
                    mes.reply(ReadResponse(false, "", listOf("Repository not found")).toJson())
                    return@launch
                }

                val root = procs[inf]?.root

                if(root == null) {
                    mes.reply(ListResponse(false, listOf(), listOf("Unknown error")).toJson())
                    return@launch
                }

                val catRes = run(ee) {
                    val rev = message.revision?.let { VcsRoot.Revision.Id(it) } ?: VcsRoot.Revision.Trunk
                    root.ls(rev)
                }

                val response = ListResponse(
                        success = catRes is VcsResult.Success,
                        errors = (catRes as? VcsResult.Failure)?.output?.toList().orEmpty(),
                        files = (catRes as? VcsResult.Success)?.v?.toList().orEmpty()
                )

                mes.reply(response.toJson())
            }

    fun handleClone(mes: Message<JsonObject>) =
        launch {
            val message: CloneRequest = fromJson(mes.body())

            log.info("Requested clone for $message")

            val url = message.url
            if(message in procs) {
                log.info("Cloning request for $url: repository already cloned")
                mes.reply(procs[message]?.toJson())
                return@launch
            }

            val uid = UUID.randomUUID();
            val randomName = File(dir, "$uid")
            log.info("Generated uid: $uid")
            log.info("Using directory: $randomName")

            vertx.executeBlockingAsync(ordered = false) {
                randomName.mkdirs()
            }

            val pendingResp = RepositoryInfo(status = CloneStatus.pending, uid = "$uid", url = url, type = message.vcs)
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
                                    errors = (res as? VcsResult.Failure)?.output?.toList().orEmpty()
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

    data class DiffRequest(val uid: String, val from: String, val to: String? = null, val path: String? = null): Jsonable
    data class DiffResponse(
            val success: Boolean = true,
            val contents: List<JsonObject>,
            val errors: List<String>
    ): Jsonable

    private fun parseDiff(diffOutput: Sequence<String>): List<JsonObject> {

        log.info(diffOutput.joinToString("\n"))

        val divided = diffOutput.splitBy { it.startsWith("diff") }.filterNot { it.isEmpty() }

        val res = divided.map {
            log.info("Parsing:")
            log.info(it.joinToString("\n"))
            val parser = UnifiedDiffParser()
            val pres = parser.parse(it.asSequence().linesAsCharSequence().asInputStream())
            log.warn(pres.map { it.toJson() })
            pres.firstOrNull()
        }

        return res.map { diff -> diff?.toJson() ?: JsonObject() }.toList()
    }

    fun handleDiff(mes: Message<JsonObject>) =
            launch {
                val message: DiffRequest = fromJson(mes.body())

                log.info("Requested read: $message")

                try{ UUID.fromString(message.uid) }
                catch (ex: Exception) {
                    mes.reply(DiffResponse(false, listOf(), listOf("Illegal path")).toJson())
                    return@launch
                }

                val inf = info[message.uid]
                if(inf == null) {
                    mes.reply(DiffResponse(false, listOf(), listOf("Repository not found")).toJson())
                    return@launch
                }

                val root = procs[inf]?.root

                if(root == null) {
                    mes.reply(DiffResponse(false, listOf(), listOf("Unknown error")).toJson())
                    return@launch
                }

                val diffRes = run(ee) {
                    val from = message.from.let { VcsRoot.Revision.Id(it) }
                    val to = message.to?.let { VcsRoot.Revision.Id(it) } ?: VcsRoot.Revision.Trunk
                    if(message.path != null) root.diff(message.path, from, to)
                    else root.diffAll(from, to)
                }

                val response = DiffResponse(
                        success = diffRes is VcsResult.Success,
                        errors = (diffRes as? VcsResult.Failure)?.output?.toList().orEmpty(),
                        contents = (diffRes as? VcsResult.Success)?.v?.let{ parseDiff(it) }.orEmpty()
                )

                mes.reply(response.toJson())
            }

}
