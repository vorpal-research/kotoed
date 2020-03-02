package org.jetbrains.research.kotoed.util

import ru.spbstu.ktuples.zip
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType

val KType.parameterMapping: Map<String, KType?>
    get() = when(val classifier = classifier) {
        is KClass<*> -> {
            (classifier.typeParameters zip this.arguments).map { (p, a) ->
                require(a.variance == null || a.variance == KVariance.INVARIANT)
                p.name to a.type
            }.toMap()
        }
        else -> mapOf()
    }

fun KType.applyMapping(mapping: Map<String, KType?>): KType =
        when(val classifier = classifier) {
            null -> this
            is KTypeParameter -> {
                mapping[classifier.name] ?: this
            }
            else -> {
                classifier.createType(
                        nullable = isMarkedNullable,
                        annotations = annotations,
                        arguments = arguments.map {
                            it.copy(type = it.type?.applyMapping(mapping))
                        }
                )
            }
        }
