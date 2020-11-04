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

package io.neos.fusion4j.neos.fusion.impl

@Suppress("unused")
class IterationInformation(
    val index: Int,
    val size: Int
) {

    @Suppress("unused")
    val cycle: Int = index + 1

    @Suppress("unused")
    val first: Boolean = index == 0

    @Suppress("unused")
    val last: Boolean = cycle == size

    @Suppress("unused")
    val even: Boolean = cycle % 2 == 0

    @Suppress("unused")
    val odd: Boolean = !even

    // kotlin messes with generated getter names
    // to stay compatible with the Neos PHP Fusion API we write
    // weird non-java-like boolean getter names
    fun getIsFirst(): Boolean = first
    fun getIsLast(): Boolean = last
    fun getIsEven(): Boolean = even
    fun getIsOdd(): Boolean = odd

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IterationInformation

        if (index != other.index) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + size
        return result
    }

    override fun toString(): String = "IterationInformation"

}
