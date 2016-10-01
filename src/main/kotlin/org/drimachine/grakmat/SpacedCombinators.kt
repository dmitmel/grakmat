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

@file:JvmName("SpacedCombinators")
package org.drimachine.grakmat

import java.util.*


var space: Parser<Char> = anyOf(' ', '\t', '\r', '\n') withName "space"
val optionalSpaces: Parser<String> = zeroOrMore(space)
        .map { spaces: List<Char> -> spaces.joinToString("") }
        .withName("spaces")


/**
 * Works like [and], but inserts spaces between parsers.
 */
infix fun <A, B> Parser<A>._and_(other: Parser<B>): Parser<Pair<A, B>> = SpacedAndParser(this, other)

/**
 * Works like [_and_], but returns only second result.
 */
infix fun <A, B> Parser<A>._then_(other: Parser<B>): Parser<B> = this _and_ other map { it.second }

/**
 * Works like [_and_], but returns only first result.
 */
infix fun <A, B> Parser<A>._before_(other: Parser<B>): Parser<A> = this _and_ other map { it.first }

/**
 * @see _and_
 */
class SpacedAndParser<out A, out B>(val leftParser: Parser<A>, val rightParser: Parser<B>) : Parser<Pair<A, B>> {
    override val expectedDescription: String
        get() = "${leftParser.expectedDescription} and ${rightParser.expectedDescription}"

    override fun eat(source: Source, input: String): Result<Pair<A, B>> {
        val leftResult: Result<A>        = leftParser.eat(source,  input)
        val middleResult: Result<String> = optionalSpaces.eat(source, leftResult.remainder)
        val rightResult: Result<B>       = rightParser.eat(source, middleResult.remainder)
        return Result(leftResult.value to rightResult.value, rightResult.remainder)
    }

    override fun toString(): String = expectedDescription
}


/**
 * Works like [repeat], but inserts spaces between results.
 */
infix fun <A> Parser<A>._repeat_(times: Int): Parser<List<A>> = SpacedRepeatParser(this, times)

/**
 * @see _repeat_
 */
class SpacedRepeatParser<out A>(val target: Parser<A>, val times: Int) : Parser<List<A>> {
    override val expectedDescription: String
        get() = "${target.expectedDescription} exactly $times times"

    override fun eat(source: Source, input: String): Result<List<A>> {
        val list = ArrayList<A>()
        var remainder = input

        // (1..times) - repeat folding 'times' times
        for (time in 1..times) {
            val next: Result<A> = target.eat(source, remainder)
            list.add(next.value)
            val spacesResult: Result<String> = optionalSpaces.eat(source, next.remainder)
            remainder = spacesResult.value
        }

        return Result(Collections.unmodifiableList(list), remainder)
    }

    override fun toString(): String = expectedDescription
}


/**
 * Works like [atLeast], but inserts spaces between results.
 */
infix fun <A> Parser<A>._atLeast_(times: Int): Parser<List<A>> = SpacedAtLeastParser(this, times)

/**
 * Alias to `atLeast(0, xxx)`.
 *
 * @see atLeast
 */
fun <A> _zeroOrMore_(target: Parser<A>): Parser<List<A>> = target _atLeast_ 0

/**
 * Alias to `atLeast(1, xxx)`.
 *
 * @see atLeast
 */
fun <A> _oneOrMore_(target: Parser<A>): Parser<List<A>> = target _atLeast_ 1

/**
 * @see _atLeast_
 */
class SpacedAtLeastParser<out A>(val target: Parser<A>, val times: Int) : Parser<List<A>> {
    override val expectedDescription: String
        get() = "${target.expectedDescription} at least $times times"

    override fun eat(source: Source, input: String): Result<List<A>> {
        var (list: List<A>, remainder: String) = target.repeat(times).eat(source, input)
        list = ArrayList<A>(list)

        do {
            val (next: A?, nextRemainder: String) = optional(target).eat(source, remainder)
            if (next != null) {
                list.add(next)
                val spacesResult: Result<String> = optionalSpaces.eat(source, nextRemainder)
                remainder = spacesResult.remainder
            } else {
                remainder = nextRemainder
            }
        } while (next != null)

        return Result(Collections.unmodifiableList(list), remainder)
    }

    override fun toString(): String = expectedDescription
}


/**
 * Works like [inRange], but inserts spaces between results.
 */
infix fun <A> Parser<A>._inRange_(bounds: IntRange) : Parser<List<A>> = SpacedRangedParser(this, bounds)

/**
 * @see _inRange_
 */
class SpacedRangedParser<out A>(val target: Parser<A>, val bounds: IntRange) : Parser<List<A>> {
    override val expectedDescription: String
        get() = "${target.expectedDescription}{${bounds.start},${bounds.endInclusive}}"

    override fun eat(source: Source, input: String): Result<List<A>> {
        var (list, remainder) = target.repeat(bounds.start).eat(source, input)
        list = ArrayList<A>(list)

        do {
            val (next: A?, nextRemainder: String) = optional(target).eat(source, remainder)
            if (next != null) {
                list.add(next)
                remainder = nextRemainder
            } else {
                val spacesResult: Result<String> = optionalSpaces.eat(source, nextRemainder)
                remainder = spacesResult.remainder
            }
        } while (next != null && list.size < bounds.endInclusive)

        return Result(Collections.unmodifiableList(list), remainder)
    }

    override fun toString(): String = expectedDescription
}
