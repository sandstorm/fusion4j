/*
 * MIT License
 *
 * Copyright (c) 2022 Sandstorm Media GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Authors:
 *  - Eric Kloss
 */

package io.neos.fusion4j.lang.semantic

import io.neos.fusion4j.lang.model.FusionPathName
import org.junit.Assert.assertEquals
import org.junit.Test

internal class KeyPositionTest {

    @Test
    fun test_parseNumeric_success() {
        assertKeyPosition<MiddlePosition>("  0123 ", "0123") { actual ->
            assertEquals("numeric index mismatch", 123, actual.numericIndex)
        }
    }

    @Test
    fun test_parseBefore_noWeight_success() {
        assertKeyPosition<BeforePosition>("before foo") { actual ->
            assertEquals("weight mismatch", 0, actual.weight)
            assertEquals("before key mismatch", FusionPathName.attribute("foo"), actual.beforeKey)
        }
    }

    @Test
    fun test_parseBefore_withWeight_success() {
        assertKeyPosition<BeforePosition>("before foo 100") { actual ->
            assertEquals("weight mismatch", 100, actual.weight)
            assertEquals("before key mismatch", FusionPathName.attribute("foo"), actual.beforeKey)
        }
    }

    @Test
    fun test_parseBefore_specialChars_success() {
        assertKeyPosition<BeforePosition>("before abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ:-_0123456789") { actual ->
            assertEquals("weight mismatch", 0, actual.weight)
            assertEquals(
                "before key mismatch",
                FusionPathName.attribute("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ:-_0123456789"),
                actual.beforeKey
            )
        }
    }

    @Test
    fun test_parseAfter_noWeight_success() {
        assertKeyPosition<AfterPosition>("after foo") { actual ->
            assertEquals("weight mismatch", 0, actual.weight)
            assertEquals("before key mismatch", FusionPathName.attribute("foo"), actual.afterKey)
        }
    }

    @Test
    fun test_parseAfter_withWeight_success() {
        assertKeyPosition<AfterPosition>("after foo 100") { actual ->
            assertEquals("weight mismatch", 100, actual.weight)
            assertEquals("before key mismatch", FusionPathName.attribute("foo"), actual.afterKey)
        }
    }

    @Test
    fun test_parseAfter_specialChars_success() {
        assertKeyPosition<AfterPosition>("after abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ:-_0123456789") { actual ->
            assertEquals("weight mismatch", 0, actual.weight)
            assertEquals(
                "before key mismatch",
                FusionPathName.attribute("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ:-_0123456789"),
                actual.afterKey
            )
        }
    }

    @Test
    fun test_parseStart_noWeight_success() {
        assertKeyPosition<StartPosition>("start") { actual ->
            assertEquals("weight mismatch", 0, actual.weight)
        }
    }

    @Test
    fun test_parseStart_withWeight_success() {
        assertKeyPosition<StartPosition>("start 100") { actual ->
            assertEquals("weight mismatch", 100, actual.weight)
        }
    }

    @Test
    fun test_parseEnd_noWeight_success() {
        assertKeyPosition<EndPosition>("end") { actual ->
            assertEquals("weight mismatch", 0, actual.weight)
        }
    }

    @Test
    fun test_parseEnd_withWeight_success() {
        assertKeyPosition<EndPosition>("end 100") { actual ->
            assertEquals("weight mismatch", 100, actual.weight)
        }
    }

    @Test(expected = KeyPositionParseException::class)
    fun test_parseError() {
        KeyPosition.parseFromString("some unknown position string")
    }

}

private inline fun <reified T : KeyPosition> assertKeyPosition(
    subjectRaw: String,
    expectedSubject: String = subjectRaw,
    code: (T) -> Unit
) {
    val actual = KeyPosition.parseFromString(subjectRaw)
    assertEquals("Key position type mismatch", T::class.java, actual::class.java)
    assertEquals("Key position subject mismatch", expectedSubject, actual.subject)
    code(actual as T)
}
