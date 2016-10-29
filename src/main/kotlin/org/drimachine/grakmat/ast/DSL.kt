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

package org.drimachine.grakmat.ast

import java.util.*

fun astNode(name: String): Node = Node(name, "", arrayListOf<Node>())

fun astNode(name: String, value: String): Node = Node(name, value, arrayListOf<Node>())

fun astNode(name: String, children: ChildrenDSL.() -> Unit): Node {
    val childrenDSL = ChildrenDSL()
    childrenDSL.children()
    return Node(name, "", childrenDSL.list)
}

fun astNode(name: String, value: String, children: ChildrenDSL.() -> Unit): Node {
    val childrenDSL = ChildrenDSL()
    childrenDSL.children()
    return Node(name, value, childrenDSL.list)
}

fun astNode(name: String, children: List<Node>): Node = Node(name, "", children)

fun astNode(name: String, value: String, children: List<Node>): Node = Node(name, value, children)

class ChildrenDSL {
    val list: ArrayList<Node> = arrayListOf()

    fun add(node: Node) { list += node }

    operator fun Node.unaryPlus() { list += this }
}
