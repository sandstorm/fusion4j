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

package io.neos.fusion4j.styleguide.ui.fusionModel

import io.neos.fusion4j.lang.annotation.FusionApi
import io.neos.fusion4j.lang.model.AbsoluteFusionPathName
import io.neos.fusion4j.lang.model.FusionPathName
import io.neos.fusion4j.lang.model.values.FusionObjectValue
import io.neos.fusion4j.lang.semantic.FusionValueReference
import io.neos.fusion4j.lang.semantic.RawFusionIndex
import io.neos.fusion4j.runtime.model.FusionDataStructure

@FusionApi
data class PathIndexModel(
    @FusionApi
    val entrypointPaths: FusionDataStructure<PathIndexEntryModel>,
    @FusionApi
    val prototypePaths: FusionDataStructure<PathIndexEntryModel>,
    @FusionApi
    val prototypeExtensionPaths: FusionDataStructure<PathIndexEntryModel>
) {

    companion object {
        fun create(rawIndex: RawFusionIndex): PathIndexModel {
            val order = { it: PathIndexEntryModel -> it.path.toString() }
            val allPaths = rawIndex.allPaths
                .map { path ->
                    PathIndexEntryModel(
                        path = path,
                        effectiveValue = FusionValueModel.create(
                            rawIndex.getFusionValueReferenceForPath(
                                path,
                                FusionPathName.root()
                            )
                        ),
                        childPaths = FusionDataStructure.fromList(
                            rawIndex.resolveChildPathFusionValues(path)
                                .map {
                                    FusionValueModel.create(it.value)
                                }
                        ),
                        directChildPaths = FusionDataStructure.fromList(
                            rawIndex.resolveNestedAttributeFusionValues(path)
                                .map {
                                    FusionValueModel.create(it.value)
                                }
                        )
                    )
                }

            val collected = allPaths
                .fold(Collector()) { result, current ->
                    when {
                        current.path.extendingPrototypeName != null -> Collector(
                            result.entrypointPaths,
                            result.prototypePaths,
                            result.prototypeExtensionPaths + current
                        )
                        current.path.isRootPrototypePath -> Collector(
                            result.entrypointPaths,
                            result.prototypePaths + current,
                            result.prototypeExtensionPaths
                        )
                        else -> Collector(
                            result.entrypointPaths + current,
                            result.prototypePaths,
                            result.prototypeExtensionPaths
                        )
                    }
                }

            return PathIndexModel(
                /*
                entrypointPaths = FusionDataStructure.fromList(
                    collected.entrypointPaths
                        .sortedBy(order)
                ),*/
                entrypointPaths = FusionDataStructure.fromList(allPaths),
                prototypePaths = FusionDataStructure.fromList(
                    collected.prototypePaths
                        .sortedBy(order)
                ),
                prototypeExtensionPaths = FusionDataStructure.fromList(
                    collected.prototypeExtensionPaths
                        .sortedBy(order)
                )
            )
        }

    }

    @FusionApi
    data class PathIndexEntryModel(
        internal val path: AbsoluteFusionPathName,
        @FusionApi
        val effectiveValue: FusionValueModel,
        @FusionApi
        val childPaths: FusionDataStructure<FusionValueModel>,
        @FusionApi
        val directChildPaths: FusionDataStructure<FusionValueModel>
    ) {
        @FusionApi
        val pathName: String = path.toString()
    }

    data class FusionValueModel(
        val type: String,
        val astHint: String
    ) {
        companion object {
            fun create(fusionValueReference: FusionValueReference): FusionValueModel =
                FusionValueModel(
                    type = when (val fusionValue = fusionValueReference.fusionValue) {
                        is FusionObjectValue ->
                            "${fusionValue.getReadableType()} ${fusionValue.getReadableValue()}"
                        else -> fusionValue.getReadableType()
                    },
                    astHint = fusionValueReference.decl.hintMessage
                )
        }
    }

    private data class Collector(
        val entrypointPaths: List<PathIndexEntryModel> = emptyList(),
        val prototypePaths: List<PathIndexEntryModel> = emptyList(),
        val prototypeExtensionPaths: List<PathIndexEntryModel> = emptyList()
    )
}