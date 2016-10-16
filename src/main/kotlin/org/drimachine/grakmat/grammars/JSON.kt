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

import org.drimachine.grakmat.*
import java.io.File

object JSON {
    private fun <A> listOf(head: A, tail: List<A>): List<A> = arrayListOf(head).apply { addAll(tail) }

    // Forward references
    private val jsonObjectRef: Parser<Map<String, Any?>> = ref { jsonObject }
    private val valueRef: Parser<Any?> = ref { value }

    private val leftSquareBracket: Parser<Char> = chr('[') withName "\'[\'"
    private val rightSquareBracket: Parser<Char> = chr(']') withName "\']\'"
    // values: value (',' value)*
    private val values: Parser<List<Any?>> = (valueRef _and_ _zeroOrMore_(chr(',') _then_ valueRef))
            .map { it: Pair<Any?, List<Any?>> -> listOf(it.first, it.second) }
    // array: '[' values ']'
    private val array: Parser<List<Any?>> = (leftSquareBracket _then_ values _before_ rightSquareBracket)
            .withName("array")

    // Digit: [0-9]
    private val digit: Parser<Char> = anyOf('0'..'9')
    // integer: digit | digit digit | digit digit digit | ... | digit digit digit digit digit digit digit digit digit
    private val integer: Parser<Int> = digit inRange 1..9 map { digits: List<Char> -> digitsToInteger(digits) }
    // float: integer '.' (digit)+
    private val float: Parser<Float> = integer before chr('.') and oneOrMore(digit) map { parts: Pair<Int, List<Char>> -> partsToFloat(parts) }
    // number: float | integer
    @Suppress("UNCHECKED_CAST")
    private val number: Parser<Number> = ((float or integer) as Parser<Number>)
            .withName("number")

    private fun digitsToInteger(digits: List<Char>): Int = digits.joinToString("").toInt()

    private fun partsToFloat(parts: Pair<Int, List<Char>>): Float {
        val (naturalPart, fracDigits) = parts
        val frac = fracDigits.joinToString("")
        return "$naturalPart.$frac".toFloat()
    }

    // EscapedCharacter: '\\' ('"' | '\\' | 'b' | 'f' | 'n' | 'r' | 't' | 'v')
    private val escapedCharacter: Parser<Char> = chr('\\') then anyOf('\"', '\\', 'b', 'f', 'n', 'r', 't', 'v') map {
        when (it) {
            '\"' -> '\"'
            '\\' -> '\\'
            'b'  -> '\b'
            'f'  -> 0x0C.toChar()
            'n'  -> '\n'
            'r'  -> '\r'
            't'  -> '\t'
            'v'  -> 0x0B.toChar()
            else -> error("unreachable code")
        }
    }
    // Character: ~('"' | '\\') | EscapedCharacter
    private val character: Parser<Char> = except('\"', '\\') or escapedCharacter withName "character"
    private val quote: Parser<Char> = chr('\"') withName "\'\"\'"
    // stringLiteral: '"' (Character)+ '"'
    private val stringLiteral: Parser<String> = (quote then zeroOrMore(character) before quote)
            .map { it.joinToString("") }
            .withName("string literal")

    private val colon: Parser<Char> = chr(':') withName "\':\'"
    private val trueValue: Parser<Boolean> = str("true")  map { true }
    private val falseValue: Parser<Boolean> = str("false") map { false }
    private val nullValue: Parser<Any?> = str("null")  map { null }
    // value: stringLiteral | number | json | array | 'true' | 'false' | 'null'
    private val value: Parser<Any?> = (stringLiteral or number or jsonObjectRef or array or trueValue or falseValue or nullValue)
            .withName("value")
    // pair: stringLiteral ':' value
    private val pair: Parser<Pair<String, Any?>> = (stringLiteral _before_ colon _and_ value)
            .withName("pair")

    private val leftBrace: Parser<Char> = chr('{') withName "\'{\'"
    private val rightBrace: Parser<Char> = chr('}') withName "\'}\'"
    // pairs: pair (',' pair)*
    private val pairs: Parser<List<Pair<String, Any?>>> = (pair _and_ _zeroOrMore_(chr(',') _then_ pair))
            .map { it: Pair<Pair<String, Any?>, List<Pair<String, Any?>>> -> listOf(it.first, it.second) }
    // jsonObject: '{' pairs '}' | '{' '}'
    private val jsonObject: Parser<Map<String, Any?>> =
            (OPTIONAL_SPACES then (
                    (leftBrace _then_ pairs _before_ rightBrace map { it: List<Pair<String, Any?>> -> it.toMap() })
                            or
                            (leftBrace _and_ rightBrace map { emptyMap<String, Any?>() })
                    ) before OPTIONAL_SPACES)
                    .withName("object")

    @JvmStatic fun parse(json: String): Map<String, Any?> = jsonObject.parse(json)
    @JvmStatic fun parseFile(file: File): Map<String, Any?> = jsonObject.parseFile(file)
}
