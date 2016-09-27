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

import org.junit.Test

import org.junit.Assert.*

class StringLengthBoundingTest {
    @Test(expected = IllegalArgumentException::class)
    fun boundLengthInEmptyString() {
        "".boundLengthTo(10)
    }

    @Test(expected = IllegalArgumentException::class)
    fun boundLengthWithNegativeValue() {
        "abc".boundLengthTo(-1)
    }

    @Test
    fun boundLengthWithoutEllipsis() {
        assertEquals("abc", "abc".boundLengthTo(5))
    }

    @Test
    fun boundLengthWithEllipsis() {
        assertEquals("12345...", "1234567890".boundLengthTo(5))
    }

    @Test
    fun boundLengthWithCustomEllipsis() {
        assertEquals("12345, too long input", "1234567890".boundLengthTo(5, ellipsis = ", too long input"))
    }
}
