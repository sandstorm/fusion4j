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

import io.neos.fusion4j.lang.file.FusionResourceName
import io.neos.fusion4j.lang.model.FusionPackageName
import io.neos.fusion4j.lang.model.decl.CodeIndexedElement
import io.neos.fusion4j.lang.parser.RawFusionModel

/**
 * Provides the comparator for Fusion code elements.
 *
 * 1. Package load order is defined explicitly by the user. See [PackageLoadOrder]
 * 2. File include load order is defined by the declarations of file includes plus the entrypoint file. See [FileIncludeOrder]
 * 3. Load order inside the same file is determined by LoC index. See [CodeIndexedElementOrder]
 */
class FusionLoadOrder(
    rawFusionModel: RawFusionModel,
    packageLoadOrder: List<FusionPackageName>,
    packageEntrypoints: Map<FusionPackageName, FusionResourceName>
) {
    private val packageOrder: PackageLoadOrder = PackageLoadOrder(packageLoadOrder)
    private val includeOrder: FileIncludeOrder = FileIncludeOrder.create(rawFusionModel, packageEntrypoints)
    private val codeIndexedOrder: CodeIndexedElementOrder = CodeIndexedElementOrder()

    /**
     * Load order:
     *  - first comparing package load order
     *  - same package: file include order
     *  - same file: code index order
     */
    val elementOrder: Comparator<CodeIndexedElement> = packageOrder
        .thenComparing(includeOrder)
        .thenComparing(codeIndexedOrder)

    init {
        if (packageLoadOrder.isEmpty()) {
            throw IllegalArgumentException("load order must not be empty")
        }
    }

    override fun toString(): String = "Package load order: $packageOrder"
}

