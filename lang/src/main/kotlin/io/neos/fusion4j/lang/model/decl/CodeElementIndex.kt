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

package io.neos.fusion4j.lang.model.decl

data class CodeElementIndex(
    val mappedByIndex: Map<Int, CodeIndexedElement>,
    val asOrderedList: List<CodeIndexedElement>
) {
    val size get() = asOrderedList.size

    inline operator fun <reified T> get(index: Int): T = getElementAt(asOrderedList, index)

    companion object {
        private fun validateIndexConsistency(vararg subLists: List<CodeIndexedElement>) {
            val flattened = subLists.toList().flatten().map(CodeIndexedElement::codeIndex)
            for (index in flattened.indices) {
                if (!flattened.contains(index)) {
                    throw IllegalStateException("Invalid code indexing: $subLists")
                }
            }
        }

        fun buildCodeElementIndex(vararg subLists: List<CodeIndexedElement>): CodeElementIndex {
            validateIndexConsistency(*subLists)
            val flattened = subLists.toList().flatten()

            return CodeElementIndex(
                flattened.associateBy(CodeIndexedElement::codeIndex),
                flattened.sortedBy(CodeIndexedElement::codeIndex)
            )
        }
    }
}

inline fun <reified T> getElementAt(asOrderedList: List<CodeIndexedElement>, index: Int): T {
    if (index < 0 || asOrderedList.size <= index) {
        throw IndexOutOfBoundsException("no existing code index $index; available from 0 to ${asOrderedList.size - 1}")
    }
    return try {
        asOrderedList[index] as T
    } catch (e: Throwable) {
        throw java.lang.IllegalStateException("no element found at index $index", e)
    }
}

inline fun <reified T> FusionDecl.getElementAt(index: Int): T = getElementAt(this.elementIndex.asOrderedList, index)
