package org.jetbrains.research.kotoed.util.template.helpers

import org.jetbrains.research.kotoed.util.Loggable
import org.jetbrains.research.kotoed.util.template.TemplateHelper
import org.jetbrains.research.kotoed.web.UrlPattern
import java.lang.reflect.Modifier

/**
 * This ugly class is intended to be accessible from templates with nice syntax
 */
class KotoedUrlHelper : TemplateHelper, Loggable {
    @JvmField  // To allow easy access from template
    val patterns: Map <String, Any>


    // This is super fucked up. But it's the only way to access UrlPattern-like structure from Jade
    private fun clazzToMap(clazz: Class<*>): Map<String, Any> {
        val map = mutableMapOf<String, Any>()

        clazz.declaredFields
                .filter { Modifier.isStatic(it.modifiers) }
                .filter { String::class.java.isAssignableFrom(it.type) }
                .forEach { map[it.name] = it.get(null) }

        clazz.declaredClasses
                .filter { Modifier.isStatic(it.modifiers) }
                .forEach{ map[it.simpleName] = clazzToMap(it) }

        return map
    }

    init {
        patterns = clazzToMap(UrlPattern.javaClass)
    }

    fun reverse(pattern: String, params: Map<String, Any>, star: Any) = UrlPattern.reverse(pattern, params, star)
}