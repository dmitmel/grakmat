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

@file:JvmName("Combinators")
package org.drimachine.grakmat

import java.lang.reflect.*
import java.util.*


/**
 * Creates parser from two others that tries to parse `a` if possible, otherwise parse `b`.
 *
 * @receiver Left parser.
 * @param other Right parser.
 * @return The result of the successful parse.
 */
infix fun <A> Parser<A>.or(other: Parser<A>): Parser<A> = OrParser(this, other)

/**
 * Creates parser, which will try to consume input with target parser,
 * or return `null`.
 *
 * @param target Target parser.
 * @see empty
 */
fun <A> optional(target: Parser<A>): Parser<A?> = target or empty<A>()

/**
 * @see or
 */
class OrParser<out A>(val leftParser: Parser<A>, val rightParser: Parser<A>) : Parser<A> {
    override val expectedDescription: String
        get() = "${leftParser.expectedDescription} or ${rightParser.expectedDescription}"

    override fun eat(source: Source, input: String): Result<A> =
        try {
            leftParser.eat(source, input)
        } catch (leftEx: ParseException) {
            try {
                rightParser.eat(source, input)
            } catch (rightEx: UnexpectedEOFException) {
                val mixedExpected: String        = getPropertyFrom(leftEx, rightEx)
                val errorPosition: ErrorPosition = leftEx.errorPosition
                val isFromNamedParser: Boolean   = isFromNamedParser(leftEx, rightEx)
                val got: String?                 = getGotFrom(leftEx)

                if (leftEx is UnexpectedEOFException)
                    throw UnexpectedEOFException(mixedExpected, errorPosition, source).isFromNamedParser(isFromNamedParser)
                else
                    throw UnexpectedTokenException(got, mixedExpected, errorPosition, source).isFromNamedParser(isFromNamedParser)
            } catch (rightEx: UnexpectedTokenException) {
                val mixedExpected: String        = getPropertyFrom(leftEx, rightEx)
                val errorPosition: ErrorPosition = leftEx.errorPosition
                val isFromNamedParser: Boolean   = isFromNamedParser(leftEx, rightEx)
                val got: String?                 = getGotFrom(leftEx)

                throw UnexpectedTokenException(got, mixedExpected, errorPosition, source).isFromNamedParser(isFromNamedParser)
            }
        }

    private fun isFromNamedParser(leftEx: ParseException, rightEx: ParseException): Boolean =
            leftEx.isFromNamedParser || rightEx.isFromNamedParser

    private fun getGotFrom(parseEx: ParseException): String? = when (parseEx) {
        is UnexpectedEOFException   -> "<EOF>"
        is UnexpectedTokenException -> parseEx.got
        else                        -> getPropertyFrom(parseEx, "got")?.toString()
    }

    private fun getPropertyFrom(leftEx: ParseException, rightEx: ParseException): String {
        var leftExpected:  String? = getPropertyFrom(leftEx, "expected")?.toString()
        var rightExpected: String? = getPropertyFrom(rightEx, "expected")?.toString()

        if (leftExpected == null)
            leftExpected = leftParser.expectedDescription
        if (rightExpected == null)
            rightExpected = rightParser.expectedDescription

        return if (leftExpected == rightExpected) leftExpected else "$leftExpected or $rightExpected"
    }

    private fun getPropertyFrom(obj: Any, propertyName: String): Any? {
        val clazz: Class<Any> = obj.javaClass

        try {
            val field: Field = clazz.getField(propertyName)
            return field.get(obj)
        } catch (e: ReflectiveOperationException) {
            try {
                val method: Method = clazz.getMethod("get" + propertyName.capitalize())
                return method.invoke(obj)
            } catch (e: ReflectiveOperationException) {
                return null
            }
        }
    }

    override fun toString(): String = expectedDescription
}


/**
 * Creates parser from two others that parses `a` and then `b`.
 *
 * @receiver Left parser.
 * @param other Right parser.
 * @return `Pair<A, B>` of both results.
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
        val leftResult:  Result<A> = leftParser.eat(source,  input)
        val rightResult: Result<B> = rightParser.eat(source, leftResult.remainder)
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
        val rawResult: Result<A> = target.eat(source, input)
        val mappedValue: B = transform(rawResult.value)
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
                if (e.isFromNamedParser)
                    throw e
                else
                    throw UnexpectedEOFException(this.name, e.errorPosition, source).isFromNamedParser(true)
            } catch (e: UnexpectedTokenException) {
                if (e.isFromNamedParser)
                    throw e
                else
                    throw UnexpectedTokenException(e.got, this.name, e.errorPosition, source).isFromNamedParser(true)
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
            val next: Result<A> = target.eat(source, remainder)
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
 * Alias to `atLeast(0, xxx)`.
 *
 * @see atLeast
 */
fun <A> zeroOrMore(target: Parser<A>): Parser<List<A>> = target atLeast 0

/**
 * Alias to `atLeast(1, xxx)`.
 *
 * @see atLeast
 */
fun <A> oneOrMore(target: Parser<A>): Parser<List<A>> = target atLeast 1

/**
 * @see atLeast
 */
class AtLeastParser<out A>(val target: Parser<A>, val times: Int) : Parser<List<A>> {
    override val expectedDescription: String
        get() = "${target.expectedDescription} at least $times times"

    override fun eat(source: Source, input: String): Result<List<A>> {
        var (results: List<A>, remainder: String) = target.repeat(times).eat(source, input)
        results = ArrayList<A>(results)

        do {
            val (nextResult: A?, nextRemainder: String) = optional(target).eat(source, remainder)
            if (nextResult != null) results.add(nextResult)
            remainder = nextRemainder
        } while (nextResult != null)

        return Result(Collections.unmodifiableList(results), remainder)
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
        var (results: List<A>, remainder: String) = target.repeat(bounds.start).eat(source, input)
        results = ArrayList<A>(results)

        do {
            val (nextResult: A?, nextRemainder: String) = optional(target).eat(source, remainder)
            if (nextResult != null) results.add(nextResult)
            remainder = nextRemainder
        } while (nextResult != null && results.size < bounds.endInclusive)

        return Result(Collections.unmodifiableList(results), remainder)
    }

    override fun toString(): String = expectedDescription
}
