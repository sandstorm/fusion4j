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

@file:Suppress("unused")

package io.neos.fusion4j.runtime

import io.neos.fusion4j.lang.model.AbsoluteFusionPathName
import io.neos.fusion4j.lang.model.QualifiedPrototypeName
import io.neos.fusion4j.lang.model.RelativeFusionPathName
import io.neos.fusion4j.lang.model.values.FusionObjectValue
import io.neos.fusion4j.lang.semantic.FusionPrototype
import io.neos.fusion4j.lang.semantic.PrototypeStore
import io.neos.fusion4j.lang.semantic.RawFusionIndex
import io.neos.fusion4j.runtime.chain.ContextInitializationRuntimeAccess
import io.neos.fusion4j.runtime.chain.EvaluationChainRuntimeAccess
import io.neos.fusion4j.runtime.eel.EelThisPointerRuntimeAccess
import io.neos.fusion4j.runtime.model.DeclaredFusionAttribute
import io.neos.fusion4j.runtime.model.EvaluationResult
import io.neos.fusion4j.runtime.model.FusionAttribute
import io.neos.fusion4j.runtime.model.RuntimeFusionObjectInstance

/**
 * This is what a Fusion Object sees from the Fusion Runtime. It wraps the runtime and makes sure the internal runtime does not leak
 * to the Fusion objects.
 *
 * The [DefaultImplementationRuntimeAccess] implementation (which is the default one) contains the Fusion call stack at the current invocation
 * point and the requested path + type (see [FusionEvaluationRequest]).
 *
 * The following classes are related (i.e. serve the same purpose for different evaluation targets):
 * - [FusionRuntimeImplementationAccess] This is what a Fusion Object sees from the Fusion Runtime. <== THIS CLASS
 * - [EelThisPointerRuntimeAccess] This is what an Eel expression sees from the fusion runtime - to access a "this." reference.
 * - [EvaluationChainRuntimeAccess] For @process and @if evaluation
 * - [ContextInitializationRuntimeAccess] is used for @context evaluation
 * These classes usually call [DefaultFusionRuntime.internalEvaluateFusionValue] for the actual evaluation.
 */
interface FusionRuntimeImplementationAccess {

    val runtimeInstance: RuntimeFusionObjectInstance
    val rawFusionIndex: RawFusionIndex
    val prototypeStore: PrototypeStore
    val callstack: FusionRuntimeStack

    val attributes: Map<RelativeFusionPathName, FusionAttribute>
        get() = runtimeInstance.attributes
    val propertyAttributes: Map<RelativeFusionPathName, FusionAttribute>
        get() = runtimeInstance.propertyAttributes

    val instancePrototype: FusionPrototype
        get() = runtimeInstance.fusionObjectInstance.prototype

    fun getRequiredPropertyAttribute(key: RelativeFusionPathName): FusionAttribute =
        propertyAttributes[key]
            ?: throw createRuntimeError("Required attribute '$key' not declared for runtime instance $this")

    fun createDeprecationError(
        deprecatedPrototype: QualifiedPrototypeName,
        alternative: String
    ): FusionRuntimeException =
        createRuntimeError("The prototype $deprecatedPrototype is deprecated; use $alternative instead")

    fun createNullAttributeError(
        attributeKey: RelativeFusionPathName
    ): FusionRuntimeException =
        createRuntimeError("The value of attribute '${attributeKey.pathAsString}' must not be null")

    fun createRuntimeError(
        message: String,
        cause: Throwable? = null
    ): FusionRuntimeException =
        FusionRuntimeException(callstack, message, runtimeInstance.fusionObjectInstance.instanceDeclaration, cause)

    fun createFusionObjectRuntimeAttribute(
        prototypeName: QualifiedPrototypeName,
        typeAttribute: FusionAttribute,
        attributePath: RelativeFusionPathName
    ): FusionAttribute =
        DeclaredFusionAttribute.runtimeAttribute(
            FusionObjectValue(prototypeName, typeAttribute.valueDecl.astReference),
            runtimeInstance.fusionObjectInstance.instanceDeclarationPath,
            attributePath,
            runtimeInstance.fusionObjectInstance.instanceDeclaration
        )

    val propertyAttributesSorted: List<FusionAttribute>
        get() =
            propertyAttributes.values
                .sortedWith { o1, o2 ->
                    runtimeInstance.positionalArraySorter
                        .compare(o1.relativePath, o2.relativePath)
                }

    fun <TResult> evaluateAbsolutePath(
        path: AbsoluteFusionPathName,
        outputType: Class<TResult>,
        contextLayer: FusionContextLayer,
        overridePrototype: QualifiedPrototypeName?
    ): EvaluationResult<TResult?>

    fun <TResult> evaluateRelativePath(
        path: RelativeFusionPathName,
        outputType: Class<TResult>,
        contextLayer: FusionContextLayer,
        overridePrototype: QualifiedPrototypeName?
    ): EvaluationResult<TResult?>

    fun <TResult> evaluateAttribute(
        attribute: FusionAttribute,
        outputType: Class<TResult>,
        contextLayer: FusionContextLayer,
        overridePrototype: QualifiedPrototypeName?
    ): EvaluationResult<TResult?>

    fun <TResult> evaluateAttribute(
        attribute: FusionAttribute,
        outputType: Class<TResult>,
        contextLayer: FusionContextLayer
    ): EvaluationResult<TResult?> =
        evaluateAttribute(attribute, outputType, contextLayer, null)

    fun <TResult> evaluateAttribute(
        attributePath: RelativeFusionPathName,
        outputType: Class<TResult>,
        contextLayer: FusionContextLayer,
        overridePrototype: QualifiedPrototypeName?
    ): EvaluationResult<TResult?>? {
        val attribute = attributes[attributePath]
        return if (attribute == null) {
            null
        } else {
            evaluateAttribute(
                attribute,
                outputType,
                contextLayer,
                overridePrototype
            )
        }
    }

    fun <TResult> evaluateRequiredAttribute(
        attributePath: RelativeFusionPathName,
        outputType: Class<TResult>,
        contextLayer: FusionContextLayer,
        overridePrototype: QualifiedPrototypeName?
    ): EvaluationResult<TResult?> {
        val attribute = attributes[attributePath]
        return if (attribute == null) {
            throw createNullAttributeError(attributePath)
        } else {
            evaluateAttribute(
                attribute,
                outputType,
                contextLayer,
                overridePrototype
            )
        }
    }

    fun <TResult> evaluateAttributeValue(
        attributePath: RelativeFusionPathName,
        outputType: Class<TResult>,
        contextLayer: FusionContextLayer,
        overridePrototype: QualifiedPrototypeName?
    ): TResult? {
        val evaluationResult = evaluateAttribute(attributePath, outputType, contextLayer, overridePrototype)
            ?: return null
        if (evaluationResult.cancelled) {
            return null
        }
        return evaluationResult.lazyValue.value
    }

    fun <TResult> evaluateRequiredAttributeOptionalValue(
        attributePath: RelativeFusionPathName,
        outputType: Class<TResult>,
        contextLayer: FusionContextLayer,
        overridePrototype: QualifiedPrototypeName?
    ): TResult? {
        val evaluationResult = evaluateAttribute(attributePath, outputType, contextLayer, overridePrototype)
            ?: throw createNullAttributeError(attributePath)
        if (evaluationResult.cancelled) {
            return null
        }
        return evaluationResult.lazyValue.value
    }

    fun <TResult> evaluateRequiredAttributeValue(
        attributePath: RelativeFusionPathName,
        outputType: Class<TResult>,
        contextLayer: FusionContextLayer,
        overridePrototype: QualifiedPrototypeName?
    ): TResult {
        val evaluationResult = evaluateAttribute(attributePath, outputType, contextLayer, overridePrototype)
            ?: throw createRuntimeError("Required attribute '$attributePath' not declared in: ${runtimeInstance.attributes.map { it.key }}")
        if (evaluationResult.cancelled) {
            throw createRuntimeError("Required attribute '$attributePath' was cancelled")
        }
        return evaluationResult.lazyValue.value
            ?: throw createNullAttributeError(attributePath)
    }

}

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateAbsolutePath(
    path: AbsoluteFusionPathName,
    nextContext: FusionContextLayer,
    overridePrototype: QualifiedPrototypeName?
): EvaluationResult<TResult?> =
    evaluateAbsolutePath(path, TResult::class.java, nextContext, overridePrototype)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateAbsolutePath(
    path: AbsoluteFusionPathName,
    overridePrototype: QualifiedPrototypeName?
): EvaluationResult<TResult?> =
    evaluateAbsolutePath(path, TResult::class.java, FusionContextLayer.empty(), overridePrototype)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateAbsolutePath(
    path: AbsoluteFusionPathName,
    nextContext: FusionContextLayer
): EvaluationResult<TResult?> =
    evaluateAbsolutePath(path, TResult::class.java, nextContext, null)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateAbsolutePath(
    path: AbsoluteFusionPathName
): EvaluationResult<TResult?> =
    evaluateAbsolutePath(path, TResult::class.java, FusionContextLayer.empty(), null)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateRelativePath(
    path: RelativeFusionPathName,
    nextContext: FusionContextLayer,
    overridePrototype: QualifiedPrototypeName?
): EvaluationResult<TResult?> =
    evaluateRelativePath(path, TResult::class.java, nextContext, overridePrototype)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateRelativePath(
    path: RelativeFusionPathName,
    overridePrototype: QualifiedPrototypeName?
): EvaluationResult<TResult?> =
    evaluateRelativePath(path, TResult::class.java, FusionContextLayer.empty(), overridePrototype)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateRelativePath(
    path: RelativeFusionPathName,
    nextContext: FusionContextLayer
): EvaluationResult<TResult?> =
    evaluateRelativePath(path, TResult::class.java, nextContext, null)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateRelativePath(
    path: RelativeFusionPathName
): EvaluationResult<TResult?> =
    evaluateRelativePath(path, TResult::class.java, FusionContextLayer.empty(), null)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateAttribute(
    attribute: FusionAttribute,
    nextContext: FusionContextLayer,
    overridePrototype: QualifiedPrototypeName?
): EvaluationResult<TResult?> =
    evaluateAttribute(attribute, TResult::class.java, nextContext, overridePrototype)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateAttribute(
    attribute: FusionAttribute,
    overridePrototype: QualifiedPrototypeName?
): EvaluationResult<TResult?> =
    evaluateAttribute(attribute, TResult::class.java, FusionContextLayer.empty(), overridePrototype)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateAttribute(
    attribute: FusionAttribute,
    nextContext: FusionContextLayer
): EvaluationResult<TResult?> =
    evaluateAttribute(attribute, TResult::class.java, nextContext, null)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateAttribute(
    attribute: FusionAttribute
): EvaluationResult<TResult?> =
    evaluateAttribute(attribute, TResult::class.java, FusionContextLayer.empty(), null)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateAttribute(
    attributePath: RelativeFusionPathName,
    nextContext: FusionContextLayer,
    overridePrototype: QualifiedPrototypeName?
): EvaluationResult<TResult?>? =
    evaluateAttribute(attributePath, TResult::class.java, nextContext, overridePrototype)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateAttribute(
    attributePath: RelativeFusionPathName,
    overridePrototype: QualifiedPrototypeName?
): EvaluationResult<TResult?>? =
    evaluateAttribute(attributePath, TResult::class.java, FusionContextLayer.empty(), overridePrototype)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateAttribute(
    attributePath: RelativeFusionPathName,
    nextContext: FusionContextLayer
): EvaluationResult<TResult?>? =
    evaluateAttribute(attributePath, TResult::class.java, nextContext, null)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateAttribute(
    attributePath: RelativeFusionPathName,
): EvaluationResult<TResult?>? =
    evaluateAttribute(attributePath, TResult::class.java, FusionContextLayer.empty(), null)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateRequiredAttribute(
    attributePath: RelativeFusionPathName,
    nextContext: FusionContextLayer,
    overridePrototype: QualifiedPrototypeName?
): EvaluationResult<TResult?> =
    evaluateRequiredAttribute(attributePath, TResult::class.java, nextContext, overridePrototype)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateRequiredAttribute(
    attributePath: RelativeFusionPathName,
    overridePrototype: QualifiedPrototypeName?
): EvaluationResult<TResult?> =
    evaluateRequiredAttribute(attributePath, TResult::class.java, FusionContextLayer.empty(), overridePrototype)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateRequiredAttribute(
    attributePath: RelativeFusionPathName,
    nextContext: FusionContextLayer
): EvaluationResult<TResult?> =
    evaluateRequiredAttribute(attributePath, TResult::class.java, nextContext, null)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateRequiredAttribute(
    attributePath: RelativeFusionPathName,
): EvaluationResult<TResult?> =
    evaluateRequiredAttribute(attributePath, TResult::class.java, FusionContextLayer.empty(), null)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateAttributeValue(
    attributePath: RelativeFusionPathName,
    nextContext: FusionContextLayer,
    overridePrototype: QualifiedPrototypeName?
): TResult? =
    evaluateAttributeValue(attributePath, TResult::class.java, nextContext, overridePrototype)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateAttributeValue(
    attributePath: RelativeFusionPathName,
    overridePrototype: QualifiedPrototypeName?
): TResult? =
    evaluateAttributeValue(attributePath, TResult::class.java, FusionContextLayer.empty(), overridePrototype)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateAttributeValue(
    attributePath: RelativeFusionPathName,
    nextContext: FusionContextLayer
): TResult? =
    evaluateAttributeValue(attributePath, TResult::class.java, nextContext, null)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateAttributeValue(
    attributePath: RelativeFusionPathName,
): TResult? =
    evaluateAttributeValue(attributePath, TResult::class.java, FusionContextLayer.empty(), null)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateRequiredAttributeValue(
    attributePath: RelativeFusionPathName,
    nextContext: FusionContextLayer,
    overridePrototype: QualifiedPrototypeName?
): TResult =
    evaluateRequiredAttributeValue(attributePath, TResult::class.java, nextContext, overridePrototype)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateRequiredAttributeValue(
    attributePath: RelativeFusionPathName,
    overridePrototype: QualifiedPrototypeName?
): TResult =
    evaluateRequiredAttributeValue(attributePath, TResult::class.java, FusionContextLayer.empty(), overridePrototype)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateRequiredAttributeValue(
    attributePath: RelativeFusionPathName,
    nextContext: FusionContextLayer
): TResult =
    evaluateRequiredAttributeValue(attributePath, TResult::class.java, nextContext, null)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateRequiredAttributeValue(
    attributePath: RelativeFusionPathName,
): TResult =
    evaluateRequiredAttributeValue(attributePath, TResult::class.java, FusionContextLayer.empty(), null)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateRequiredAttributeOptionalValue(
    attributePath: RelativeFusionPathName,
    nextContext: FusionContextLayer,
    overridePrototype: QualifiedPrototypeName?
): TResult? =
    evaluateRequiredAttributeOptionalValue(attributePath, TResult::class.java, nextContext, overridePrototype)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateRequiredAttributeOptionalValue(
    attributePath: RelativeFusionPathName,
    overridePrototype: QualifiedPrototypeName?
): TResult? =
    evaluateRequiredAttributeOptionalValue(
        attributePath,
        TResult::class.java,
        FusionContextLayer.empty(),
        overridePrototype
    )

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateRequiredAttributeOptionalValue(
    attributePath: RelativeFusionPathName,
    nextContext: FusionContextLayer
): TResult? =
    evaluateRequiredAttributeOptionalValue(attributePath, TResult::class.java, nextContext, null)

inline fun <reified TResult> FusionRuntimeImplementationAccess.evaluateRequiredAttributeOptionalValue(
    attributePath: RelativeFusionPathName,
): TResult? =
    evaluateRequiredAttributeOptionalValue(attributePath, TResult::class.java, FusionContextLayer.empty(), null)
