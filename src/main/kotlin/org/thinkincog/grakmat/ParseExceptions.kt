/*
 * Copyright (c) 2016 ThinkInCog.org
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

package org.thinkincog.grakmat

/**
 * Base exception for all parse exceptions.
 *
 * @param errorDescription Error description.
 * @param errorPosition Line with error.
 */
open class ParseException(val errorDescription: String, val errorPosition: ErrorPosition) : RuntimeException() {
    var tag: Tag = Tag.NONE
    fun withTag(tag: Tag): ParseException = this.apply { this.tag = tag }

    /** @see [ErrorPosition.lineNumber]. */
    val lineNumber: Int
        get() = errorPosition.lineNumber
    /** @see [ErrorPosition.lineIndex]. */
    val lineIndex: Int
        get() = errorPosition.lineIndex
    /** @see [ErrorPosition.columnNumber]. */
    val columnNumber: Int
        get() = errorPosition.columnNumber
    /** @see [ErrorPosition.columnIndex]. */
    val columnIndex: Int
        get() = errorPosition.columnIndex
    /** @see [ErrorPosition.lineSource]. */
    val lineSource: String
        get() = errorPosition.lineSource

    /**
     * Returns message for exception. Here's the format:
     * ```
     * error description
     * line number: line source
     *                   ^
     * ```
     * Where `^` is pointer to the column with error.
     */
    override val message: String
        get() = "$errorDescription\n$errorPosition"

    enum class Tag {
        NONE, NAMED
    }
}

/**
 * Parse exception for unexpected EOF.
 *
 * @param expected Expected token.
 */
class UnexpectedEOFException(val expected: String, errorPosition: ErrorPosition)
: ParseException("Expected $expected, but got <EOF>", ErrorPosition(errorPosition.lineNumber, errorPosition.lineSource.length + 1, errorPosition.lineSource))

/**
 * Parse exception for unexpected token.
 *
 * @param expected Expected token.
 */
class UnexpectedTokenException(val expected: String, errorPosition: ErrorPosition)
: ParseException("Expected $expected", errorPosition)
