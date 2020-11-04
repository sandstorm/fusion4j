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
import io.neos.fusion4j.lang.model.decl.FusionPathCopyDecl
import io.neos.fusion4j.lang.model.decl.FusionPathDecl
import io.neos.fusion4j.lang.model.decl.FusionPathErasureDecl
import io.neos.fusion4j.lang.model.decl.RootFusionDecl
import io.neos.fusion4j.lang.model.decl.values.ExpressionValueDecl
import io.neos.fusion4j.lang.model.values.ExpressionValue
import io.neos.fusion4j.lang.parser.RawFusionModel

/**
 * Part of the [RawFusionIndex] and the [SemanticallyNormalizedFusionModel].
 * The path index holds an index of all [FusionPathDecl] by [AbsoluteFusionPathName].
 * It is combined over all Fusion packages and files.
 */
data class PathIndex(
    private val index: Map<AbsoluteFusionPathName, FusionPathIndexEntry>,
    private val loadOrder: FusionLoadOrder
) {

    companion object {
        fun buildPathIndex(
            model: RawFusionModel,
            loadOrder: FusionLoadOrder
        ): PathIndex = PathIndex(model.declarations
            // take all paths from path index of root fusion
            .map(RootFusionDecl::pathNameIndex)
            // already mapped by paths
            .flatMap { pathNameIndex ->
                pathNameIndex.mappedByPath.asSequence()
            }
            // collect all declarations over all files
            .groupBy({ it.key }, { it.value })
            // create index entry
            .mapValues {
                FusionPathIndexEntry(
                    it.key,
                    buildRawPathIndexForPath(it.value, loadOrder),
                    buildRawPathIndexForPath(it.value, loadOrder),
                    buildRawPathIndexForPath(it.value, loadOrder),
                    buildRawPathIndexForPath(it.value, loadOrder)
                )
            }
            .mapKeys { it.key },
            loadOrder
        )

        private inline fun <reified R : FusionPathDecl> buildRawPathIndexForPath(
            allDeclarationsForPath: List<List<FusionPathDecl>>,
            loadOrder: FusionLoadOrder
        ): List<R> = allDeclarationsForPath
            .flatten()
            .filterIsInstance<R>()
            .sortedWith(loadOrder.elementOrder)

    }

    val declaredPaths: Set<AbsoluteFusionPathName> =
        index.keys.fold(emptySet()) { result, explicitPath ->
            result +
                    // virtual paths
                    explicitPath.allParentPaths()
                        .filterNot { result.contains(it) } +
                    // explicit path
                    explicitPath
        }

    val size: Int = declaredPaths.size

    fun get(pathName: AbsoluteFusionPathName): FusionPathIndexEntry =
        getOrNull(pathName) ?: throw FusionIndexError("Path name $pathName not found in raw Fusion index")

    fun getOrNull(pathName: AbsoluteFusionPathName): FusionPathIndexEntry? = index[pathName]

    /**
     * Includes all descendent paths, not only direct child paths.
     */
    fun getAllChildPropertyPaths(pathName: AbsoluteFusionPathName): Set<AbsoluteFusionPathName> =
        index.values
            .filter { it.path.propertyPath && it.path.isAnyChildOf(pathName) }
            .map { it.path }
            .toSet()

    fun getParentPathErasures(pathName: AbsoluteFusionPathName): List<FusionPathErasureDecl> =
        pathName.allParentPaths()
            .mapNotNull { index[it]?.erasures }
            .mapNotNull {
                it.sortedWith(loadOrder.elementOrder).firstOrNull()
            }
            .sortedWith(loadOrder.elementOrder)

    fun getParentPathCopies(pathName: AbsoluteFusionPathName): List<FusionPathCopyDecl> =
        pathName.allParentPaths()
            .mapNotNull { index[it]?.copies }
            .flatten()
            .sortedWith(loadOrder.elementOrder)

    /**
     * This can be used for pre-parsing expressions during runtime
     * initialization. Note, that this method returns ALL declared
     * expression values, including unused ones! So parsing/caching
     * is best done lazy to avoid parsing too many (unused) expressions.
     */
    fun getAllExpressionValues(): Set<ExpressionValue> =
        index.values
            .flatMap { it.assignments }
            .map { it.valueDeclaration }
            .filterIsInstance<ExpressionValueDecl>()
            .map { it.fusionValue }
            .toSet()

    fun isDeclared(pathName: AbsoluteFusionPathName): Boolean = declaredPaths.contains(pathName)

}