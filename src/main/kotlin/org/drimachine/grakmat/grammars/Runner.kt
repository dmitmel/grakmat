/*
 * Copyright (c) 2016 Drimachine.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drimachine.grakmat.grammars

import org.drimachine.grakmat.ParseException
import java.io.File
import kotlin.system.exitProcess

object Runner {
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            val (grammar, file, trace) = parseArguments(args)

            val parseFunction: (String) -> Any = createParseFunctionForGrammar(grammar)
            val parseFileFunction: (File) -> Any = createParseFileFunctionForGrammar(grammar)
            val handleFunction: (ParseException) -> Unit = createHandleFunction(trace)
            val parseAction = createParseAction(file, parseFunction, parseFileFunction, handleFunction)
            parseAction()
        } catch (e: IllegalArgumentException) {
            System.err.println("grakmat: ${e.message}")
        }
    }

    private fun parseArguments(args: Array<String>): Triple<String, File?, Boolean> {
        val (flags, positionals) = args.partition { it.startsWith("-") }
        val trace: Boolean = ("-t" in flags || "--trace" in flags)
        val flagsWithoutTrace = flags.filterNot { it == "-t" || it == "--trace" }
        if (flagsWithoutTrace.isNotEmpty())
            throw IllegalArgumentException("unexpected flags: ${flagsWithoutTrace.joinToString()}")

        val grammar: String = positionals.getOrNull(0) ?: throw IllegalArgumentException("expected grammar argument")
        val file: File? = positionals.getOrNull(1)?.let { path -> parseFileFromPath(path) }
        if (positionals.size > 2)
            throw IllegalArgumentException("unexpected positionals: ${positionals.joinToString()}")

        return Triple(grammar, file, trace)
    }

    private fun parseFileFromPath(path: String): File {
        val file = File(path)
        if (file.exists() && file.isFile)
            return file
        else if (file.exists() && file.isDirectory)
            throw IllegalArgumentException("${file.absolutePath}: is a directory")
        else
            throw IllegalArgumentException("${file.absolutePath}: no such file or directory")
    }

    private fun createParseFunctionForGrammar(grammar: String): (String) -> Any =
            when (grammar) {
                "json" -> { input -> JSON.parse(input)                      }
                "gdl"  -> { input -> GrammarDefinitionLanguage.parse(input) }
                else   -> throw IllegalArgumentException("$grammar: no such grammar")
            }

    private fun createParseFileFunctionForGrammar(grammar: String): (File) -> Any =
            when (grammar) {
                "json" -> { file -> JSON.parseFile(file)                      }
                "gdl"  -> { file -> GrammarDefinitionLanguage.parseFile(file) }
                else   -> throw IllegalArgumentException("$grammar: no such grammar")
            }

    private fun createHandleFunction(trace: Boolean): (ParseException) -> Unit =
            if (trace)
                { e -> e.printStackTrace(System.out) }
            else
                { e -> println(e.message) }

    private fun createParseAction(file: File?,
                                  parseFunction: (String) -> Any, parseFileFunction: (File) -> Any,
                                  handleFunction: (ParseException) -> Unit): () -> Any =
            if (file != null)
                createParseFileAction(file, parseFileFunction, handleFunction)
            else
                createInterpreterAction(parseFunction, handleFunction)

    private fun createParseFileAction(file: File, parseFileFunction: (File) -> Any,
                                      handleFunction: (ParseException) -> Unit): () -> Any =
            {    // Start of lambda
                try {
                    val result = parseFileFunction(file)
                    println(result)
                } catch (e: ParseException) {
                    handleFunction(e)
                }
            }    // End of lambda

    private fun createInterpreterAction(parseFunction: (String) -> Any,
                                        handleFunction: (ParseException) -> Unit): () -> Any =
            {    // Start of lambda
                while (true) {
                    try {
                        print(">>> ")
                        val optionalInput: String? = readLine()
                        val input = optionalInput ?: exitProcess(1)

                        if (input == ":quit") {
                            exitProcess(0)
                        } else {
                            val result = parseFunction(input)
                            println(result)
                        }
                    } catch (e: ParseException) {
                        handleFunction(e)
                    }
                }
            }    // Start of lambda
}
