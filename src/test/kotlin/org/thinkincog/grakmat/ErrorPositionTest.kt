/*
 * Copyright (c) 2016 ThinkInCog.org
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

package org.thinkincog.grakmat

import org.junit.*
import org.junit.Assert.*

class ErrorPositionTest {
    private val inputText = "ab\ncd\nef"

    @Test fun testFirstLineAndFirstColumn() {
        assertEquals(ErrorPosition(1 to 1, "ab"), inputText.errorPosition(0))
    }

    @Test fun testFirstLineAndSecondColumn() {
        assertEquals(ErrorPosition(1 to 2, "ab"), inputText.errorPosition(1))
    }

    @Test fun testFirstLineAndLastColumn() {
        assertEquals(ErrorPosition(1 to 3, "ab"), inputText.errorPosition(2))
    }

    @Test fun testSecondLineAndFirstColumn() {
        assertEquals(ErrorPosition(2 to 1, "cd"), inputText.errorPosition(3))
    }

    @Test fun testSecondLineAndSecondColumn() {
        assertEquals(ErrorPosition(2 to 2, "cd"), inputText.errorPosition(4))
    }

    @Test fun testSecondLineAndLastColumn() {
        assertEquals(ErrorPosition(2 to 3, "cd"), inputText.errorPosition(5))
    }

    @Test fun testCRLF() {
        assertEquals(ErrorPosition(2 to 1, "b"), "a\r\nb\r\nc".errorPosition(3))
    }
}
