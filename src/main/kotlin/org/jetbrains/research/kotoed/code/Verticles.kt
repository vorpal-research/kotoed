package org.jetbrains.research.kotoed.code

import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.vcsreader.VcsProject
import org.vcsreader.VcsRoot
import org.vcsreader.vcs.git.GitVcsRoot
import org.vcsreader.vcs.hg.HgVcsRoot
import org.vcsreader.vcs.svn.SvnVcsRoot
import java.util.*

class CodeVerticle: AbstractVerticle() {
    override fun start() {
        val eb = vertx.eventBus()

        eb.consumer<JsonObject>(Address.Code.Download, this::handleDownload)
    }

    enum class VCS{ git, mercurial, svn }
    data class CloneRequest(val vcs: VCS, val url: String): Jsonable

    fun handleDownload(mes: Message<JsonObject>) {
        val message: CloneRequest = fromJson(mes.body())
        val fs = vertx.fileSystem()

        val randomName = System.getProperty("user.dir") + "/vcs/" + UUID.randomUUID()

        launch(Unconfined) {
            vxu { fs.mkdir(randomName, it)  }

            val root: VcsRoot =
                    when(message.vcs) {
                        VCS.git -> GitVcsRoot(randomName, message.url)
                        VCS.mercurial -> HgVcsRoot(randomName, message.url)
                        VCS.svn -> SvnVcsRoot(message.url)
                    }

            val res = vertx.executeBlockingAsync(ordered = false) {
                VcsProject(root).cloneToLocal()
            }

            vertx.goToEventLoop()

            val resp = object: Jsonable {
                val success = res.isSuccessful
                val path = randomName
                val errors = res.vcsErrors()
                val exceptions = res.exceptions().map { it.message }
            }

            mes.reply(resp.toJson())
        }


    }
}
