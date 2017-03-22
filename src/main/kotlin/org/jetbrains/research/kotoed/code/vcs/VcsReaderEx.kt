package org.jetbrains.research.kotoed.code.vcs

import org.jetbrains.research.kotoed.util.allLines
import java.io.File

data class CommandLine(val args: List<String>) {
    constructor(vararg vargs: String): this(vargs.asList())

    data class CommandOutput(val cout: Sequence<String>, val cerr: Sequence<String>)

    fun execute(wd: File = File(System.getProperty("user.dir")),
                input: Sequence<String> = sequenceOf()): CommandOutput {
        val pb = ProcessBuilder(args).directory(wd).start()

        val cin = pb.outputStream.writer()
        for(l in input) cin.append(l).appendln()

        val cout = pb.inputStream.bufferedReader().allLines()
        val cerr = pb.errorStream.bufferedReader().allLines()
        return CommandOutput(cout, cerr)
    }

}




