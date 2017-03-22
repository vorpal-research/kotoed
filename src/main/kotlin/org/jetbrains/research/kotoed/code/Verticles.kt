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
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.coroutines.launch
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.vcsreader.VcsProject
import org.vcsreader.VcsRoot
import org.vcsreader.vcs.git.GitVcsRoot
import org.vcsreader.vcs.hg.HgVcsRoot
import org.vcsreader.vcs.svn.SvnVcsRoot
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
    val procs by lazy<Cache<CloneRequest, CloneResponse>> {
        CacheBuilder
                .newBuilder()
                .maximumSize(Config.VCS.CloneCapacity.toLong())
                .expireAfterAccess(Config.VCS.CloneExpire, TimeUnit.MILLISECONDS)
                .removalListener(this::onCacheRemove)
                .build()
    }

    fun onCacheRemove(removalNotification: RemovalNotification<CloneRequest, CloneResponse>) =
            launch {
                if(removalNotification.value.status == CloneStatus.pending) return@launch

                vertx.goToEventLoop()
                val fs = vertx.fileSystem()
                val uid = removalNotification.value.uid
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
    }

    override fun stop() = launch {
        procs.cleanUp()
        val fs = vertx.fileSystem()
        fs.deleteRecursiveAsync(dir.absolutePath)

        super.stop()
    }

    enum class VCS{ git, mercurial, svn }
    enum class CloneStatus{ done, pending }

    data class CloneRequest(val vcs: VCS, val url: String): Jsonable
    data class CloneResponse(
        val status: CloneStatus,
        val uid: String,
        val url: String,
        val success: Boolean = true,
        val errors: List<String> = listOf(),
        val exceptions: List<String>  = listOf()
    ): Jsonable

    data class ReadRequest(val uid: String, val path: String): Jsonable
    data class ReadResponse(
            val success: Boolean = true,
            val contents: String,
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

                val catalog = File(dir, message.uid).canonicalFile
                val canPath = File(catalog, message.path).canonicalFile
                if(!canPath.exists() || !canPath.isFile ) {
                    mes.reply(ReadResponse(false, "", listOf("File not found")).toJson())
                    return@launch
                }
                if(!canPath.canRead() || !canPath.path.startsWith(catalog.path)) {
                    mes.reply(ReadResponse(false, "", listOf("Permission denied")).toJson())
                    return@launch
                }

                val fs = vertx.fileSystem()
                val buf = fs.readFileAsync(canPath.absolutePath)
                mes.reply(ReadResponse(true, buf.toString(UTF_8), listOf()).toJson())
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

            val pendingResp = CloneResponse(status = CloneStatus.pending, uid = "$uid", url = url)
            procs[message] = pendingResp

            val root: VcsRoot =
                    when(message.vcs) {
                        VCS.git -> GitVcsRoot(randomName.absolutePath, url)
                        VCS.mercurial -> HgVcsRoot(randomName.absolutePath, url)
                        VCS.svn -> SvnVcsRoot(url)
                    }

            vertx.timedOut(Config.VCS.PendingTimeout) {
                run {

                    val res = run(ee) {
                        VcsProject(root).cloneToLocal().also {
                            log.info("Cloning finished for $url in directory $randomName")
                        }
                    }
                    vertx.goToEventLoop()

                    log.info("Cloning request for $url successful")
                    val resp =
                            pendingResp.copy(
                                    status = CloneStatus.done,
                                    success = res.isSuccessful,
                                    errors = res.vcsErrors(),
                                    exceptions = res.exceptions().map{ it.message ?: "" }
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
