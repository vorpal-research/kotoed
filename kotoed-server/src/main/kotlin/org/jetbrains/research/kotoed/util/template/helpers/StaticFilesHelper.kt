package org.jetbrains.research.kotoed.util.template.helpers

import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.impl.FileResolver
import org.apache.commons.codec.digest.DigestUtils
import org.jetbrains.research.kotoed.util.Loggable
import org.jetbrains.research.kotoed.util.template.TemplateHelper
import java.io.Closeable
import java.io.FileInputStream
import java.io.OutputStreamWriter
import java.nio.file.Paths

class StaticFilesHelper(vertx: Vertx,
                        val staticWebBase: String = "/static",
                        staticLocalBase: String = "webroot/static",
                        val jsBundlePattern: String = "js/%s.bundle.js",
                        val cssBundlePattern: String = "css/%s.css"
                        ) : TemplateHelper, Loggable {
    /**
     * We calculate file hashes to drop clients' caches when these files are updated
     */
    private val staticHashes: Map<String, String>

    init {
        // XXX Using io.vertx.core.impl.FileResolver is a bit fucked up
        // but there is no other way to walk static files the way StaticHandler does
        val fr = FileResolver(vertx)
        try {
            val dir = fr.resolveFile(staticLocalBase)
            log.trace("Calculating static files hashes")
            log.trace("Static directory is $dir")
            staticHashes = dir.walk().
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
        } finally {
            fr.close { }
        }

    }

    fun staticPath(path: String, withHash: Boolean = true): String {
        val webPath = Paths.get(staticWebBase, path).toString()
        if (!withHash) {
            return webPath
        } else if (staticHashes.containsKey(path)) {
            return "$webPath?${staticHashes[path]}"
        } else {
            log.warn("Cannot find hash for path $webPath")
            return webPath
        }
    }

    private fun jsBundleName(bundle: String) = String.format(jsBundlePattern, bundle)

    private fun cssBundleName(bundle: String) = String.format(cssBundlePattern, bundle)

    fun jsBundlePath(bundle: String, withHash: Boolean = true) = staticPath(jsBundleName(bundle), withHash)

    fun cssBundlePath(bundle: String, withHash: Boolean = true) = staticPath(cssBundleName(bundle), withHash)

}