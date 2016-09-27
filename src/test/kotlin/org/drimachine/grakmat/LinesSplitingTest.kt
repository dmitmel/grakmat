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

package org.drimachine.grakmat

import org.junit.*
import org.junit.Assert.*

class LinesSplitingTest {
    @Test fun testLF() {
        assertEquals(
                listOf("a\n", "b\n", "c"),
                "a\nb\nc".linesWithSeparators()
        )
    }

    @Test fun testCR() {
        assertEquals(
                listOf("a\r", "b\r", "c"),
                "a\rb\rc".linesWithSeparators()
        )
    }

    @Test fun testCRLF() {
        assertEquals(
                listOf("a\r\n", "b\r\n", "c"),
                "a\r\nb\r\nc".linesWithSeparators()
        )
    }

    @Test fun testEmptyLinesAndLF() {
        assertEquals(
                listOf("a\n", "\n", "c"),
                "a\n\nc".linesWithSeparators()
        )
    }

    @Test fun testEmptyLinesAndCR() {
        assertEquals(
                listOf("a\r", "\r", "c"),
                "a\r\rc".linesWithSeparators()
        )
    }

    @Test fun testEmptyLinesAndCRLF() {
        assertEquals(
                listOf("a\r\n", "\r\n", "c"),
                "a\r\n\r\nc".linesWithSeparators()
        )
    }

    @Test fun testLastLF() {
        assertEquals(
                listOf("a\n", "b\n", "c\n"),
                "a\nb\nc\n".linesWithSeparators()
        )
    }

    @Test fun testLastCR() {
        assertEquals(
                listOf("a\r", "b\r", "c\r"),
                "a\rb\rc\r".linesWithSeparators()
        )
    }

    @Test fun testLastCRLF() {
        assertEquals(
                listOf("a\r\n", "b\r\n", "c\r\n"),
                "a\r\nb\r\nc\r\n".linesWithSeparators()
        )
    }
}
