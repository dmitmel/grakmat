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

@file:JvmName("Combinators")

package org.thinkincog.grakmat

import java.util.*

/**
 * Creates parser from two others which tries to parse result this way:
 *
 * ```
 * 1. Try to use the left parser.
 * 2. If left parser fails...
 *    2.1. Try to use the right parser.
 *    2.2. If right parser fails...
 *         2.2.1. Throw an exception.
 *    2.3. Otherwise...
 *         2.3.1. Return result of right parser.
 * 3. Otherwise...
 *     3.1. Return result of left parser.
 * ```
 *
 * @receiver Left parser.
 * @param other Right parser.
 */
infix fun <A> Parser<A>.or(other: Parser<A>): Parser<A> = OrParser(this, other)

/**
 * @see or
 */
class OrParser<out A>(val leftParser: Parser<A>, val rightParser: Parser<A>) : Parser<A> {
    override val expectedDescription: String
        get() = "${leftParser.expectedDescription} or ${rightParser.expectedDescription}"

    override fun eat(source: Source, input: String): Result<A> {
        try {
            return leftParser.eat(source, input)
        } catch (leftEx: ParseException) {
            try {
                return rightParser.eat(source, input)
            } catch (rightEx: UnexpectedEOFException) {
                val mixedExpected: String        = getExpectedFrom(leftEx, rightEx)
                val errorPosition: ErrorPosition = getErrorPositionFrom(leftEx, rightEx)
                val tag: ParseException.Tag = getTagFrom(leftEx, rightEx)

                if (leftEx is UnexpectedEOFException)
                    throw UnexpectedEOFException(mixedExpected, errorPosition).withTag(tag)
                else
                    throw UnexpectedTokenException(mixedExpected, errorPosition).withTag(tag)
            } catch (rightEx: UnexpectedTokenException) {
                val mixedExpected: String   = getExpectedFrom(leftEx, rightEx)
                val errorPosition: ErrorPosition = getErrorPositionFrom(leftEx, rightEx)
                val tag: ParseException.Tag = getTagFrom(leftEx, rightEx)

                throw UnexpectedTokenException(mixedExpected, errorPosition).withTag(tag)
            }
        }
    }

    private fun getTagFrom(leftEx: ParseException, rightEx: ParseException): ParseException.Tag {
        val anyIsNamed = (leftEx.tag == ParseException.Tag.NAMED && rightEx.tag == ParseException.Tag.NAMED)
        return if (anyIsNamed) ParseException.Tag.NAMED else ParseException.Tag.NONE
    }

    private fun getErrorPositionFrom(leftEx: ParseException, rightEx: ParseException): ErrorPosition {
        return leftEx.errorPosition
    }

    private fun getExpectedFrom(leftEx: ParseException, rightEx: ParseException): String {
        var leftExpected  = getExpectedFrom(leftEx)
        var rightExpected = getExpectedFrom(rightEx)

        if (leftExpected == null)
            leftExpected = leftParser.expectedDescription
        if (rightExpected == null)
            rightExpected = rightParser.expectedDescription

        return if (leftExpected == rightExpected) leftExpected else "$leftExpected or $rightExpected"
    }

    private fun getExpectedFrom(parseException: ParseException): String? {
        val clazz = parseException.javaClass

        try {
            val field = clazz.getField("expected")
            return field.get(parseException).toString()
        } catch (e: ReflectiveOperationException) {
            try {
                val method = clazz.getMethod("getExpected")
                return method.invoke(parseException).toString()
            } catch (e: ReflectiveOperationException) {
                return null
            }
        }
    }

    override fun toString(): String = expectedDescription
}


/**
 * Creates parser from two others which tries to parse result this way:
 *
 * ```
 * 1. Try to use the left parser.
 * 2. If left parser fails...
 *    2.1. Throw an exception.
 * 3. Otherwise...
 *    3.1. Try to use the right parser on remainder of left one.
 *    3.2. If right parser fails...
 *         3.2.1. Throw an exception.
 *    3.3. Otherwise...
 *         3.3.1 Return both results.
 * ```
 *
 * @receiver Left parser.
 * @param other Right parser.
 */
infix fun <A, B> Parser<A>.and(other: Parser<B>): Parser<Pair<A, B>> = AndParser(this, other)

/**
 * Works like [and], but returns only second result.
 */
infix fun <A, B> Parser<A>.then(other: Parser<B>): Parser<B> = this and other map { it.second }

/**
 * Works like [and], but returns only first result.
 */
infix fun <A, B> Parser<A>.before(other: Parser<B>): Parser<A> = this and other map { it.first }

/**
 * @see and
 */
class AndParser<out A, out B>(val leftParser: Parser<A>, val rightParser: Parser<B>) : Parser<Pair<A, B>> {
    override val expectedDescription: String
        get() = "${leftParser.expectedDescription} and ${rightParser.expectedDescription}"

    override fun eat(source: Source, input: String): Result<Pair<A, B>> {
        val leftResult  = leftParser.eat(source,  input)
        val rightResult = rightParser.eat(source, leftResult.remainder)
        return Result(leftResult.value to rightResult.value, rightResult.remainder)
    }

    override fun toString(): String = expectedDescription
}


/**
 * Creates parser, that eats input with the target parser,
 * and maps it using transformer function.
 *
 * @receiver Target parser.
 * @param transform Transformer function.
 */
infix fun <A, B> Parser<A>.map(transform: (A) -> B): Parser<B> = MappedParser(this, transform)

/**
 * @see map
 */
class MappedParser<A, out B>(val target: Parser<A>, val transform: (A) -> B) : Parser<B> {
    override val expectedDescription: String
        get()  = target.expectedDescription

    override fun eat(source: Source, input: String): Result<B> {
        val rawResult = target.eat(source, input)
        val mappedValue = transform(rawResult.value)
        return Result(mappedValue, rawResult.remainder)
    }

    override fun toString(): String = target.toString()
}


/**
 * Creates parser, that has name, and will use this name
 * as description in rethrown parsing errors.
 *
 * @receiver Target parser.
 * @param name Name of this parser.
 */
infix fun <A> Parser<A>.withName(name: String): Parser<A> = NamedParser(this, name)

/**
 *
 */
class NamedParser<out A>(val target: Parser<A>, val name: String) : Parser<A> {
    override val expectedDescription: String
        get() = name

    override fun eat(source: Source, input: String): Result<A> =
            try {
                target.eat(source, input)
            } catch (e: UnexpectedEOFException) {
                if (e.tag == ParseException.Tag.NAMED)
                    throw e
                else
                    throw UnexpectedEOFException(this.name, e.errorPosition).withTag(ParseException.Tag.NAMED)
            } catch (e: UnexpectedTokenException) {
                if (e.tag == ParseException.Tag.NAMED)
                    throw e
                else
                    throw UnexpectedTokenException(this.name, e.errorPosition).withTag(ParseException.Tag.NAMED)
            }

    override fun toString(): String = expectedDescription
}


/**
 * Creates parser, that matches target parser *EXACTLY* _n_ times.
 *
 * @receiver Target parser.
 * @param times Times to repeat.
 */
infix fun <A> Parser<A>.repeat(times: Int): Parser<List<A>> = RepeatParser(this, times)

/**
 * @see repeat
 */
class RepeatParser<out A>(val target: Parser<A>, val times: Int) : Parser<List<A>> {
    override val expectedDescription: String
        get() = "${target.expectedDescription} exactly $times times"

    override fun eat(source: Source, input: String): Result<List<A>> {
        val list = ArrayList<A>()
        var remainder = input

        // (1..times) - repeat folding 'times' times
        for (time in 1..times) {
            val next = target.eat(source, remainder)
            list.add(next.value)
            remainder = next.remainder
        }

        return Result(Collections.unmodifiableList(list), remainder)
    }

    override fun toString(): String = expectedDescription
}


/**
 * Creates parser, that matches target parser *AT LEAST* _n_ times.
 *
 * @receiver Target parser.
 * @param times Minimum times to repeat.
 */
infix fun <A> Parser<A>.atLeast(times: Int): Parser<List<A>> = AtLeastParser(this, times)

/**
 * @see atLeast
 */
class AtLeastParser<out A>(val target: Parser<A>, val times: Int) : Parser<List<A>> {
    override val expectedDescription: String
        get() = "${target.expectedDescription} at least $times times"

    override fun eat(source: Source, input: String): Result<List<A>> {
        var (list, remainder) = target.repeat(times).eat(source, input)
        list = ArrayList<A>(list)

        do {
            val (next, r) = optional(target).eat(source, remainder)
            if (next != null) list.add(next)
            remainder = r
        } while (next != null)

        return Result(Collections.unmodifiableList(list), remainder)
    }

    override fun toString(): String = expectedDescription
}


/**
 * Creates parser, that matches target parser from _BOUNDS START_ to _BOUNDS END_.
 *
 * @receiver Target parser.
 * @param bounds Min and max count of matches _INCLUSIVE_.
 */
infix fun <A> Parser<A>.inRange(bounds: IntRange) : Parser<List<A>> = RangedParser(this, bounds)

/**
 * @see inRange
 */
class RangedParser<out A>(val target: Parser<A>, val bounds: IntRange) : Parser<List<A>> {
    override val expectedDescription: String
        get() = "${target.expectedDescription}{${bounds.start},${bounds.endInclusive}}"

    override fun eat(source: Source, input: String): Result<List<A>> {
        var (list, remainder) = target.repeat(bounds.start).eat(source, input)
        list = ArrayList<A>(list)

        do {
            val (next, r) = optional(target).eat(source, remainder)
            if (next != null) list.add(next)
            remainder = r
        } while (next != null && list.size < bounds.endInclusive)

        return Result(Collections.unmodifiableList(list), remainder)
    }
}
