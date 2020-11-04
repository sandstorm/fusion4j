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

import io.neos.fusion4j.lang.model.decl.CodeIndexedElement
import kotlin.math.max

/**
 * Part of the [FusionLoadOrder].
 */
class IncludeAwareElementOrder(
    private val fileIncludeIndex: FileIncludeIndex
) : Comparator<CodeIndexedElement> {
    override fun compare(o1: CodeIndexedElement?, o2: CodeIndexedElement?): Int {
        checkNotNull(o1)
        checkNotNull(o2)

        val file1 = o1.sourceIdentifier
        val file2 = o2.sourceIdentifier
        if (file1 == file2) {
            return 0
        }

        val chains1 = fileIncludeIndex.includeChains[file1]
            ?: throw IllegalArgumentException("Include chain for Fusion file $file1 not found")
        val chains2 = fileIncludeIndex.includeChains[file2]
            ?: throw IllegalArgumentException("Include chain for Fusion file $file2 not found")

        if (chains1.includeChain.isEmpty() && chains2.includeChain.isEmpty()) {
            // this indicates a programming BUG since elements in DIFFERENT FILES must at least have one chain
            // ONLY package entrypoints may have empty include chains
            throw IllegalArgumentException("no chains found for nether $file1 or $file2")
        }

        // trivial case: one of the elements is in the entrypoint file
        if (chains1.includeChain.isEmpty()) {
            return CodeIndexedElement.CODE_INDEX_COMPARATOR.compare(o1, chains2.includeChain.first().effectiveInclude)
        }
        if (chains2.includeChain.isEmpty()) {
            return CodeIndexedElement.CODE_INDEX_COMPARATOR.compare(chains1.includeChain.first().effectiveInclude, o2)
        }

        val includePositions = (0 until max(chains1.includeChain.size, chains2.includeChain.size))
            .mapNotNull {
                val entry1 = if (chains1.includeChain.size > it) chains1.includeChain[it].effectiveInclude else o1
                val entry2 = if (chains2.includeChain.size > it) chains2.includeChain[it].effectiveInclude else o2
                if (entry1.sourceIdentifier != entry2.sourceIdentifier) {
                    null
                } else {
                    CodeIndexedElement.CODE_INDEX_COMPARATOR.compare(entry1, entry2)
                }
            }
        if (includePositions.isEmpty()) {
            throw IllegalStateException("Could not determine include positions of ${o1.elementIdentifier} and ${o2.elementIdentifier}")
        }

        val includePosition = includePositions.firstOrNull { it != 0 } ?: 0
        return if (includePosition == 0) {
            // TODO is the sorting by name correct?
            DEFAULT_FILE_NAME_ORDER.compare(o1, o2)
        } else {
            includePosition
        }
    }

    companion object {
        private val DEFAULT_FILE_NAME_ORDER: Comparator<CodeIndexedElement> = Comparator<CodeIndexedElement> { o1, o2 ->
            checkNotNull(o1)
            checkNotNull(o2)
            // reversed here
            compareValues(o2.sourceIdentifier.resourceName.name, o1.sourceIdentifier.resourceName.name)
        }
    }

}