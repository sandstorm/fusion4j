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

package io.neos.fusion4j.lang.model

import io.neos.fusion4j.lang.model.decl.*

data class PathNameIndex(
    val mappedByPath: Map<AbsoluteFusionPathName, List<FusionPathDecl>>,
    val allPathVariants: Set<AbsoluteFusionPathName>
) {
    fun mergeWithChildren(vararg children: List<PathNameIndex>): PathNameIndex {
        val flattened = children.toList().flatten()
        val childrenMappedByPath = flattened.map(PathNameIndex::mappedByPath)
        val childrenMappedByPathFlat = if (childrenMappedByPath.isEmpty()) {
            emptyMap()
        } else {
            childrenMappedByPath.reduce { acc, map -> acc + map }
        }
        return PathNameIndex(
            (mappedByPath.asSequence() + childrenMappedByPathFlat.asSequence())
                .groupBy({ it.key }, { it.value })
                .mapValues { it.value.flatten() },
            allPathVariants + flattened.flatMap(PathNameIndex::allPathVariants)
        )
    }

    fun getAllPrototypeNamesFromPathSegments(): Set<QualifiedPrototypeName> =
        allPathVariants
            .flatMap(FusionPathName::segments)
            .filterIsInstance<PrototypeCallPathSegment>()
            .map(PrototypeCallPathSegment::prototypeName)
            .toSet()

    fun getAllPathExtensionsForPrototype(prototypeName: QualifiedPrototypeName): Set<FusionPathDecl> =
        mappedByPath
            .filterKeys { prototypeName == it.extendingPrototypeName }
            .flatMap { it.value }
            .toSet()

    companion object {
        fun buildPathNameIndex(
            assignments: List<FusionPathAssignmentDecl>,
            configurations: List<FusionPathConfigurationDecl>,
            copyDeclarations: List<FusionPathCopyDecl>,
            erasures: List<FusionPathErasureDecl>
        ): PathNameIndex {
            val allPathDeclarations = assignments + configurations + copyDeclarations + erasures
            return PathNameIndex(
                mappedByPath = allPathDeclarations
                    .groupBy(FusionPathDecl::absolutePath),
                allPathVariants = allPathDeclarations
                    .map(FusionPathDecl::absolutePath)
                    .flatMap(AbsoluteFusionPathName::allVariants)
                    .toSet()
            ).mergeWithChildren(
                assignments
                    .map { it.valueDeclaration }
                    .mapNotNull(FusionValueDecl::body)
                    .map(InnerFusionDecl::pathNameIndex),
                configurations
                    .map(FusionPathConfigurationDecl::body)
                    .map(InnerFusionDecl::pathNameIndex),
                copyDeclarations
                    .mapNotNull(FusionPathCopyDecl::body)
                    .map(InnerFusionDecl::pathNameIndex)
            )
        }
    }
}