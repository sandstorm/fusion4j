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
import io.neos.fusion4j.lang.file.FusionSourceFileIdentifier
import io.neos.fusion4j.lang.model.decl.CodeIndexedElement
import io.neos.fusion4j.lang.parser.RawFusionModel

/**
 * Part of the [FusionLoadOrder]. When two lines live in the same package but NOT the same file, this comparator
 * calculates the actual load order for those two lines based on file includes.
 */
class FileIncludeOrder(
    private val packageOrderByName: Map<FusionPackageName, IncludeAwareElementOrder>
) : Comparator<CodeIndexedElement> {

    override fun compare(o1: CodeIndexedElement?, o2: CodeIndexedElement?): Int {
        checkNotNull(o1)
        checkNotNull(o2)
        // elements from different packages are not comparable
        assert(o1.sourceIdentifier.packageName == o2.sourceIdentifier.packageName) {
            "package names must be equal, but was: ${o1.sourceIdentifier.packageName} and ${o2.sourceIdentifier.packageName}"
        }

        val packageIncludeOrder = packageOrderByName[o1.sourceIdentifier.packageName]
            ?: throw IllegalStateException("No package include order found for ${o1.sourceIdentifier.packageName}")

        return packageIncludeOrder.compare(o1, o2)
    }

    companion object {
        fun create(
            rawFusionModel: RawFusionModel,
            packageEntrypoints: Map<FusionPackageName, FusionResourceName>
        ): FileIncludeOrder {
            val fileIncludeIndices: Map<FusionPackageName, FileIncludeIndex> =
                packageEntrypoints.mapValues { entrypointEntry ->
                    val packageFiles = rawFusionModel.declarations
                        .filter { it.sourceIdentifier.packageName == entrypointEntry.key }
                        .toSet()
                    FileIncludeIndex.create(
                        packageName = entrypointEntry.key,
                        entrypointFile = entrypointEntry.value,
                        packageFiles = packageFiles
                    )
                }

            val packageOrderByName: Map<FusionPackageName, IncludeAwareElementOrder> =
                rawFusionModel.declarations
                    .groupBy { it.sourceIdentifier.packageName }
                    .mapValues {
                        IncludeAwareElementOrder(
                            fileIncludeIndices[it.key]
                                ?: throw IllegalArgumentException("no entrypoint for package ${it.key}")
                        )
                    }

            return FileIncludeOrder(packageOrderByName)
        }
    }

}