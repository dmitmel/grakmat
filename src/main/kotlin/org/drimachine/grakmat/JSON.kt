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

@file:JvmName("JSON")

package org.drimachine.grakmat

import kotlin.system.exitProcess

/**
 * Adds `this` value to the head of list.
 *
 * @receiver Head of the result.
 * @param list Tail of the result
 */
private operator fun <T> T.plus(list: List<T>): List<T> {
    val result = arrayListOf<T>()
    result += this
    result += list
    return result.toList()
}

// Forward references
private val jsonRef:  Parser<Map<String, Any?>> = ref { json }
private val valueRef: Parser<Any?>              = ref { value }

private val leftSquareBracket:  Parser<Char> = char('[') withName "\'[\'"
private val rightSquareBracket: Parser<Char> = char(']') withName "\']\'"
// values: value (',' value)*
private val values: Parser<List<Any?>> = valueRef and zeroOrMore(char(',') then valueRef) map { it: Pair<Any?, List<Any?>> -> it.first + it.second }
// array: '[' values ']'
private val array: Parser<List<Any?>> = leftSquareBracket then values before rightSquareBracket

// Digit: [0-9]
private val digit: Parser<Char> = anyOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
// integer: digit | digit digit | digit digit digit | ... | digit digit digit digit digit digit digit digit digit
private val integer: Parser<Int> = digit inRange 1..9 map ::digitsToInteger
// float: integer '.' (digit)+
private val float: Parser<Float> = integer before char('.') and oneOrMore(digit) map ::partsToFloat
// number: float | integer
@Suppress("UNCHECKED_CAST")
private val number:  Parser<Number> = ((float or integer) as Parser<Number>) withName "number"

private fun digitsToInteger(digits: List<Char>): Int = digits.joinToString("").toInt()

private fun partsToFloat(parts: Pair<Int, List<Char>>): Float {
    val (naturalPart, fracDigits) = parts
    val frac = fracDigits.joinToString("")
    return "$naturalPart.$frac".toFloat()
}

// EscapedCharacter: '\\' ('"' | '\\' | 'b' | 'f' | 'n' | 'r' | 't' | 'v')
private val escapedCharacter: Parser<Char> = char('\\') then anyOf('\"', '\\', 'b', 'f', 'n', 'r', 't', 'v') map {
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
private val quote:     Parser<Char> = char('\"') withName "\'\"\'"
// stringLiteral: '"' (Character)+ '"'
private val stringLiteral: Parser<String> = quote then zeroOrMore(character) before quote map { it.joinToString("") } withName "string literal"

private val colon:      Parser<Char>    = char(':') withName "\':\'"
private val trueValue:  Parser<Boolean> = str("true")  map { true }
private val falseValue: Parser<Boolean> = str("false") map { false }
private val nullValue:  Parser<Any?>    = str("null")  map { null }
// value: stringLiteral | number | json | array | 'true' | 'false' | 'null'
private val value: Parser<Any?> = stringLiteral or number or jsonRef or array or trueValue or falseValue or nullValue withName "value"
// pair: stringLiteral ':' value
private val pair: Parser<Pair<String, Any?>> = stringLiteral before colon and value withName "pair"

private val leftBrace:  Parser<Char> = char('{') withName "\'{\'"
private val rightBrace: Parser<Char> = char('}') withName "\'}\'"
// emptyObject: '{' '}'
private val emptyObject: Parser<Map<String, Any?>> = leftBrace and rightBrace map { emptyMap<String, Any>() } withName "empty object"
// pairs: pair (',' pair)*
private val pairs: Parser<List<Pair<String, Any?>>> = pair and zeroOrMore(char(',') then pair) map { it: Pair<Pair<String, Any?>, List<Pair<String, Any?>>> -> it.first + it.second }
// jsonObject: '{' pairs '}'
private val jsonObject: Parser<Map<String, Any?>> = leftBrace then pairs before rightBrace map { it.toMap() } withName "object"
// json: jsonObject | emptyObject
private val json: Parser<Map<String, Any?>> = jsonObject or emptyObject

fun main(args: Array<String>) {
    while (true) {
        try {
            print(">>> ")
            val input = readLine() ?: exitProcess(0)
            val result = json.parse(input)
            println(result)
        } catch (e: ParseException) {
            println(e.message)
        }
    }
}
