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

@file:JvmName("Strings")
package org.drimachine.grakmat


/**
 * Adds [ellipsis] to [String] if it's length is greater than [maxLength].
 */
@JvmOverloads
fun String.boundLengthTo(maxLength: Int, ellipsis: String = "..."): String =
        if (this.isEmpty())
            throw IllegalArgumentException("String is empty")
        else if (maxLength < 1)
            throw IllegalArgumentException("Max length is smaller than 1")
        else if (this.length > maxLength)
            this.substring(0, maxLength) + ellipsis
        else
            this


/**
 * Data-class for data about the error position.
 *
 * @param lineNumber Line number.
 * @param columnNumber Column number.
 * @param lineSource Line lineSource *without separators*.
 */
data class ErrorPosition(val lineNumber: Int, val columnNumber: Int, val lineSource: String) {
    val lineIndex: Int
        get() = lineNumber - 1
    val columnIndex: Int
        get() = columnNumber - 1

    constructor(coordinates: Pair<Int, Int>, source: String) : this(coordinates.first, coordinates.second, source)

    /**
     * Returns detailed text info for this line. Here's the format:
     * ```
     * line number: line source
     *                   ^
     * ```
     * Where `^` is pointer to the column number.
     */
    override fun toString(): String {
        val sourcePrefix = "$lineNumber: "
        val columnPointer = buildString {
            append(" ".repeat(sourcePrefix.length))
            append(" ".repeat(columnIndex))
            append('^')
        }

        return "$sourcePrefix$lineSource\n$columnPointer"
    }
}

fun String.errorPosition(index: Int): ErrorPosition {
    var offset = 0        // The largest index in a previous line
    var lineNumber = 1

    val lines = this.linesWithSeparators()
    for (line in lines) {
        // Long-long offset  <--- Offset
        // Long-long offset  <--- Offset
        // target line
        //    |       |        If this line contains index, it will be trapped in then box -
        //    |       |        it's lower than {offset + length of target line},
        //  Index   Length     and greater than {offset}.
        if (offset + line.length > index) {
            val columnIndex = index - offset
            val columnNumber = columnIndex + 1
            val lineWithoutSeparator = line.removeSuffix("\r\n").removeSuffix("\r").removeSuffix("\n")
            return ErrorPosition(lineNumber, columnNumber, lineWithoutSeparator)
        } else {
            offset += line.length
            lineNumber++
        }
    }

    val lastLineNumber   = lines.size
    val lastLineSource   = if (lines.isEmpty()) "" else lines.last()
    val lastColumnNumber = lastLineSource.length + 1
    return ErrorPosition(lastLineNumber, lastColumnNumber, lastLineSource)
}

/**
 * Splits string to list of lines, which also includes used separators.
 *
 * ## Examples:
 *
 * ```
 * a\n
 * b\n   ====> ["a\n", "b\n", "c"]
 * c
 * ```
 *
 * ```
 * a\r
 * b\r   ====> ["a\r", "b\r", "c\r"]
 * c\r
 * ```
 *
 * ```
 * a\r\n
 * \r\n  ====> ["a\r\n", "\r\n", "b\r\n", "c"]
 * b\r\n
 * c
 * ```
 */
fun String.linesWithSeparators(): List<String> {
    var line = StringBuilder()
    val lines = arrayListOf<String>()
    var previousCharIsCR = false

    for (char in this) {
        when (char) {
            '\r' -> {
                if (previousCharIsCR) {
                    lines += line.toString()
                    line = StringBuilder()
                }

                previousCharIsCR = true
                line.append(char)
            }

            '\n' -> {
                line.append(char)
                if (previousCharIsCR)
                    previousCharIsCR = false
                lines += line.toString()
                line = StringBuilder()
            }

            else -> {
                if (previousCharIsCR) {
                    previousCharIsCR = false
                    lines += line.toString()
                    line = StringBuilder()
                }

                line.append(char)
            }
        }
    }

    // Adding the last line
    if (line.isNotEmpty())
        lines += line.toString()

    return lines
}
