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
import org.drimachine.grakmat.ast.*
import java.io.File

object GrammarDefinitionLanguage {
    private val expressionRef: Parser<Node> = ref { expression }

    // fragment inRangeCombinator = '(' -> expression <- ')' <- '{', NUMBER <- ',', NUMBER <- '}' { ... };
    private val inRangeCombinator: Parser<Node> = (LEFT_PAREN _then_ expressionRef _before_ RIGHT_PAREN _before_ LEFT_BRACE _and_ NUMBER _before_ COMMA _and_ NUMBER _before_ RIGHT_BRACE)
            .map { (expressionAndMin, max: String) ->
                val (expression: Node, min: String) = expressionAndMin
                astNode("inRange") {
                    +astNode("min", min)
                    +astNode("max", max)
                    +astNode("expression", listOf(expression))
                }
            }
    // fragment spacedInRangeCombinator = '(' -> expression <- ')' <- '-' <- '{', NUMBER <- ',', NUMBER <- '}' { ... };
    private val spacedInRangeCombinator: Parser<Node> = (LEFT_PAREN _then_ expressionRef _before_ RIGHT_PAREN _before_ MINUS _before_ LEFT_BRACE _and_ NUMBER _before_ COMMA _and_ NUMBER _before_ RIGHT_BRACE)
            .map { (expressionAndMin, max: String) ->
                val (expression: Node, min: String) = expressionAndMin
                astNode("_inRange_") {
                    +astNode("min", min)
                    +astNode("max", max)
                    +astNode("expression", listOf(expression))
                }
            }
    // fragment atLeastCombinator = '(' -> expression <- ')' <- '{', NUMBER <- ',' <- '}' { ... };
    private val atLeastCombinator: Parser<Node> = (LEFT_PAREN _then_ expressionRef _before_ RIGHT_PAREN _before_ LEFT_BRACE _and_ NUMBER _before_ COMMA _before_ RIGHT_BRACE)
            .map { (expression: Node, number: String) ->
                astNode("atLeast") {
                    +astNode("times", number)
                    +astNode("expression", listOf(expression))
                }
            }
    // fragment spacedAtLeastCombinator = '(' -> expression <- ')' <- '-' <- '{', NUMBER <- ',' <- '}' { ... };
    private val spacedAtLeastCombinator: Parser<Node> = (LEFT_PAREN _then_ expressionRef _before_ RIGHT_PAREN _before_ MINUS _before_ LEFT_BRACE _and_ NUMBER _before_ COMMA _before_ RIGHT_BRACE)
            .map { (expression: Node, number: String) ->
                astNode("_atLeast_") {
                    +astNode("times", number)
                    +astNode("expression", listOf(expression))
                }
            }
    // fragment repeatCombinator = '(' -> expression <- ')' <- '{', NUMBER <- '}' { ... };
    private val repeatCombinator: Parser<Node> = (LEFT_PAREN _then_ expressionRef _before_ RIGHT_PAREN _before_ LEFT_BRACE _and_ NUMBER _before_ RIGHT_BRACE)
            .map { (expression: Node, number: String) ->
                astNode("repeat") {
                    +astNode("times", number)
                    +astNode("expression", listOf(expression))
                }
            }
    // fragment spacedRepeatCombinator = '(' -> expression <- ')' <- '-' <- '{', NUMBER <- '}' { ... };
    private val spacedRepeatCombinator: Parser<Node> = (LEFT_PAREN _then_ expressionRef _before_ RIGHT_PAREN _before_ MINUS _before_ LEFT_BRACE _and_ NUMBER _before_ RIGHT_BRACE)
            .map { (expression: Node, number: String) ->
                astNode("_repeat_") {
                    +astNode("times", number)
                    +astNode("expression", listOf(expression))
                }
            }
    // fragment optionalCombinator = '(' -> expression <- ')' <- '?' { ... };
    private val optionalCombinator: Parser<Node> = (LEFT_PAREN _then_ expressionRef _before_ RIGHT_PAREN _before_ QUESTION_MARK)
            .map { expression: Node ->
                astNode("optional") {
                    +astNode("expression", listOf(expression))
                }
            }
    // fragment requiredCombinator = '(' -> expression <- ')' <- '!' { ... };
    private val requiredCombinator: Parser<Node> = (LEFT_PAREN _then_ expressionRef _before_ RIGHT_PAREN _before_ EXCLAMATION_MARK)
            .map { expression: Node ->
                astNode("required") {
                    +astNode("expression", listOf(expression))
                }
            }
    // fragment zeroOrMoreCombinator = '(' -> expression <- ')' <- '*' { ... };
    private val zeroOrMoreCombinator: Parser<Node> = (LEFT_PAREN _then_ expressionRef _before_ RIGHT_PAREN _before_ ASTERISK)
            .map { expression: Node ->
                astNode("zeroOrMore") {
                    +astNode("expression", listOf(expression))
                }
            }
    // fragment spacedZeroOrMoreCombinator = '(' -> expression <- ')' <- '-' <- '*' { ... };
    private val spacedZeroOrMoreCombinator: Parser<Node> = (LEFT_PAREN _then_ expressionRef _before_ RIGHT_PAREN _before_ MINUS _before_ ASTERISK)
            .map { expression: Node ->
                astNode("_zeroOrMore_") {
                    +astNode("expression", listOf(expression))
                }
            }
    // fragment oneOrMoreCombinator = '(' -> expression <- ')' <- '+' { ... };
    private val oneOrMoreCombinator: Parser<Node> = (LEFT_PAREN _then_ expressionRef _before_ RIGHT_PAREN _before_ PLUS)
            .map { expression: Node ->
                astNode("oneOrMore") {
                    +astNode("expression", listOf(expression))
                }
            }
    // fragment spacedOneOrMoreCombinator = '(' -> expression <- ')' <- '-' <- '+' { ... };
    private val spacedOneOrMoreCombinator: Parser<Node> = (LEFT_PAREN _then_ expressionRef _before_ RIGHT_PAREN _before_ MINUS _before_ PLUS)
            .map { expression: Node ->
                astNode("_oneOrMore_") {
                    +astNode("expression", listOf(expression))
                }
            }
    // countCombinator = requiredCombinator | optionalCombinator | zeroOrMoreCombinator | spacedZeroOrMoreCombinator | oneOrMoreCombinator | spacedOneOrMoreCombinator | spacedRepeatCombinator | repeatCombinator | spacedAtLeastCombinator | atLeastCombinator | spacedInRangeCombinator | inRangeCombinator;
    private val countCombinator: Parser<Node> = (requiredCombinator or optionalCombinator or zeroOrMoreCombinator or spacedZeroOrMoreCombinator or oneOrMoreCombinator or spacedOneOrMoreCombinator or spacedRepeatCombinator or repeatCombinator or spacedAtLeastCombinator or atLeastCombinator or spacedInRangeCombinator or inRangeCombinator)
            .withName("count combinator")

    // fragment escapedCharacter: Char = '\\' > ["'\\bnrt] { ... };
    private val escapedCharacter: Parser<Char> = (char('\\') then anyOf('\"', '\'', '\\', 'b', 'n', 'r', 't'))
            .map {
                when (it) {
                    '\"' -> '\"'
                    '\'' -> '\''
                    '\\' -> '\\'
                    'b'  -> '\b'
                    'n'  -> '\n'
                    'r'  -> '\r'
                    't'  -> '\t'
                    else -> error("unreachable code")
                }
            }
    // fragment stringCharacter: (Char) = [^"\\] | escapedCharacter;
    private val stringCharacter: Parser<Char> = (except('\"', '\\') or escapedCharacter)
    // stringLiteral = '"' > (stringCharacter)* < '"' { ... };
    private val stringLiteral: Parser<Node> = (DOUBLE_QUOTE then zeroOrMore(stringCharacter) before DOUBLE_QUOTE)
            .map { chars: List<Char> -> astNode("string", chars.joinToString("")) }
            .withName("string literal")
    // characterLiteral = '\'' > stringCharacter < '\'' { ... };
    private val characterLiteral: Parser<Node> = (QUOTE then stringCharacter before QUOTE)
            .map { char: Char -> astNode("string", char.toString()) }
            .withName("character literal")

    // fragment groupEscapedCharacter: (Char) = '\\' > [\^]\\-bnrt] { ... };
    private val groupEscapedCharacter: Parser<Char> = (char('\\') then anyOf('^', ']', '\\', '-', 'b', 'n', 'r', 't'))
            .map {
                when (it) {
                    '^' -> '^'
                    ']' -> ']'
                    '\\' -> '\\'
                    '-' -> '-'
                    'b'  -> '\b'
                    'n'  -> '\n'
                    'r'  -> '\r'
                    't'  -> '\t'
                    else -> error("unreachable code")
                }
            }
    // groupCharacter: (Char) = [^\]\\] or groupEscapedCharacter;
    private val groupCharacter: Parser<Char> = (except(']', '\\') or groupEscapedCharacter)
            .withName("character")
    // groupCharacterNode = groupCharacter { ... };
    private val groupCharacterNode: Parser<Node> = groupCharacter map { astNode("char", it.toString()) }
    // rangeExpression = groupCharacter < '-' groupCharacter { ... };
    private val rangeExpression: Parser<Node> = (groupCharacter before MINUS and groupCharacter)
            .map { (firstChar: Char, lastChar: Char) -> astNode("range", listOf(astNode("firstChar", firstChar.toString()), astNode("lastChar", lastChar.toString()))) }
            .withName("range expression")
    // anyOfParser = '[' > (groupCharacterNode | rangeExpression)+ < ']' { ... };
    private val anyOfParser: Parser<Node> = (LEFT_BRACKET then oneOrMore(groupCharacterNode or rangeExpression) before RIGHT_BRACKET)
            .map { chars: List<Node> -> astNode("anyOf", chars) }
            .withName("including group")
    // exceptParser = '[' > '^' > (groupCharacterNode | rangeExpression)+ < ']' { ... };
    private val exceptParser: Parser<Node> = (LEFT_BRACKET then CARET then oneOrMore(groupCharacterNode or rangeExpression) before RIGHT_BRACKET)
            .map { chars: List<Node> -> astNode("except", chars) }
            .withName("excluding group")

    // anyCharacter = '*';
    private val anyCharParser: Parser<Node> = ASTERISK
            .map { astNode("anyChar") }
            .withName("any character")

    // grouping = '(' -> expression <- ')' { ... };
    private val grouping: Parser<Node> = (LEFT_PAREN _then_ expressionRef _before_ RIGHT_PAREN)
            .map { expression: Node -> astNode("grouping", listOf(expression)) }
            .withName("grouping")
    // parserCreator = stringLiteral | characterLiteral | exceptParser | anyOfParser | anyCharParser;
    private val parserCreator: Parser<Node> = (stringLiteral or characterLiteral or exceptParser or anyOfParser or anyCharParser)
            .withName("parser creator")
    // ruleReference = IDENTIFIER { ... };
    private val ruleReference: Parser<Node> = (IDENTIFIER)
            .map { ruleName: String -> astNode("ruleReference", ruleName) }
            .withName("rule reference")
    // baseExpression = countCombinator | ruleReference | parserCreator | grouping;
    private val baseExpression: Parser<Node> = (countCombinator or ruleReference or parserCreator or grouping)
            .withName("base expression")

    // fragment thenCombinator = baseExpression <- '>', expression { ... };
    private val thenCombinator: Parser<Node> = (baseExpression _before_ GREATER_THAN_SIGN _and_ expressionRef)
            .map { (leftExpression: Node, rightExpression: Node) ->
                astNode("then") {
                    +astNode("left", listOf(leftExpression))
                    +astNode("right", listOf(rightExpression))
                }
            }
    // fragment spacedThenCombinator = baseExpression <- '-' < '>', expression { ... };
    private val spacedThenCombinator: Parser<Node> = (baseExpression _before_ MINUS before GREATER_THAN_SIGN _and_ expressionRef)
            .map { (leftExpression: Node, rightExpression: Node) ->
                astNode("_then_") {
                    +astNode("left", listOf(leftExpression))
                    +astNode("right", listOf(rightExpression))
                }
            }
    // fragment beforeCombinator = baseExpression <- '<' expression { ... };
    private val beforeCombinator: Parser<Node> = (baseExpression _before_ LESS_THAN_SIGN _and_ expressionRef)
            .map { (leftExpression: Node, rightExpression: Node) ->
                astNode("before") {
                    +astNode("left", listOf(leftExpression))
                    +astNode("right", listOf(rightExpression))
                }
            }
    // fragment spacedBeforeCombinator = baseExpression <- '<' < '-', expression { ... };
    private val spacedBeforeCombinator: Parser<Node> = (baseExpression _before_ LESS_THAN_SIGN before MINUS _and_ expressionRef)
            .map { (leftExpression: Node, rightExpression: Node) ->
                astNode("_before_") {
                    +astNode("left", listOf(leftExpression))
                    +astNode("right", listOf(rightExpression))
                }
            }
    // fragment andCombinator = expression < SPACES expression { ... };
    private val andCombinator: Parser<Node> = (baseExpression before SPACES and expressionRef)
            .map { (leftExpression: Node, rightExpression: Node) ->
                astNode("and") {
                    +astNode("left", listOf(leftExpression))
                    +astNode("right", listOf(rightExpression))
                }
            }
    // fragment spacedAndCombinator = baseExpression <- ',', expression { ... };
    private val spacedAndCombinator: Parser<Node> = (baseExpression _before_ COMMA _and_ expressionRef)
            .map { (leftExpression: Node, rightExpression: Node) ->
                astNode("_and_") {
                    +astNode("left", listOf(leftExpression))
                    +astNode("right", listOf(rightExpression))
                }
            }
    // fragment orCombinator = expression <- '|', expression { ... };
    private val orCombinator: Parser<Node> = (baseExpression _before_ VERTICAL_BAR _and_ expressionRef)
            .map { (leftExpression: Node, rightExpression: Node) ->
                astNode("or") {
                    +astNode("left", listOf(leftExpression))
                    +astNode("right", listOf(rightExpression))
                }
            }
    // combinator = orCombinator | spacedAndCombinator | andCombinator | spacedThenCombinator | thenCombinator | beforeCombinator | spacedBeforeCombinator;
    private val combinator: Parser<Node> = (orCombinator or spacedAndCombinator or andCombinator or spacedThenCombinator or thenCombinator or beforeCombinator or spacedBeforeCombinator)
            .withName("combinator")

    // expression = combinator | baseExpression;
    private val expression: Parser<Node> = (combinator or baseExpression)
            .withName("expression")

    private val nestedBlockRef: Parser<String> = ref { nestedBlock }
    // fragment codeChar: (String) = [^{}];
    private val codeChar: Parser<String> = except('{', '}') map Char::toString
    // fragment nestedBlock: (String) = '{' > (nestedBlock | codeChar) < '}';
    private val nestedBlock: Parser<String> = (LEFT_BRACE then zeroOrMore(nestedBlockRef or codeChar) before RIGHT_BRACE)
            .map { it: List<String> -> "{${it.joinToString("")}}" }
    // code: (String) = '{' > (nestedBlock | codeChar)* < '}' { ... };
    private val code: Parser<String> = (LEFT_BRACE then zeroOrMore(nestedBlock or codeChar) before RIGHT_BRACE)
            .map { it: List<String> -> it.joinToString("") }
            .withName("code")

    // ruleType: (String) = '(' -> ([^)])+ <- ')' { ... };
    private val ruleType: Parser<String> = (char('(') _then_ oneOrMore(except(')')) _before_ char(')'))
            .map { it: List<Char> -> it.joinToString("") }
            .withName("type")
    // rule = ("fragment")? < SPACES IDENTIFIER, (':' -> ruleType) <- '=', expression, (code)? { ... };
    private val rule: Parser<Node> = (optional(str("fragment")) before SPACES and IDENTIFIER _and_ optional(COLON _then_ ruleType) _before_ EQUALS_SIGN _and_ expression _and_ optional(code))
            .map { it: Pair<Pair<Pair<Pair<String?, String>, String?>, Node>, String?> ->
                val (left1, code: String?) = it
                val (left2, expression: Node) = left1
                val (left3, type: String?) = left2
                val (fragment: String?, name: String) = left3

                astNode("rule") {
                    if (fragment != null)
                        +astNode("fragment")
                    +astNode("name", name)
                    if (type != null)
                        +astNode("type", type)
                    +astNode("expression", listOf(expression))
                    if (code != null)
                        +astNode("code", code)
                }
            }
            .withName("rule")
    // rule = IDENTIFIER, (':' -> ruleType) <- '=', expression, (code)? { ... };
    private val mainRule: Parser<Node> = (IDENTIFIER _and_ optional(COLON _then_ ruleType) _before_ EQUALS_SIGN _and_ expression _and_ code)
            .map { it: Pair<Pair<Pair<String, String?>, Node>, String?> ->
                val (left1, code: String?) = it
                val (left2, expression: Node) = left1
                val (name: String, type: String?) = left2
                astNode("mainRule") {
                    +astNode("name", name)
                    if (type != null)
                        +astNode("type", type)
                    +astNode("expression", listOf(expression))
                    if (code != null)
                        +astNode("code", code)
                }
            }
            .withName("main rule")

    // grammarName = "grammar" < SPACES > IDENTIFIER { ... };
    private val grammarName: Parser<Node> = (str("grammar") before SPACES then IDENTIFIER)
            .map { name: String -> astNode("name", name) }
            .withName("grammar name")

    // grammar = OPTIONAL_SPACES > grammarName <- ';', mainRule <- ';', (rule <- ';')* { ... };
    private val grammar: Parser<Node> = (OPTIONAL_SPACES then grammarName _before_ SEMICOLON _and_ mainRule _before_ SEMICOLON _and_ _zeroOrMore_(rule _before_ SEMICOLON) before OPTIONAL_SPACES)
            .map { grammarParts: Pair<Pair<Node, Node>, List<Node>> ->
                val (left1, rules: List<Node>) = grammarParts
                val (grammarName: Node, mainRule: Node) = left1

                astNode("grammar") {
                    +grammarName
                    +mainRule
                    +astNode("rules", rules)
                }
            }
            .withName("grammar")

    @JvmStatic fun parse(json: String): Node = grammar.parse(json)
    @JvmStatic fun parseFile(file: File): Node = grammar.parseFile(file)
}
