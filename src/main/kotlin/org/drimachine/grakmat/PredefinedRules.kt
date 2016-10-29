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

@file:JvmName("PredefinedRules")
package org.drimachine.grakmat

val SEMICOLON: Parser<Char> = char(';')
        .withName("';'")
val COLON: Parser<Char> = char(':')
        .withName("':'")
val DOUBLE_QUOTE: Parser<Char> = char('\"')
        .withName("'\"'")
val QUOTE: Parser<Char> = char('\'')
        .withName("'\\''")
val ASTERISK: Parser<Char> = char('*')
        .withName("'*'")
val LEFT_BRACKET: Parser<Char> = char('[')
        .withName("'['")
val RIGHT_BRACKET: Parser<Char> = char(']')
        .withName("']'")
val LEFT_BRACE: Parser<Char> = char('{')
        .withName("'{'")
val RIGHT_BRACE: Parser<Char> = char('}')
        .withName("'}'")
val CARET: Parser<Char> = char('^')
        .withName("'^'")
val COMMA: Parser<Char> = char(',')
        .withName("','")
val MINUS: Parser<Char> = char('-')
        .withName("'-'")
val PLUS: Parser<Char> = char('+')
        .withName("'+'")
val SLASH: Parser<Char> = char('/')
        .withName("'/'")
val BACKSLASH: Parser<Char> = char('\\')
        .withName("'\\'")
val GREATER_THAN_SIGN: Parser<Char> = char('>')
        .withName("'>'")
val LESS_THAN_SIGN: Parser<Char> = char('<')
        .withName("'<'")
val LEFT_PAREN: Parser<Char> = char('(')
        .withName("'('")
val RIGHT_PAREN: Parser<Char> = char(')')
        .withName("')'")
val DOT: Parser<Char> = char('.')
        .withName("'.'")
val UNDERSCORE: Parser<Char> = char('_')
        .withName("'_'")
val VERTICAL_BAR: Parser<Char> = char('|')
        .withName("'|'")
val AMPERSAND: Parser<Char> = char('&')
        .withName("'&'")
val QUESTION_MARK: Parser<Char> = char('?')
        .withName("'?'")
val EQUALS_SIGN: Parser<Char> = char('=')
        .withName("'='")
val EXCLAMATION_MARK: Parser<Char> = char('!')
        .withName("'!'")
val AT_SIGN: Parser<Char> = char('@')
        .withName("'@'")
val HASH: Parser<Char> = char('#')
        .withName("'#'")

val DIGIT: Parser<Char> = anyOf('0'..'9')
        .withName("DIGIT")
val NUMBER: Parser<String> = (oneOrMore(DIGIT))
        .map { digits: List<Char> -> digits.joinToString("") }
        .withName("NUMBER")
val IDENTIFIER: Parser<String> = oneOrMore(anyOf(('a'..'z') + ('A'..'Z') + ('0'..'9') + '_'))
        .map { it: List<Char> -> it.joinToString("") }
        .withName("IDENTIFIER")

object Numbers {
    private fun <A> listOf(head: A, tail: List<A>): List<A> = arrayListOf(head).apply { addAll(tail) }

    private fun List<Char>.digitsToInt(): Int {
        val (multiplier: Int, result: Int) =
                this.foldRight(1 to 0) { digit: Char, (multiplier: Int, result: Int) ->
                    (multiplier * 10) to (result + Character.getNumericValue(digit) * multiplier) }
        return result
    }

    // fragment zeroInteger: (Int) = '0' { ... };
    private val zeroInteger: Parser<Int> = char('0') map { 0 }
    // fragment positiveInteger: (Int) = [1-9] ([0-9])* { ... };
    private val positiveInteger: Parser<Int> = (anyOf('1'..'9') and zeroOrMore(DIGIT))
            .map { (headDigit: Char, tailDigits: List<Char>) -> listOf(headDigit, tailDigits).digitsToInt() }
    // fragment zeroOrPositiveInteger: (Int) = zeroInteger | positiveInteger;
    private val zeroOrPositiveInteger: Parser<Int> = zeroInteger or positiveInteger
    // INTEGER: (Int) = ('-')? positiveInteger { ... };
    val INTEGER: Parser<Int> = (optional(char('-')) and zeroOrPositiveInteger)
            .map { (minus: Char?, integer: Int) ->
                if (minus == null) integer else -integer }
            .withName("INTEGER")

    // fragment exp: (Pair<Char, Int>) = [Ee] > ([+\-])? positiveInteger { ... };
    private val exp: Parser<Pair<Char, Int>> = (anyOf('E', 'e') then optional(anyOf('+', '-')) and zeroOrPositiveInteger)
            .map { (operator: Char?, integer: Int) ->
                if (operator == null) '+' to integer else operator to integer }

    // fragment zeroOrPositiveFloating: (Double) = INTEGER < '.' (DIGIT)+ { ... };
    @Suppress("RemoveCurlyBracesFromTemplate")
    private val zeroOrPositiveFloating: Parser<Double> = (INTEGER before DOT and oneOrMore(DIGIT))
            .map { (integer: Int, fracDigits: List<Char>) ->
                "${integer}.${fracDigits.joinToString("")}".toDouble() }

    // fragment floatingWithExp: (Number) = FLOATING (exp)? { ... };
    private val floatingWithExp: Parser<Double> = (zeroOrPositiveFloating and optional(exp))
            .map { (floating: Double, exp: Pair<Char, Int>?) ->
                if (exp != null) {
                    val (expOperator: Char, expInt: Int) = exp
                    if (expOperator == '+')
                        (floating * Math.pow(10.0, expInt.toDouble()))
                    else // expOperator will be always '-'
                        (floating / Math.pow(10.0, expInt.toDouble()))
                } else {
                    floating
                }
            }
    // FLOATING: (Double) = ('-')? positiveInteger { ... };
    val FLOATING: Parser<Double> = (optional(char('-')) and floatingWithExp)
            .map { (minus: Char?, floating: Double) ->
                if (minus == null) floating else -floating }
            .withName("FLOATING")

    // fragment numberFromInteger: (Number) = INTEGER (exp)? { ... };
    @Suppress("USELESS_CAST")
    private val numberFromInteger: Parser<Number> = (INTEGER and optional(exp))
            .map { (integer: Int, exp: Pair<Char, Int>?) ->
                // Compiler implicitly casts Double and Int results to Any, so I have to use explicit cast
                if (exp != null) {
                    val (expOperator: Char, expInt: Int) = exp
                    if (expOperator == '+')
                        (integer.toDouble() * Math.pow(10.0, expInt.toDouble())) as Number    // <-- Double
                    else // expOperator will be always '-'
                        (integer.toDouble() / Math.pow(10.0, expInt.toDouble())) as Number    // <-- Double
                } else {
                    integer as Number    // <-- Int
                }
            }
    // NUMBER: (Number) = FLOATING | numberFromInteger;
    val NUMBER: Parser<Number> = (FLOATING or numberFromInteger)
            .withName("NUMBER")
}
