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

import io.neos.fusion4j.lang.model.FusionPackageName
import io.neos.fusion4j.lang.model.decl.CodeIndexedElement

/**
 * Part of the [FusionLoadOrder]. The package loading order is defined explicitly by an ordered list of package names.
 */
class PackageLoadOrder(
    private val packageLoadOrder: List<FusionPackageName>
) : Comparator<CodeIndexedElement> {
    override fun compare(o1: CodeIndexedElement?, o2: CodeIndexedElement?): Int {
        val element1 = checkNotNull(o1)
        val element2 = checkNotNull(o2)
        val index1 = packageLoadOrder.indexOf(element1.sourceIdentifier.packageName)
        val index2 = packageLoadOrder.indexOf(element2.sourceIdentifier.packageName)
        val validator: (Int, FusionPackageName) -> Unit = { index, fusionFileName ->
            if (index < 0) {
                throw IllegalArgumentException("Fusion package '$fusionFileName' not listed in package load order: $packageLoadOrder")
            }
        }
        validator(index1, element1.sourceIdentifier.packageName)
        validator(index2, element2.sourceIdentifier.packageName)
        // reverse here -> higher index means higher order
        return index2.compareTo(index1)
    }

    override fun toString(): String = "- ${packageLoadOrder.joinToString("\n - ")}\n"
}