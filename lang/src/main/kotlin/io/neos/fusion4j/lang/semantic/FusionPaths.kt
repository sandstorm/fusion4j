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

import io.neos.fusion4j.lang.model.FusionPathName
import io.neos.fusion4j.lang.model.RelativeFusionPathName
import io.neos.fusion4j.lang.model.values.IntegerValue
import io.neos.fusion4j.lang.model.values.StringValue
import mu.KLogger
import mu.KotlinLogging

private val log: KLogger = KotlinLogging.logger {}

/**
 * A set of default Fusion paths and functions that have semantic meaning.
 */
interface FusionPaths {
    companion object {
        /**
         * Defines which (Java-)class is used to render a Fusion object instance.
         */
        val CLASS_META_ATTRIBUTE: RelativeFusionPathName =
            FusionPathName.metaAttribute("class")

        /**
         * Declare additional context variables for the current evaluation in Fusion itself.
         */
        val CONTEXT_META_ATTRIBUTE: RelativeFusionPathName =
            FusionPathName.metaAttribute("context")

        /**
         * Declare conditions that may prevent the output from being rendered.
         */
        val IF_META_ATTRIBUTE: RelativeFusionPathName =
            FusionPathName.metaAttribute("if")

        /**
         * Declare post-processors for the rendering output.
         */
        val PROCESS_META_ATTRIBUTE: RelativeFusionPathName =
            FusionPathName.metaAttribute("process")

        /**
         * Declare multiple attributes from context as path declarations (spreading of context arrays like `props`).
         */
        val APPLY_META_ATTRIBUTE: RelativeFusionPathName =
            FusionPathName.metaAttribute("apply")

        /**
         * Positional key sorting.
         */
        val POSITION_META_ATTRIBUTE: RelativeFusionPathName =
            FusionPathName.metaAttribute("position")

        /**
         * Filters all direct child paths from the given nested paths. Turns non-direct child paths into "virtual" paths
         * as well if there are no explicit declarations.
         *
         * Also makes the reference paths relative to the given base.
         *
         * The order of the paths list determines the overriding semantic. Later items will override
         * paths from earlier items. Untyped paths do not override previous typed paths.
         */
        fun getAllDirectChildPaths(
            paths: List<Map<RelativeFusionPathName, FusionValueReference>>,
            relativeBase: RelativeFusionPathName = FusionPathName.current()
        ): Map<RelativeFusionPathName, FusionValueReference> {
            log.trace { "resolving all direct child paths ..." }
            val pathsFiltered = childPaths(paths, relativeBase)
            return pathsFiltered
                .fold(emptyMap()) { result, valueEntry ->
                    if (valueEntry.first.segments.size == relativeBase.segments.size + 1) {
                        result + if (relativeBase.segments.isNotEmpty()) {
                            valueEntry.first.relativeTo(relativeBase) to valueEntry.second.relativeTo(relativeBase)
                        } else {
                            valueEntry
                        }
                    } else {
                        // virtual path
                        val virtualPath = RelativeFusionPathName.fromSegments(
                            listOf(valueEntry.first.segments[relativeBase.segments.size])
                        )
                        if (result.containsKey(virtualPath)) {
                            result
                        } else {
                            result + (virtualPath to
                                    FusionValueReference.virtual(
                                        valueEntry.second.absolutePath.cutTail(valueEntry.first.segments.size) + relativeBase,
                                        virtualPath,
                                        valueEntry.second.decl
                                    )
                                    )
                        }
                    }
                }
        }

        fun getAllDirectChildPaths(
            paths: Map<RelativeFusionPathName, FusionValueReference>,
            relativeBase: RelativeFusionPathName = FusionPathName.current()
        ): Map<RelativeFusionPathName, FusionValueReference> =
            getAllDirectChildPaths(listOf(paths), relativeBase)

        fun getAllNestedChildPaths(
            paths: List<Map<RelativeFusionPathName, FusionValueReference>>,
            relativeBase: RelativeFusionPathName
        ): Map<RelativeFusionPathName, FusionValueReference> {
            val pathsFiltered = childPaths(paths, relativeBase)
            return pathsFiltered
                .associate {
                    it.first.relativeTo(relativeBase) to it.second.relativeTo(relativeBase)
                }
        }

        fun getAllKeyPositions(
            childPaths: Map<RelativeFusionPathName, FusionValueReference>,
            relativeBase: RelativeFusionPathName,
            keyPositionSubPath: RelativeFusionPathName,
        ): Map<RelativeFusionPathName, KeyPosition> =
            childPaths.filterKeys { it.segments.size == 2 + relativeBase.segments.size && it.endsWith(keyPositionSubPath) }
                .mapValues {
                    when (val positionFusionValue = it.value.fusionValue) {
                        is StringValue -> KeyPosition.parseFromString(positionFusionValue.value)
                        is IntegerValue -> MiddlePosition(
                            positionFusionValue.value.toString(),
                            positionFusionValue.value
                        )
                        else -> throw IllegalArgumentException("Position meta key must be of type String or Integer; but was: $positionFusionValue")
                    }
                }
                .mapKeys { it.key.parent().relativeToParent() }

        private fun childPaths(
            paths: List<Map<RelativeFusionPathName, FusionValueReference>>,
            relativeBase: RelativeFusionPathName
        ): List<Pair<RelativeFusionPathName, FusionValueReference>> {
            val pathsAsList = paths
                .toList()
                .fold(emptyMap<RelativeFusionPathName, FusionValueReference>()) { result, currentPaths ->
                    result + currentPaths
                        // untyped paths do not override existing typed paths
                        .filterNot {
                            val alreadyPresentPath = result[it.key]
                            it.value.untyped && alreadyPresentPath != null && !alreadyPresentPath.untyped
                        }
                }
                .toList()

            val pathsFiltered = if (relativeBase.segments.isNotEmpty()) {
                pathsAsList.filter { it.first.isAnyChildOf(relativeBase) }
            } else {
                pathsAsList
            }
            return pathsFiltered
        }

    }
}