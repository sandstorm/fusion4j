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

import io.neos.fusion4j.lang.model.AbsoluteFusionPathName

// TODO this API could be nicer, add typesafe steps instead of generic outer type
class RawIndexResolveStack<TCurrentItem : RawIndexResolveStackItem> internal constructor(
    val currentItem: TCurrentItem,
    oldStack: List<RawIndexResolveStackItem> = emptyList(),
) {
    private val stack: List<RawIndexResolveStackItem> = listOf(currentItem) + oldStack

    init {
        if (stack.isEmpty()) throw IllegalArgumentException("Stack must not be empty")
    }

    fun nextCopyValue(pathName: AbsoluteFusionPathName): RawIndexResolveStack<RawIndexResolveStackItemValue> =
        nextValue(pathName, "copy")

    fun nextNestedValue(pathName: AbsoluteFusionPathName): RawIndexResolveStack<RawIndexResolveStackItemValue> =
        nextValue(pathName, "nested")

    private fun nextValue(
        pathName: AbsoluteFusionPathName,
        action: String
    ): RawIndexResolveStack<RawIndexResolveStackItemValue> =
        RawIndexResolveStack(RawIndexResolveStackItemValue(pathName, action), stack)

    override fun toString(): String = stack.joinToString(" \n - ", " - ") { it.toReadableString() }

    companion object {
        fun createForValue(valuePath: AbsoluteFusionPathName): RawIndexResolveStack<RawIndexResolveStackItemValue> =
            RawIndexResolveStack(RawIndexResolveStackItemValue(valuePath, "resolve"))

        fun createForNestedValues(path: AbsoluteFusionPathName): RawIndexResolveStack<RawIndexResolveStackItemNestedValues> =
            RawIndexResolveStack(RawIndexResolveStackItemNestedValues(path))

    }

}

interface RawIndexResolveStackItem {
    fun toReadableString(): String
}

data class RawIndexResolveStackItemValue(
    val valuePath: AbsoluteFusionPathName,
    val action: String
) : RawIndexResolveStackItem {
    override fun toReadableString(): String = "value: $valuePath"
}

data class RawIndexResolveStackItemNestedValues(
    val path: AbsoluteFusionPathName
) : RawIndexResolveStackItem {
    override fun toReadableString(): String = "nested values: $path"
}
