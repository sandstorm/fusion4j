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
import io.neos.fusion4j.lang.file.FusionSourceFileIdentifier
import io.neos.fusion4j.lang.model.FusionPackageName
import io.neos.fusion4j.lang.model.decl.CodeIndexedElement
import io.neos.fusion4j.lang.model.decl.FusionFileIncludeDecl
import io.neos.fusion4j.lang.model.decl.RootFusionDecl

/**
 * Index to answer the element order (performant) based on file include directives. Used in the [FileIncludeOrder] to
 * compare Fusion language elements.
 */
data class FileIncludeIndex(
    val packageName: FusionPackageName,
    val includeChains: Map<FusionSourceFileIdentifier, FileIncludeChain>,
) {
    companion object {
        fun create(
            packageName: FusionPackageName,
            entrypointFile: FusionResourceName,
            packageFiles: Set<RootFusionDecl>
        ): FileIncludeIndex {

            val includedFilesByFile: Map<RootFusionDecl, IncludedFiles> = packageFiles
                .map { file ->
                    val otherFiles = packageFiles - file
                    IncludedFiles(
                        file,
                        otherFiles
                            .map { other ->
                                val firstMatchingInclude = file.fileIncludes
                                    .sortedWith(CodeIndexedElement.CODE_INDEX_COMPARATOR)
                                    .firstOrNull { it.patternAsRegex.matches(other.sourceIdentifier.resourceName.name) }
                                // importing the entrypoint file is not allowed (loop protection)
                                other to firstMatchingInclude
                            }
                            .mapNotNull { p -> p.second?.let { Pair(p.first, it) } }
                            .toMap()
                            .onEach {
                                if (it.key.sourceIdentifier.resourceName == entrypointFile) {
                                    throw FusionIndexError(
                                        "Recursion loop detected resolving " +
                                                "file includes of ${file.sourceIdentifier}; including the " +
                                                "entrypoint file ${entrypointFile.name} is not allowed"
                                    )
                                }
                            }
                    )
                }
                .associateBy { it.includingFile }


            val includeChains: Map<FusionSourceFileIdentifier, FileIncludeChain> = packageFiles
                .associateBy { it.sourceIdentifier }
                .mapValues { file ->
                    FileIncludeChain.create(entrypointFile, file.value, includedFilesByFile)
                }

            return FileIncludeIndex(packageName, includeChains)
        }
    }
}

data class IncludedFiles(
    val includingFile: RootFusionDecl,
    val includedFiles: Map<RootFusionDecl, FusionFileIncludeDecl>
)

data class FileIncludeChain(
    val includedFile: FusionSourceFileIdentifier,
    val includeChain: List<FileIncludeChainEntry>
) {
    override fun toString(): String = "$includedFile included by" + includeChain.joinToString("\n - ", "\n", "\n")

    companion object {
        fun create(
            entrypointFile: FusionResourceName,
            includedFile: RootFusionDecl,
            allIncludedFilesByFile: Map<RootFusionDecl, IncludedFiles>
        ): FileIncludeChain {
            val chain = flattenToRootInclude(entrypointFile, includedFile, allIncludedFilesByFile)
            return FileIncludeChain(includedFile.sourceIdentifier, chain)
        }

        private fun flattenToRootInclude(
            entrypointFile: FusionResourceName,
            includedFile: RootFusionDecl,
            allIncludedFilesByFile: Map<RootFusionDecl, IncludedFiles>,
            foundSoFar: List<FusionSourceFileIdentifier> = emptyList()
        ): List<FileIncludeChainEntry> {
            return allIncludedFilesByFile
                .filter { it.value.includedFiles.containsKey(includedFile) }
                .map { importEntry ->
                    val includedFilesByIncluding = allIncludedFilesByFile[importEntry.key]
                        ?: throw IllegalStateException("Including file ${includedFile.sourceIdentifier} not found in " +
                                allIncludedFilesByFile.map { it.key.sourceIdentifier })
                    val includeDecl = includedFilesByIncluding.includedFiles[includedFile]
                        ?: throw IllegalStateException("Fusion include decl ${includedFile.sourceIdentifier} not found in " +
                                includedFilesByIncluding.includedFiles.map { it.key.sourceIdentifier })
                    // loop protection
                    if (foundSoFar.any { it == includedFile.sourceIdentifier }) {
                        throw FusionIndexError("Recursion loop detected resolving file includes of package " +
                                "'${includedFile.elementIdentifier.fusionFile.packageName}' and file " +
                                "${includedFile.sourceIdentifier.resourceName}; loop: " +
                                "${foundSoFar.map { it.resourceName } + entrypointFile}, offending: $includeDecl")
                    }
                    if (includedFilesByIncluding.includingFile.sourceIdentifier.resourceName == entrypointFile) {
                        return listOf(
                            FileIncludeChainEntry(
                                includedFilesByIncluding.includingFile.sourceIdentifier,
                                includeDecl
                            )
                        )
                    }
                    return flattenToRootInclude(
                        entrypointFile,
                        includedFilesByIncluding.includingFile,
                        allIncludedFilesByFile,
                        foundSoFar + includedFile.sourceIdentifier
                    ) + listOf(
                        FileIncludeChainEntry(
                            includedFilesByIncluding.includingFile.sourceIdentifier,
                            includeDecl
                        )
                    )
                }
        }
    }

}

data class FileIncludeChainEntry(
    val includingFile: FusionSourceFileIdentifier,
    val effectiveInclude: FusionFileIncludeDecl
) {
    override fun toString(): String =
        "${includingFile.resourceName} (line ${effectiveInclude.astReference.startPosition.line})"
}
