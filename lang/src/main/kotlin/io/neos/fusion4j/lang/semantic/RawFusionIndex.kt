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
import io.neos.fusion4j.lang.model.AbsoluteFusionPathName
import io.neos.fusion4j.lang.model.FusionPackageName
import io.neos.fusion4j.lang.model.FusionPathName
import io.neos.fusion4j.lang.model.RelativeFusionPathName
import io.neos.fusion4j.lang.model.decl.*
import io.neos.fusion4j.lang.model.values.ErasedValue
import io.neos.fusion4j.lang.model.values.FusionValue
import io.neos.fusion4j.lang.model.values.StringValue
import io.neos.fusion4j.lang.parser.RawFusionModel
import mu.KLogger
import mu.KotlinLogging

private val log: KLogger = KotlinLogging.logger {}

/**
 * This class is an essential part of the Fusion semantic.
 * It helps you resolve the actual Fusion value or all descendent paths for a given path.
 */
class RawFusionIndex(
    val pathIndex: PathIndex,
    val loadOrder: FusionLoadOrder,
    private val pathValues: Map<AbsoluteFusionPathName, PathResolveResult>,
    private val childPaths: Map<AbsoluteFusionPathName, Map<RelativeFusionPathName, FusionValueReference>>
) {

    private val directChildPathIndex: Map<AbsoluteFusionPathName, Map<RelativeFusionPathName, FusionValueReference>> =
        childPaths
            .mapValues { entry ->
                FusionPaths.getAllDirectChildPaths(entry.value)
                    .filterValues { it.fusionValue !is ErasedValue }
            }

    val allEffectiveFusionObjectImplementationClassNames: List<String> =
        pathValues
            .filter { it.key.endsWith(FusionPaths.CLASS_META_ATTRIBUTE) }
            .map { it.value.fusionValue }
            .filterIsInstance<StringValue>()
            .map { it.value }

    val allPaths: Set<AbsoluteFusionPathName> = pathValues.keys

    companion object {
        fun build(
            rawFusionModel: RawFusionModel,
            packageLoadOrder: List<FusionPackageName>,
            packageEntrypoints: Map<FusionPackageName, FusionResourceName>
        ): RawFusionIndex {
            val loadOrder = FusionLoadOrder(rawFusionModel, packageLoadOrder, packageEntrypoints)
            val pathIndex = PathIndex.buildPathIndex(rawFusionModel, loadOrder)

            val declaredChildPathIndex = pathIndex.declaredPaths
                .mapNotNull {
                    val childPathValues = resolveChildPathFusionValues(
                        RawIndexResolveStack.createForNestedValues(it),
                        pathIndex,
                        loadOrder
                    )
                    if (childPathValues.isNotEmpty()) {
                        it to childPathValues
                    } else {
                        null
                    }
                }
                .toMap()

            val allChildPathIndex = declaredChildPathIndex.entries
                .fold(declaredChildPathIndex) { all, entry ->
                    entry.value.entries
                        .fold(all) { result, valueEntry ->
                            val effectivePath = entry.key + valueEntry.key
                            val existingValues = all[effectivePath]
                                ?: emptyMap()
                            val nestedValues = entry.value
                                .filter {
                                    it.key.isAnyChildOf(valueEntry.key)
                                }
                                .mapKeys {
                                    it.key.relativeTo(valueEntry.key)
                                }
                            val newValues = nestedValues
                                .filterKeys {
                                    !existingValues.containsKey(it)
                                }
                                .mapValues {
                                    it.value.relativeTo(valueEntry.key)
                                }
                            if (newValues.isNotEmpty()) {
                                result + (effectivePath to newValues)
                            } else {
                                result
                            }
                        }

                }

            val allPathsFlat = pathIndex.declaredPaths + allChildPathIndex
                .flatMap {
                    setOf(it.key) + it.value.keys.map { relative -> it.key + relative }
                }
                .toSet()

            val pathValueIndex = allPathsFlat
                .mapNotNull {
                    resolvePath(
                        RawIndexResolveStack.createForValue(it),
                        it,
                        pathIndex,
                        loadOrder
                    )
                }
                .associateBy { it.requestedPath }

            return RawFusionIndex(
                pathIndex,
                loadOrder,
                pathValueIndex,
                allChildPathIndex
            )
        }

        private fun resolvePath(
            stack: RawIndexResolveStack<RawIndexResolveStackItemValue>,
            requestedPath: AbsoluteFusionPathName? = null,
            pathIndex: PathIndex,
            loadOrder: FusionLoadOrder
        ): PathResolveResult? {
            val path = stack.currentItem.valuePath
            val originallyRequestedPath = requestedPath ?: path

            if (log.isDebugEnabled) {
                log.debug("Resolving path '$path'\n$stack")
            }

            val pathIndexEntry = pathIndex.getOrNull(path)
            val declaredAssignment: FusionPathAssignmentDecl? = pathIndexEntry?.getEffectiveAssignment(loadOrder)
            val declaredErasure: FusionPathErasureDecl? = pathIndexEntry?.getEffectiveErasure(loadOrder)
            val declaredCopy: FusionPathCopyDecl? = pathIndexEntry?.getEffectiveCopy(loadOrder)
            val declaredConfigurations: List<FusionPathConfigurationDecl> =
                pathIndexEntry?.configurations ?: emptyList()
            val parentErasures: List<FusionPathErasureDecl> = pathIndex.getParentPathErasures(path)
            val parentCopies: List<FusionPathDecl> = pathIndex.getParentPathCopies(path)
                .mapNotNull {
                    resolvePathCopyDeclaration(
                        it,
                        stack,
                        originallyRequestedPath,
                        pathIndex,
                        loadOrder
                    )?.decl
                }

            val effectivePathDeclarations: List<FusionPathDecl> =
                (parentErasures + parentCopies + declaredConfigurations + listOfNotNull(
                    declaredAssignment,
                    declaredErasure,
                    declaredCopy
                ))
                    .sortedWith(loadOrder.elementOrder)
            if (effectivePathDeclarations.isEmpty()) {
                return null
            } else {

                val firstErasure = effectivePathDeclarations.firstOrNull { it is FusionPathErasureDecl }

                // erasure is excluded
                val allUntilFirstErasure =
                    if (firstErasure != null)
                        effectivePathDeclarations.subList(0, effectivePathDeclarations.indexOf(firstErasure))
                    else
                        effectivePathDeclarations

                // erased value (undefined)
                if (allUntilFirstErasure.isEmpty() && firstErasure != null) {
                    return PathResolveResult.undefinedErased(
                        originallyRequestedPath,
                        path,
                        firstErasure as FusionPathErasureDecl
                    )
                }

                val effectivePathDeclaration = allUntilFirstErasure
                    .firstOrNull { it !is FusionPathConfigurationDecl }
                val firstConfiguration = allUntilFirstErasure.firstOrNull { it is FusionPathConfigurationDecl }
                // only path configurations (untyped value)
                if (effectivePathDeclaration == null && firstConfiguration != null) {
                    return PathResolveResult.untyped(
                        originallyRequestedPath,
                        path,
                        firstConfiguration as FusionPathConfigurationDecl
                    )
                }
                // path assignment
                if (effectivePathDeclaration is FusionPathAssignmentDecl) {
                    return PathResolveResult.assignment(originallyRequestedPath, path, effectivePathDeclaration)
                }

                // path copy
                if (effectivePathDeclaration is FusionPathCopyDecl) {
                    return if (firstConfiguration != null) {
                        // TODO remember & comment: what does this line do ??? ;)
                        PathResolveResult.untyped(
                            originallyRequestedPath,
                            path,
                            firstConfiguration as FusionPathConfigurationDecl
                        )
                    } else {
                        resolvePathCopyDeclaration(
                            effectivePathDeclaration,
                            stack,
                            originallyRequestedPath,
                            pathIndex,
                            loadOrder
                        )
                    }
                }

                throw FusionIndexError("Could not determine fusion value for '$path'; unknown declaration type: $effectivePathDeclaration")
            }
        }

        private fun resolvePathCopyDeclaration(
            copyDecl: FusionPathCopyDecl,
            stack: RawIndexResolveStack<RawIndexResolveStackItemValue>,
            requestedPath: AbsoluteFusionPathName,
            pathIndex: PathIndex,
            loadOrder: FusionLoadOrder
        ): PathResolveResult? {
            val currentValuePath = stack.currentItem.valuePath
            val pathToResolve = getAbsoluteCopyTargetPath(copyDecl, currentValuePath)
            return resolvePath(stack.nextCopyValue(pathToResolve), requestedPath, pathIndex, loadOrder)
        }


        private fun resolveAbsoluteCopyTargetPath(
            copyDecl: FusionPathCopyDecl,
            currentValuePath: AbsoluteFusionPathName
        ) = when (val copyTargetPath = copyDecl.pathToCopy.pathName) {
            is AbsoluteFusionPathName -> copyTargetPath
            is RelativeFusionPathName -> currentValuePath + copyTargetPath
            else -> throw FusionIndexError("Unknown path type $copyTargetPath")
        }

        private fun getAbsoluteCopyTargetPath(
            copyDecl: FusionPathCopyDecl,
            currentValuePath: AbsoluteFusionPathName
        ): AbsoluteFusionPathName {
            val absoluteCopyTargetPath = resolveAbsoluteCopyTargetPath(copyDecl, currentValuePath)
            val relativeResolvePath = currentValuePath.relativeTo(copyDecl.absolutePath)
            // append relative path to reference before resolving
            return absoluteCopyTargetPath + relativeResolvePath
        }

        private fun resolveChildPathFusionValues(
            stack: RawIndexResolveStack<RawIndexResolveStackItemNestedValues>,
            pathIndex: PathIndex,
            loadOrder: FusionLoadOrder
        ): Map<RelativeFusionPathName, FusionValueReference> {
            val path = stack.currentItem.path
            val allNonPathCopyPaths = pathIndex.getAllChildPropertyPaths(path)
                .map { BasePathAndActualPath(path, it) }
            return (allNonPathCopyPaths + getAllNestedValueCopyParentPaths(path, pathIndex))
                .map {
                    // the load order decides which effective value is taken
                    // if there are multiple declarations of the same path
                    val resolvePath = resolvePath(
                        stack.nextNestedValue(it.effectivePath),
                        null,
                        pathIndex,
                        loadOrder
                    )
                        ?: throw IllegalStateException("Could not resolve child path Fusion value ${it.effectivePath}; no value found")

                    resolvePath.toValueReference(it.basePath)
                }
                .associateBy { it.relativePath }
        }

        private fun getAllNestedValueCopyParentPaths(
            path: AbsoluteFusionPathName,
            pathIndex: PathIndex
        ): Set<BasePathAndActualPath> {
            val indexEntry = pathIndex.getOrNull(path)
            val pathCopies = (indexEntry?.copies ?: emptyList()) + pathIndex.getParentPathCopies(path)
            return pathCopies
                .map {
                    val copyPath = resolveAbsoluteCopyTargetPath(it, path)
                    val pathOffset = path.relativeTo(it.absolutePath)
                    copyPath to pathOffset
                }
                .flatMap { pathAndOffset ->
                    pathIndex.getAllChildPropertyPaths(pathAndOffset.first + pathAndOffset.second)
                        .map { BasePathAndActualPath(pathAndOffset.first + pathAndOffset.second, it) }
                }
                .toSet()
        }

    }

    /**
     * Resolves the effective [FusionValue] for the given [AbsoluteFusionPathName].
     *
     * @param path the input path
     */
    fun getFusionValueForPath(path: AbsoluteFusionPathName): PathResolveResult =
        resolveRequiredPath(path)

    /**
     * Resolves the effective [FusionValue] held inside a [FusionValueReference] to keep track of the
     * declaration paths (absolute and relative).
     *
     * @see FusionValueReference
     */
    fun getFusionValueReferenceForPath(
        path: AbsoluteFusionPathName,
        basePath: AbsoluteFusionPathName
    ): FusionValueReference =
        resolveRequiredPath(path)
            .toValueReference(basePath)

    private fun resolveRequiredPath(
        path: AbsoluteFusionPathName
    ): PathResolveResult =
        pathValues[path]
            ?: throw FusionIndexError("Could not determine fusion value for undeclared path '${path}'")

    /**
     * Resolves all descendant paths for the given [AbsoluteFusionPathName] as [FusionValueReference]s.
     * The results are mapped by their relative Fusion paths.
     *
     * Erased values are included.
     */
    fun resolveChildPathFusionValues(path: AbsoluteFusionPathName): Map<RelativeFusionPathName, FusionValueReference> =
        childPaths[path] ?: emptyMap()

    /**
     * Resolves positions for all direct child paths of the given base path.
     *
     * @see FusionPaths.getAllKeyPositions
     */
    fun resolveChildPathKeyPositions(
        path: AbsoluteFusionPathName,
        keyPositionSubPath: RelativeFusionPathName = FusionPaths.POSITION_META_ATTRIBUTE
    ): Map<RelativeFusionPathName, KeyPosition> =
        FusionPaths.getAllKeyPositions(resolveChildPathFusionValues(path), FusionPathName.current(), keyPositionSubPath)

    /**
     * Resolves all direct child paths / nested attributes for the given [AbsoluteFusionPathName] as [FusionValueReference]s.
     *
     * Erased values are excluded, virtual paths are included.
     */
    fun resolveNestedAttributeFusionValues(path: AbsoluteFusionPathName): Map<RelativeFusionPathName, FusionValueReference> =
        directChildPathIndex[path] ?: emptyMap()

}

/**
 * This is used to keep track of base paths for path copy operations
 * and later relativize the FusionValueReference relative path.
 */
internal data class BasePathAndActualPath(
    val basePath: AbsoluteFusionPathName,
    val effectivePath: AbsoluteFusionPathName
)