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

import net.sourceforge.argparse4j.*
import net.sourceforge.argparse4j.impl.*
import net.sourceforge.argparse4j.inf.*
import org.drimachine.grakmat.ParseException
import java.io.File
import kotlin.system.exitProcess

object Runner {
    @JvmStatic
    fun main(args: Array<String>) {
        val parser: ArgumentParser = ArgumentParsers.newArgumentParser("grakmat")
            .description("Runner for built-in grammars.")

        parser.addArgument("grammar")
                .type(String::class.java)
                .help("Grammar to parse.")
                .choices("json", "url", "uri")
        parser.addArgument("file")
                .type(String::class.java)
                .help("File to parse content from (default - read from STDIN).")
                .nargs("?")
        parser.addArgument("-t", "--trace")
                .help("Show stack trace on parse errors.")
                .action(Arguments.storeTrue())

        try {
            val result: Namespace = parser.parseArgs(args)
            val grammar: String = result.getString("grammar")
            val file: String? = result.getString("file")
            val trace: Boolean = result.getBoolean("trace")

            val parseFunction: (String) -> Any = createParseFunctionForGrammar(grammar)
            val parseFileFunction: (File) -> Any = createParseFileFunctionForGrammar(grammar)
            val handleFunction: (ParseException) -> Unit = createHandleFunction(trace)
            val parseAction = createParseAction(file, parseFunction, parseFileFunction, handleFunction)
            parseAction()
        } catch (e: ArgumentParserException) {
            parser.handleError(e)
        }
    }

    private fun createParseFunctionForGrammar(grammar: String): (String) -> Any =
            when (grammar) {
                "json" -> { input -> JSON.parse(input) }
                "url"  -> { input -> URL.parse(input)  }
                "uri"  -> { input -> URI.parse(input)  }
                else -> error("Unreachable code")
            }

    private fun createParseFileFunctionForGrammar(grammar: String): (File) -> Any =
            when (grammar) {
                "json" -> { file -> JSON.parseFile(file) }
                "url"  -> { file -> URL.parseFile(file)  }
                "uri"  -> { file -> URI.parseFile(file)  }
                else -> error("Unreachable code")
            }

    private fun createHandleFunction(trace: Boolean): (ParseException) -> Unit =
            if (trace)
                { e -> e.printStackTrace(System.out) }
            else
                { e -> println(e.message) }

    private fun createParseAction(filePath: String?,
                                  parseFunction: (String) -> Any, parseFileFunction: (File) -> Any,
                                  handleFunction: (ParseException) -> Unit): () -> Any =
            if (filePath == null)
                createInterpreterAction(parseFunction, handleFunction)
            else
                createParseFileAction(filePath, parseFileFunction, handleFunction)

    private fun createParseFileAction(filePath: String, parseFileFunction: (File) -> Any,
                                      handleFunction: (ParseException) -> Unit): () -> Any =
            {    // Start of lambda
                try {
                    val file = File(filePath)
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
