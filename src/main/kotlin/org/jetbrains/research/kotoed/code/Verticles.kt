package org.jetbrains.research.kotoed.code

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.RemovalNotification
import com.hazelcast.nio.Bits.UTF_8
import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.run
import org.jetbrains.research.kotoed.code.vcs.Git
import org.jetbrains.research.kotoed.code.vcs.Mercurial
import org.jetbrains.research.kotoed.code.vcs.VcsResult
import org.jetbrains.research.kotoed.code.vcs.VcsRoot
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.coroutines.launch
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
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

//            var timedOut = false
//
//            val timerID = vertx.setTimer(Config.VCS.PendingTimeout) {
//                timedOut = true
//
//            }
//
//            val res = run(ee){
//                VcsProject(root).cloneToLocal().also {
//                    log.info("Cloning finished for $url in directory $randomName")
//                }
//            }
//
//            vertx.goToEventLoop()
//            vertx.cancelTimer(timerID)
//            // there is no race here, as both handlers are guaranteed to run in event loop
//            log.info("Cloning request for $url successful")
//            val resp =
//                    procs[message]!!.copy(
//                            status = CloneStatus.done,
//                            success = res.isSuccessful,
//                            errors = res.vcsErrors(),
//                            exceptions = res.exceptions().map{ it.message ?: "" }
//                    )
//
//            procs[message] = resp
//
//            if(!timedOut)  mes.reply(resp.toJson())
        }.ignore()

    data class DiffRequest(val from: String, val to: String): Jsonable

    fun handleDiff(mes: Message<JsonObject>) {

    }

}
