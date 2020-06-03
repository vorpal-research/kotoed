import com.google.common.base.CaseFormat
import io.vertx.core.Promise
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import kotlinx.warnings.Warnings
import org.jetbrains.research.kotoed.data.api.DbRecordWrapper
import org.jetbrains.research.kotoed.data.api.VerificationData
import org.jetbrains.research.kotoed.database.tables.records.CourseRecord
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*
import org.jetbrains.research.kotoed.web.UrlPattern
import org.jooq.Record
import org.jooq.TableRecord
import org.reflections.ReflectionUtils
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import org.reflections.scanners.SubTypesScanner
import org.reflections.scanners.TypeAnnotationsScanner
import java.io.PrintWriter
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import java.time.temporal.Temporal
import kotlin.coroutines.Continuation
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure

fun Type.isSubclassOf(other: Class<*>) = this is Class<*> && other.isAssignableFrom(this)
inline fun <reified T> Type.isSubclassOf() = isSubclassOf(T::class.java)

class AppendScope(val appendable: Appendable, val indent: Int = 0) {
    inline fun indent(indent: Int = 4, body: AppendScope.() -> Unit) {
        AppendScope(appendable, indent + this.indent).body()
    }

    operator fun String.unaryPlus() {
        appendable.appendln(this.trimIndent().prependIndent(" ".repeat(indent)))
    }
}

inline fun formatTo(appendable: Appendable, body: AppendScope.() -> Unit) {
    AppendScope(appendable).body()
}

inline fun AppendScope.appendln(value: CharSequence) = appendable.appendln(value)

private val numberClasses = setOf(
    java.lang.Integer.TYPE,
    java.lang.Integer::class.java,
    java.lang.Short.TYPE,
    java.lang.Short::class.java,
    java.lang.Long.TYPE,
    java.lang.Long::class.java,
    java.lang.Byte.TYPE,
    java.lang.Byte::class.java,
    java.lang.Float.TYPE,
    java.lang.Float::class.java,
    java.lang.Double.TYPE,
    java.lang.Double::class.java
)

enum class Purpose { ForInput, ForOutput }

fun classToTS(clazz: Type, purpose: Purpose, visited: MutableSet<Type> = mutableSetOf()): String {
    fun recurse(clazz: Type) = classToTS(clazz, purpose, visited)
    return when {
        clazz in visited -> "any"
        clazz in numberClasses || clazz.isSubclassOf<Temporal>() -> "number"
        clazz.isSubclassOf<CharSequence>() -> "string"
        clazz == java.lang.Boolean.TYPE || clazz.isSubclassOf<Boolean>() -> "boolean"
        clazz == kotlin.Any::class.java -> "any"
        clazz == kotlin.Unit::class.java -> "void"
        clazz.isSubclassOf<JsonObject>()  -> "any"
        clazz.isSubclassOf<JsonArray>() -> "Array<any>"
        clazz is WildcardType -> {
            visited += clazz
            when {
                clazz.upperBounds.size > 1 || clazz.lowerBounds.size > 1 -> "any"
                clazz.upperBounds.isNotEmpty() && clazz.upperBounds.first() != Object::class.java ->
                    recurse(clazz.upperBounds.first())
                clazz.lowerBounds.isNotEmpty() ->
                    recurse(clazz.lowerBounds.first())
                else -> "any"
            }
        }
        clazz is ParameterizedType -> {
            visited += clazz

            val base = clazz.rawType as Class<*>
            val params = clazz.actualTypeArguments
            when {
                base.isSubclassOf<Collection<*>>() -> {
                    "Array<${recurse(params[0])}>"
                }
                base.isSubclassOf<Map<*, *>>() && clazz.actualTypeArguments.size >= 2 -> {
                    "Array<[${recurse(params[0])}, ${recurse(params[1])}]>"
                }
                base.isSubclassOf<Map<*, *>>() -> {
                    val mapInterface = base.genericInterfaces.find {
                        it is ParameterizedType && it.rawType.isSubclassOf(Map::class.java)
                    } as ParameterizedType
                    "Array<[${recurse(mapInterface.actualTypeArguments[0])}, ${recurse(mapInterface.actualTypeArguments[1])}]>"
                }
                base.isSubclassOf<DbRecordWrapper<*>>() -> {
                    "{record: ${recurse(params[0])}, verificationData: ${recurse(VerificationData::class.java)}}"
                }
                else -> "any"
            }
        }
        clazz is Class<*> && clazz.isSubclassOf<TableRecord<*>>() -> {
            clazz as Class<TableRecord<*>>
            visited += clazz

            val instance = clazz.newInstance() as TableRecord<*>
            val table = instance.getTable()
            val params = clazz.methods.filter {
                !it.isSynthetic
                        && it.parameters.isEmpty()
                        && it.name.startsWith("get")
                        && it.name != "getClass"
                        && it.name != "getTable"
            }
            params.joinToString(prefix = "{", postfix = "}") {
                val propName = it.name.removePrefix("get").decapitalize()
                val name = camelToKey.convert(propName)
                val `?` = if(purpose == Purpose.ForInput || table.field(name).dataType.nullable()) "?" else ""
                "${propName}${`?`}: ${recurse(it.genericReturnType)}"
            }
        }
        clazz is Class<*> && clazz.isEnum -> {
            clazz.enumConstants.joinToString(" | ") { "'$it'" }
        }
        clazz is Class<*> && clazz.isSubclassOf<SerializedAsArray<*>>() -> {
            val arrInterface = clazz.genericInterfaces.find {
                it is ParameterizedType && it.rawType.isSubclassOf(SerializedAsArray::class.java)
            } as ParameterizedType
            "Array<${recurse(arrInterface.actualTypeArguments[0])}>"
        }
        clazz is Class<*> && clazz.isSubclassOf<SerializedAsObject<*, *>>() -> {
            val objInterface = clazz.genericInterfaces.find {
                it is ParameterizedType && it.rawType == SerializedAsObject::class.java
            } as ParameterizedType
            "Mapping<${recurse(objInterface.actualTypeArguments[0])}, " +
                    "${recurse(objInterface.actualTypeArguments[1])}>"
        }

        clazz is Class<*> -> {
            visited += clazz

            val props = clazz.kotlin.declaredMemberProperties
            props.joinToString(prefix = "{", postfix = "}") {
                val `?` = if(it.returnType.isMarkedNullable) "?" else ""
                "${it.name}${`?`}: ${recurse(it.returnType.javaType)}"
            }
        }
        else -> "any"
    }
}

object AddressExporter {
    internal operator fun Int.times(s: String) = s.repeat(this)
    internal operator fun String.times(i: Int) = repeat(i)

    @JvmStatic
    fun AppendScope.dumpKotlinObject(a: Any) {
        a::class.declaredMemberFunctions
                .filter { it.valueParameters.all { it.type.jvmErasure == String::class } }
                .forEach {
            +"${it.name}: (${it.valueParameters.joinToString(",") { "${it.name}: string" }}) => {"
            val args = it.valueParameters.map { "\${${it.name}}" }.toTypedArray()
            indent {
                it.call(a, *args).let {
                    +Json.encode(it).unquote().let { "return `$it`;" }
                }
            }
            +"},"
        }

        a::class.declaredMemberProperties
                .filter{ it.returnType.jvmErasure == String::class }
                .forEach {

            @Suppress(Warnings.UNCHECKED_CAST)
            it as KProperty1<Any, String>

            val value = it.call().let(Json::encode)

            +"${it.name}: $value as $value,"
        }
        a::class.nestedClasses.forEach {
            +"${it.simpleName}: {"
            it.objectInstance?.let {
                indent {
                    dumpKotlinObject(it)
                }
            }
            +"},"
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val pw = PrintWriter("kotoed-js/src/main/ts/util/kotoed-generated.ts")

        formatTo(pw) {
            + "import {Mapping} from './types'"
            +"/* to regenerate this file, run AddressExporter.kt */"
            +"export namespace Generated {"
            indent {
                +"/*  see org/jetbrains/research/kotoed/eventbus/Address.kt */"
                +"export const Address = {"
                indent {
                    +"Api: {"
                    indent {
                        dumpKotlinObject(Address.Api)
                    }
                    +"}"
                }
                +"};"
                +"/*  see org/jetbrains/research/kotoed/web/UrlPattern.kt */"
                +"export const UrlPattern = {"
                indent {
                    dumpKotlinObject(UrlPattern)
                }
                +"};"
                +"/*  see server code */"
                +"export interface ApiBindingOutputs {"
                indent {
                    Reflections("org.jetbrains.research.kotoed", MethodAnnotationsScanner())
                            .getMethodsAnnotatedWith(JsonableEventBusConsumerFor::class.java)
                            .forEach { method ->
                                val a = method.getAnnotation(JsonableEventBusConsumerFor::class.java)
                                if(a.address.startsWith("kotoed.api")) {
                                    val lastParameter = method.parameters.lastOrNull()
                                    val returnType = when {
                                        lastParameter !== null && Continuation::class.java.isAssignableFrom(lastParameter.type) -> {
                                            (lastParameter.parameterizedType as? ParameterizedType)
                                                    ?.actualTypeArguments
                                                    ?.firstOrNull()
                                                    ?: Object::class.java
                                        }
                                        else -> method.genericReturnType
                                    }
                                    +"[\"${a.address}\"]: ${classToTS(returnType, Purpose.ForOutput)};"
                                }
                            }

                    +"[key: string]: any"
                }
                +"};"
                +"export interface ApiBindingInputs {"
                indent {
                    Reflections("org.jetbrains.research.kotoed", MethodAnnotationsScanner())
                            .getMethodsAnnotatedWith(JsonableEventBusConsumerFor::class.java)
                            .forEach { method ->
                                val a = method.getAnnotation(JsonableEventBusConsumerFor::class.java)
                                if(a.address.startsWith("kotoed.api")) {
                                    val type = method.parameters.firstOrNull()?.parameterizedType ?: Unit::class.java
                                    +"[\"${a.address}\"]: ${classToTS(type, Purpose.ForInput)};"
                                }
                            }

                    +"[key: string]: any"
                }
                +"};"
            }
            +"}"
        }

        pw.close()
    }
}


