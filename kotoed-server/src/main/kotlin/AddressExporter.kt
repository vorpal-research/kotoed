import io.vertx.core.json.Json
import kotlinx.Warnings
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.unquote
import org.jetbrains.research.kotoed.web.UrlPattern
import java.io.PrintWriter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure

object AddressExporter {
    internal operator fun Int.times(s: String) = s.repeat(this)
    internal operator fun String.times(i: Int) = repeat(i)

    @JvmStatic
    fun dumpKotlinObject(w: PrintWriter, a: Any, indent: Int = 0) {
        a::class.declaredMemberFunctions
                .filter { it.valueParameters.all { it.type.jvmErasure == String::class } }
                .forEach {
            w.print("    " * indent)
            w.print(it.name)
            w.println(": (${it.valueParameters.joinToString(",") { "${it.name}: string" }}) => {")
            val args = it.valueParameters.map { "\${${it.name}}" }.toTypedArray()
            it.call(a, *args).let {
                w.print("    " * (indent + 1))
                w.println(Json.encode(it).unquote().let { "return `$it`;" })
            }
            w.print("    " * indent)
            w.println("},")
        }

        a::class.declaredMemberProperties
                .filter{ it.returnType.jvmErasure == String::class }
                .forEach {

            @Suppress(Warnings.UNCHECKED_CAST)
            it as KProperty1<Any, String>

            w.print("    " * indent)
            w.print(it.name)
            w.print(": ")
            it.call().let(Json::encode).let(w::print)
            w.println(",")
        }
        a::class.nestedClasses.forEach {
            w.print("    " * indent)
            w.print(it.simpleName)
            w.println(": {")
            it.objectInstance?.let {
                dumpKotlinObject(w, it, indent + 1)
            }
            w.print("    " * indent)
            w.println("},")
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val pw = PrintWriter("kotoed-js/src/main/ts/util/kotoed-generated.ts")

        """
            /* to regenerate this file, run AddressExporter.kt */
            export namespace Generated {

            /*  see org/jetbrains/research/kotoed/eventbus/Address.kt */
                export const Address = {
                    Api: {
        """.trimIndent().let(pw::println)

        dumpKotlinObject(pw, Address.Api, 3)

        """
                    }
                };

            /*  see org/jetbrains/research/kotoed/web/UrlPattern.kt */
                export const UrlPattern = {
        """.trimIndent().let(pw::println)

        dumpKotlinObject(pw, UrlPattern, 2)

        """
                }

            }
        """.trimIndent().let(pw::println)

        pw.close()
    }
}


