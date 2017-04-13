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

import freemarker.template.Configuration
import freemarker.template.Template
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.templ.FreeMarkerTemplateEngine
import io.vertx.ext.web.templ.impl.CachingTemplateEngine
import io.vertx.ext.web.templ.impl.VertxWebObjectWrapper
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter

class FreeMarkerTemplateEngineImplEx :
        CachingTemplateEngine<Template>(
                FreeMarkerTemplateEngine.DEFAULT_TEMPLATE_EXTENSION,
                FreeMarkerTemplateEngine.DEFAULT_MAX_CACHE_SIZE
        ), FreeMarkerTemplateEngine {

    private val config: Configuration = Configuration(Configuration.VERSION_2_3_22)
    private val loader: FreeMarkerTemplateLoaderEx = FreeMarkerTemplateLoaderEx()

    init {
        config.objectWrapper = VertxWebObjectWrapper(config.incompatibleImprovements)
        config.templateLoader = loader
    }

    override fun setExtension(extension: String): FreeMarkerTemplateEngine {
        doSetExtension(extension)
        return this
    }

    override fun setMaxCacheSize(maxCacheSize: Int): FreeMarkerTemplateEngine {
        this.cache.setMaxSize(maxCacheSize)
        return this
    }

    override fun render(
            context: RoutingContext,
            templateFileName: String,
            handler: Handler<AsyncResult<Buffer>>) = TODO("Use FreeMarkerTemplateEngineImpl instead")

    fun render(
            vertx: Vertx,
            templateFileName: String,
            variables: Map<String, Any?>,
            handler: Handler<AsyncResult<Buffer>>) {
        try {
            var template = cache[templateFileName]
            if (template == null) {
                // real compile
                synchronized(this) {
                    loader.setVertx(vertx)
                    // compile
                    template = config.getTemplate(adjustLocation(templateFileName))
                }
                cache.put(templateFileName, template!!)
            }

            ByteArrayOutputStream().use { baos ->
                template!!.process(variables, OutputStreamWriter(baos))
                handler.handle(Future.succeededFuture(Buffer.buffer(baos.toByteArray())))
            }

        } catch (ex: Exception) {
            handler.handle(Future.failedFuture<Buffer>(ex))
        }
    }
}
