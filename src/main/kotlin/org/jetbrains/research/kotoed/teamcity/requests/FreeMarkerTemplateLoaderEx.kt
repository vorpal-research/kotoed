/*
 * Copyright 2016 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */
package org.jetbrains.research.kotoed.teamcity.requests

import freemarker.cache.TemplateLoader
import io.vertx.core.Vertx
import java.io.IOException
import java.io.Reader
import java.io.StringReader

internal class FreeMarkerTemplateLoaderEx : TemplateLoader {

    @Transient private var vertx: Vertx? = null

    fun setVertx(vertx: Vertx) {
        this.vertx = vertx
    }

    @Throws(IOException::class)
    override fun findTemplateSource(name: String): Any? {
        try {
            // check if exists on file system
            if (vertx!!.fileSystem().existsBlocking(name)) {
                val buff = vertx!!.fileSystem().readFileBlocking(name)
                return StringTemplateSource(name, buff.toString(), System.currentTimeMillis())
            } else {
                return null
            }
        } catch (e: Exception) {
            throw IOException(e)
        }
    }

    override fun getLastModified(templateSource: Any): Long {
        return (templateSource as StringTemplateSource).lastModified
    }

    @Throws(IOException::class)
    override fun getReader(templateSource: Any, encoding: String): Reader {
        return StringReader((templateSource as StringTemplateSource).source)
    }

    @Throws(IOException::class)
    override fun closeTemplateSource(templateSource: Any) {
        // ...
    }

    internal class StringTemplateSource internal constructor(
            internal val name: String,
            internal val source: String,
            internal val lastModified: Long) {

        override fun equals(other: Any?): Boolean {
            return other is StringTemplateSource && name == other.name
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }
    }
}
