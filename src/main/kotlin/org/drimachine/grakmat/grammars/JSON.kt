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

    private val jsonObjectRef: Parser<Map<String, Any?>> = ref { jsonObject }
    private val valueRef: Parser<Any?> = ref { value }

    // values: (List<Any?>) = value, (',' -> value)* { ... };
    private val values: Parser<List<Any?>> = (valueRef _and_ _zeroOrMore_(char(',') _then_ valueRef))
            .map { (head: Any?, tail: List<Any?>) -> listOf(head, tail) }
            .withName("values")
    // array: (List<Any?>) = '[' -> values <- ']';
    private val array: Parser<List<Any?>> = (LEFT_BRACKET _then_ values _before_ RIGHT_BRACKET)
            .withName("array")

    private val number: Parser<Number> = Numbers.NUMBER
            .withName("number")

    // EscapedCharacter: '\\' ('"' | '\'' | '\\' | 'b' | 'f' | 'n' | 'r' | 't' | 'v')
    private val escapedCharacter: Parser<Char> = char('\\') then anyOf('\"', '\'', '\\', 'b', 'f', 'n', 'r', 't', 'v') map {
        when (it) {
            '\"' -> '\"'
            '\'' -> '\''
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
    private val quote: Parser<Char> = char('\"') withName "\'\"\'"
    // stringLiteral: '"' (Character)+ '"'
    private val stringLiteral: Parser<String> = (quote then zeroOrMore(character) before quote)
            .map { it.joinToString("") }
            .withName("string literal")

    private val colon: Parser<Char> = char(':') withName "\':\'"
    private val trueValue: Parser<Boolean> = str("true")  map { true }
    private val falseValue: Parser<Boolean> = str("false") map { false }
    private val nullValue: Parser<Any?> = str("null")  map { null }
    // value: stringLiteral | number | json | array | 'true' | 'false' | 'null'
    private val value: Parser<Any?> = (stringLiteral or number or jsonObjectRef or array or trueValue or falseValue or nullValue)
            .withName("value")
    // pair: stringLiteral ':' value
    private val pair: Parser<Pair<String, Any?>> = (stringLiteral _before_ colon _and_ value)
            .withName("pair")

    private val leftBrace: Parser<Char> = char('{') withName "\'{\'"
    private val rightBrace: Parser<Char> = char('}') withName "\'}\'"
    // pairs: pair (',' pair)*
    private val pairs: Parser<List<Pair<String, Any?>>> = (pair _and_ _zeroOrMore_(char(',') _then_ pair))
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
