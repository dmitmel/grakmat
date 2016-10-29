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

package org.drimachine.grakmat

/**
 * Base exception for all parse exceptions.
 *
 * @param errorDescription Error description.
 * @param errorPosition Line with error.
 */
open class ParseException(val errorDescription: String, val errorPosition: ErrorPosition, val source: Source) : RuntimeException() {
    var isFromNamedParser = false
    fun isFromNamedParser(isFromNamedParser: Boolean): ParseException = apply { this.isFromNamedParser = isFromNamedParser }

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
        get() = "${source.fileName}:$lineNumber: $errorDescription\n$errorPosition"
}

/**
 * Superclass for all exceptions, that **can't be caught** by combinators.
 */
open class NotCatchableParseException(errorDescription: String, errorPosition: ErrorPosition, source: Source) : ParseException(errorDescription, errorPosition, source)

/**
 * Not catchable parse exception for unexpected EOF.
 *
 * @param expected Expected token.
 */
class NotCatchableUnexpectedEOFException(val expected: String, errorPosition: ErrorPosition, source: Source)
: NotCatchableParseException(
        "Expected $expected, but got <EOF>",
        ErrorPosition(errorPosition.lineNumber,
                errorPosition.lineSource.length + 1,    // Making column number of error position point after the end of source
                errorPosition.lineSource),
        source)

/**
 * Not catchable parse exception for unexpected token.
 *
 * @param expected Expected token.
 */
class NotCatchableUnexpectedTokenException(val got: String?, val expected: String, errorPosition: ErrorPosition, source: Source)
: NotCatchableParseException("Expected $expected" + (if (got != null) ", but got \'$got\'" else ""), errorPosition, source)


/**
 * Superclass for all exceptions, that **can be caught** by combinators.
 */
open class CatchableParseException(errorDescription: String, errorPosition: ErrorPosition, source: Source) : ParseException(errorDescription, errorPosition, source)

/**
 * Parse exception for unexpected EOF.
 *
 * @param expected Expected token.
 */
class UnexpectedEOFException(val expected: String, errorPosition: ErrorPosition, source: Source)
: CatchableParseException(
        "Expected $expected, but got <EOF>",
        ErrorPosition(errorPosition.lineNumber,
                errorPosition.lineSource.length + 1,    // Making column number of error position point after the end of source
                errorPosition.lineSource),
        source)

/**
 * Parse exception for unexpected token.
 *
 * @param expected Expected token.
 */
class UnexpectedTokenException(val got: String?, val expected: String, errorPosition: ErrorPosition, source: Source)
: CatchableParseException("Expected $expected" + (if (got != null) ", but got \'$got\'" else ""), errorPosition, source)
