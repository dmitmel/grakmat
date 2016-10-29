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

data class Node(val name: String, val value: String, val children: List<Node>) {
    override fun toString(): String = toStringRecursive(this)

    private fun toStringRecursive(node: Node, depth: Int = 0): String = buildString {
        append("  ".repeat(depth))
        append(node.name)
        if (node.value.isNotEmpty())
            append(": ${node.value}")
        node.children.forEach {
            append('\n')
            append(toStringRecursive(it, depth + 1))
        }
    }
}
