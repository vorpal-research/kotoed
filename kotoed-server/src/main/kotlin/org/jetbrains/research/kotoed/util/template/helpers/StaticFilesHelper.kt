package org.jetbrains.research.kotoed.util.template.helpers

import io.vertx.core.Vertx
import io.vertx.core.impl.FileResolver
import org.apache.commons.codec.digest.DigestUtils
import org.jetbrains.research.kotoed.util.Loggable
import org.jetbrains.research.kotoed.util.template.TemplateHelper
import java.io.FileInputStream
import java.nio.file.Paths

class StaticFilesHelper(vertx: Vertx,
                        val staticWebBase: String = "static",
                        staticLocalBase: String = "webroot/static",
                        val bundlePattern: String = "js/%s.bundle.js") : TemplateHelper, Loggable {
    /**
     * We calculate file hashes to drop clients' caches when these files are updated
     */
    private val staticHashes: Map<String, String>

    init {
        // XXX Using io.vertx.core.impl.FileResolver is a bit fucked up
        // but there is no other way to walk static files the way StaticHandler does
        val dir = FileResolver(vertx).resolveFile(staticLocalBase)
        log.trace("Calculating static files hashes")
        log.trace("Static directory is $dir")
        staticHashes =  dir.walk().
                filter { it.isFile }.
                map { "${dir.toPath().relativize(it.toPath())}" to FileInputStream(it) }.
                map { (path, fis) ->
                    fis.use {
                        log.trace("Calculating hash for $path")
                        val hash = DigestUtils.sha1Hex(it)
                        log.trace("Hash is $hash")
                        path to hash
                    }
                }.toMap()

    }

    fun staticPath(path: String): String = Paths.get(staticWebBase, path).toString()

    fun staticPathWithHash(path: String) =
            if (staticHashes.containsKey(path))
                "${staticPath(path)}?${staticHashes[path]}"
            else staticPath(path).also {
                log.warn("Cannot find hash for path $path")
            }

    private fun jsBundleName(bundle: String) = String.format(bundlePattern, bundle)

    fun bundlePath(bundle: String) = staticPath(jsBundleName(bundle))

    fun bundlePathWithHash(bundle: String) = staticPathWithHash(jsBundleName(bundle))
}