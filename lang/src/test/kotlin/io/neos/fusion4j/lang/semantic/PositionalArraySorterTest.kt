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

import io.neos.fusion4j.lang.model.FusionPathName.Companion.attribute
import org.junit.Assert
import org.junit.Test

internal class PositionalArraySorterTest {

    @Test
    fun test_beforeWeight() {
        assertArrayOrder(
            setOf("1", "2", "3", "4", "10", "20", "a", "b", "c", "d"),
            mapOf(
                "a" to "before 3",
                "b" to "before 3 100",
                "c" to "before 10 99999",
                "d" to "before c",
            ),
            listOf("1", "2", "b", "a", "3", "4", "d", "c", "10", "20")
        )
    }

    @Test
    fun test_afterWeight() {
        assertArrayOrder(
            setOf("1", "2", "3", "4", "10", "20", "a", "b", "c", "d"),
            mapOf(
                "a" to "after 3",
                "b" to "after 3 100",
                "c" to "after 10 99999",
                "d" to "after c",
            ),
            listOf("1", "2", "3", "a", "b", "4", "10", "c", "d", "20")
        )
    }

    @Test
    fun test_fallbackToKeyNameAlphanumericSorting() {
        assertArrayOrder(
            sortedSetOf("c", "b", "a"),
            emptyMap(),
            listOf("a", "b", "c")
        )
    }

}

private fun assertArrayOrder(
    allKeys: Set<String>,
    positions: Map<String, String>,
    expectedOrder: List<String>
) {
    val allKeysAsAttribute = allKeys.map(::attribute).toSet()
    val sorter = PositionalArraySorter.createSorter(
        allKeysAsAttribute,
        positions
            .mapValues { KeyPosition.parseFromString(it.value) }
            .mapKeys { attribute(it.key) }
    )
    val actualKeys = allKeysAsAttribute.sortedWith(sorter)
    Assert.assertEquals("sorted keys mismatch", expectedOrder, actualKeys.map { it.propertyName })
}