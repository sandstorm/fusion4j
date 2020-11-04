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
import io.neos.fusion4j.lang.model.FusionPathName
import io.neos.fusion4j.lang.model.QualifiedPrototypeName
import io.neos.fusion4j.lang.model.RelativeFusionPathName
import io.neos.fusion4j.lang.model.decl.FusionLangElement
import io.neos.fusion4j.lang.model.decl.PrototypeDecl

/**
 * This class represents a loaded / semantically normalized prototype. Similar to the [FusionObjectInstance], it holds all
 * nested child path declarations. It also holds a reference to its inherited prototype if it extends any.
 *
 * NOTE: multi-inheritance is NOT possible in Fusion, even though, the syntax allows you to write Fusion that actually
 * looks like multi-inheritance.
 *
 * Also, all prototype extensions are kept in here. Those attributes are already grouped by their extension scope.
 */
data class FusionPrototype(
    val prototypeName: QualifiedPrototypeName,
    val rootPrototypeDeclarations: List<PrototypeDecl>,
    val inheritedPrototype: FusionPrototype?,
    val declaredChildPaths: Map<RelativeFusionPathName, FusionValueReference>,
    val declaredExtensionChildPaths: Map<PrototypeExtensionScope, Map<RelativeFusionPathName, FusionValueReference>>
) {

    val declarations: Set<FusionLangElement> =
        (rootPrototypeDeclarations + declaredExtensionChildPaths.values.flatMap { it.values.map(FusionValueReference::decl) })
            .toSet()

    val prototypePath: AbsoluteFusionPathName = buildRootPrototypePath(prototypeName)

    val declaredAttributes: Map<RelativeFusionPathName, FusionValueReference> =
        FusionPaths.getAllDirectChildPaths(declaredChildPaths)

    val attributes: Map<RelativeFusionPathName, FusionValueReference> =
        if (inheritedPrototype == null) {
            declaredAttributes
        } else {
            // inherited prototype recursion here
            inheritedPrototype.attributes + declaredAttributes
        }

    val declaredExtensionAttributes: Map<PrototypeExtensionScope, Map<RelativeFusionPathName, FusionValueReference>> =
        declaredExtensionChildPaths
            .mapValues {
                FusionPaths.getAllDirectChildPaths(it.value)
            }

    fun getAttributes(evaluationPath: EvaluationPath): Map<RelativeFusionPathName, FusionValueReference> {
        // extension attributes win over prototype attributes
        val effectiveAttributes = declaredAttributes + getExtensionAttributesForScope(evaluationPath)
        return if (inheritedPrototype == null) {
            effectiveAttributes
        } else {
            // more concrete prototype attributes win over more abstract ones
            inheritedPrototype.getAttributes(evaluationPath) + effectiveAttributes
        }
    }

    fun getChildPathValue(evaluationPath: EvaluationPath, relativePath: RelativeFusionPathName): FusionValueReference? {
        val attribute = (declaredChildPaths + getExtensionAttributesForScope(evaluationPath))[relativePath]
        return if (inheritedPrototype == null) {
            attribute
        } else {
            attribute ?: inheritedPrototype.getChildPathValue(evaluationPath, relativePath)
        }
    }

    fun getSubAttributes(
        evaluationPath: EvaluationPath,
        relativeBase: RelativeFusionPathName
    ): Map<RelativeFusionPathName, FusionValueReference> {
        // extension attributes win over prototype attributes
        val effectiveAttributes = FusionPaths.getAllDirectChildPaths(declaredChildPaths, relativeBase) +
                getExtensionSubAttributesForScope(evaluationPath, relativeBase)
        return if (inheritedPrototype == null) {
            effectiveAttributes
        } else {
            // more concrete prototype attributes win over more abstract ones
            inheritedPrototype.getSubAttributes(evaluationPath, relativeBase) + effectiveAttributes
        }
    }

    fun getChildPaths(
        evaluationPath: EvaluationPath,
        relativeBase: RelativeFusionPathName = FusionPathName.current()
    ): Map<RelativeFusionPathName, FusionValueReference> {
        // extension attributes win over prototype attributes
        val childPaths = FusionPaths.getAllNestedChildPaths(
            listOf(declaredChildPaths, getExtensionAttributesForScope(evaluationPath)),
            relativeBase
        )
        return if (inheritedPrototype == null) {
            childPaths
        } else {
            // more concrete prototype attributes win over more abstract ones
            inheritedPrototype.getChildPaths(evaluationPath, relativeBase) + childPaths
        }
    }

    private fun getExtensionAttributesForScope(
        evaluationPath: EvaluationPath
    ): Map<RelativeFusionPathName, FusionValueReference> {
        val allMatchingScopesOrdered: List<PrototypeExtensionScope> = declaredExtensionAttributes.keys
            .filter { it.isInScope(evaluationPath) }
            .sorted()
        return allMatchingScopesOrdered.fold(emptyMap()) { result, matchingScope ->
            result + declaredExtensionAttributes[matchingScope]!!
        }
    }

    private fun getExtensionSubAttributesForScope(
        evaluationPath: EvaluationPath,
        relativeBase: RelativeFusionPathName
    ): Map<RelativeFusionPathName, FusionValueReference> {
        val allMatchingScopesOrdered: List<PrototypeExtensionScope> = declaredExtensionChildPaths.keys
            .filter { it.isInScope(evaluationPath) }
            .sorted()
        val allExtensionChildPaths: Map<RelativeFusionPathName, FusionValueReference> = allMatchingScopesOrdered
            .fold(emptyMap()) { result, matchingScope ->
                result + declaredExtensionChildPaths[matchingScope]!!
            }
        return FusionPaths.getAllDirectChildPaths(
            allExtensionChildPaths,
            relativeBase
        )
    }

    companion object {
        fun buildRootPrototypePath(
            prototypeName: QualifiedPrototypeName
        ): AbsoluteFusionPathName =
            FusionPathName.root().appendPrototypeCallSegment(prototypeName)
    }

}