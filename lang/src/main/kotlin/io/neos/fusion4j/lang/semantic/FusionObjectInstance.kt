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
import io.neos.fusion4j.lang.model.RelativeFusionPathName
import io.neos.fusion4j.lang.model.decl.FusionLangElement
import io.neos.fusion4j.lang.model.values.ErasedValue
import io.neos.fusion4j.lang.model.values.PrimitiveFusionValue

/**
 * Represents a semantically initialized Fusion object instance.
 * It holds all declared nested paths and holds a reference to its [FusionPrototype].
 *
 * The effective set of attributes are calculated and stored in [FusionObjectInstance.attributes].
 * Semantically equal / redundant declarations are already filtered by the [RawFusionIndex]. Also, path copies
 * are already resolved. This class is responsible for the following parts of the semantic:
 * <ul>
 *     <li>attributes from the instance body win over attributes from prototypes and evaluation path instance attributes</li>
 *     <li>evaluation path instance attributes win over attributes from prototypes</li>
 *     <li>erased attributes are filtered</li>
 *     <li>attributes from more concrete inherited prototypes win over attributes from more abstract prototypes</li>
 * </ul>
 *
 * The only thing that cannot be calculated from the parsed Fusion itself are "applied attributes" ('at'-apply syntax).
 * Those are only available during runtime, since the current evaluation runtime context is required to do so.
 */
data class FusionObjectInstance(
    val evaluationPath: EvaluationPath,
    val instanceDeclaration: FusionLangElement,
    val prototype: FusionPrototype,
    val instanceChildPaths: Map<RelativeFusionPathName, FusionValueReference>,
    val evaluationPathInstanceChildPaths: Map<RelativeFusionPathName, FusionValueReference>,
    val keyPositionSubPath: RelativeFusionPathName = FusionPaths.POSITION_META_ATTRIBUTE
) {

    init {
        if (evaluationPath.currentType != prototype.prototypeName) {
            throw IllegalArgumentException("Could not create Fusion object instance; type of evaluation path $evaluationPath must match prototype; but was: ${prototype.prototypeName}")
        }
    }

    val instanceDeclarationPath: AbsoluteFusionPathName = evaluationPath.toDeclarationPath()

    val childPaths: Map<RelativeFusionPathName, FusionValueReference> =
        (prototype.getChildPaths(evaluationPath) + evaluationPathInstanceChildPaths + instanceChildPaths)
            .filterValues { it.fusionValue !is ErasedValue }

    fun getChildPathValue(relativePath: RelativeFusionPathName): FusionValueReference? = childPaths[relativePath]

    val attributes: Map<RelativeFusionPathName, FusionValueReference> =
        FusionPaths.getAllDirectChildPaths(
            // order of overriding
            listOf(
                prototype.getChildPaths(evaluationPath),
                // TODO is this correct? do instance child paths win over evaluation path child paths? or do they merge here?
                evaluationPathInstanceChildPaths,
                instanceChildPaths
            )
        )
            .filterValues { it.fusionValue !is ErasedValue }

    val declaredAttributes: Map<RelativeFusionPathName, DeclaredFusionAttribute> =
        attributes.mapValues { DeclaredFusionAttribute(it.value) }

    val declaredPropertyAttributes: Map<RelativeFusionPathName, DeclaredFusionAttribute> =
        declaredAttributes.filterKeys { it.propertyAttribute }

    val attributePositions: Map<RelativeFusionPathName, KeyPosition> =
        FusionPaths.getAllKeyPositions(childPaths, FusionPathName.current(), keyPositionSubPath)

    private fun getDeclaredSubAttributes(
        metaAttributePath: RelativeFusionPathName
    ): Map<RelativeFusionPathName, FusionValueReference> =
        (prototype.getSubAttributes(evaluationPath, metaAttributePath) +
                FusionPaths.getAllDirectChildPaths(
                    childPaths,
                    metaAttributePath
                ))
            .filterValues { it.fusionValue !is ErasedValue }

    val contextAttributes: Map<RelativeFusionPathName, FusionValueReference> =
        getDeclaredSubAttributes(FusionPaths.CONTEXT_META_ATTRIBUTE)

    val conditionalAttributes: Map<RelativeFusionPathName, FusionValueReference> =
        getDeclaredSubAttributes(FusionPaths.IF_META_ATTRIBUTE)

    val processorAttributes: Map<RelativeFusionPathName, FusionValueReference> =
        getDeclaredSubAttributes(FusionPaths.PROCESS_META_ATTRIBUTE)

    val declaredApplyAttributes = getDeclaredSubAttributes(FusionPaths.APPLY_META_ATTRIBUTE)

    val positionalArraySorter: PositionalArraySorter =
        PositionalArraySorter.createSorter(
            declaredPropertyAttributes.keys,
            attributePositions
        )

    val instancePropertyAttributesSorted: List<DeclaredFusionAttribute> =
        declaredPropertyAttributes.values
            .sortedWith { o1, o2 ->
                positionalArraySorter
                    .compare(o1.relativePath, o2.relativePath)
            }

    val applyAttributes: List<Pair<RelativeFusionPathName, FusionValueReference>> = run {
        val applyAttributePositions =
            FusionPaths.getAllKeyPositions(childPaths, FusionPaths.APPLY_META_ATTRIBUTE, keyPositionSubPath)
        val applySorter: PositionalArraySorter = PositionalArraySorter.createSorter(
            declaredApplyAttributes.keys,
            applyAttributePositions
        )
        declaredApplyAttributes
            .toList()
            .sortedWith(
                Comparator.comparing(
                    Pair<RelativeFusionPathName, *>::first,
                    applySorter
                )
            )
    }

    val implementationClass: String
        get() = getStringPrimitiveAttributeValue(FusionPaths.CLASS_META_ATTRIBUTE)
            ?: throw IllegalStateException("Could not get implementation class for $this; no '@class' attribute set for prototype $prototype; instance: $this")

    fun getStringPrimitiveAttributeValue(pathName: RelativeFusionPathName): String? =
        getPrimitiveAttributeValue(pathName)

    inline fun <reified TResult> getPrimitiveAttributeValue(pathName: RelativeFusionPathName): TResult? {
        val loadedValue = getAttribute(pathName)?.fusionValue
        return if (loadedValue != null) {
            if (loadedValue is PrimitiveFusionValue<*>) {
                if (loadedValue.value is TResult) {
                    loadedValue.value as TResult
                } else {
                    throw IllegalArgumentException("Cannot satisfy requested prototype attribute type ${TResult::class.java} from ${loadedValue.getReadableValueAndType()}")
                }
            } else {
                throw IllegalArgumentException("Cannot get fusion value $loadedValue as prototype attribute, value must be a primitive")
            }
        } else {
            null
        }
    }

    fun getAttribute(pathName: RelativeFusionPathName): FusionValueReference? = attributes[pathName]
    fun getRequiredAttribute(pathName: RelativeFusionPathName): FusionValueReference = attributes[pathName]
        ?: throw FusionIndexError("Attribute '$pathName' not found for instance $this")

    fun getSubAttributes(
        evaluationPath: EvaluationPath,
        relativeBase: RelativeFusionPathName
    ): Map<RelativeFusionPathName, FusionValueReference> =
        prototype.getSubAttributes(evaluationPath, relativeBase) +
                FusionPaths.getAllDirectChildPaths(instanceChildPaths, relativeBase)

    fun getAttributePathsSorted(order: Comparator<RelativeFusionPathName>): List<RelativeFusionPathName> =
        attributes.keys.sortedWith(order)

    override fun toString(): String = "$evaluationPath"
}