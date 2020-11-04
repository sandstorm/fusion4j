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

package io.neos.fusion4j.runtime

import io.neos.fusion4j.lang.model.AbsoluteFusionPathName
import io.neos.fusion4j.lang.model.QualifiedPrototypeName
import io.neos.fusion4j.lang.semantic.FusionPrototype
import io.neos.fusion4j.lang.semantic.SemanticallyNormalizedFusionModel
import io.neos.fusion4j.runtime.model.RuntimeFusionObjectInstance
import mu.KLogger
import mu.KotlinLogging

private val log: KLogger = KotlinLogging.logger {}

/**
 * FusionRuntime is the main implementation for evaluating Fusion. Usually, you pass in a [io.neos.fusion4j.lang.semantic.SemanticallyNormalizedFusionModel]
 * to instantiate it together with package entry points and a load order, and then call [evaluate] or [evaluateLazy] (which is the main entry point).
 *
 * [evaluate] then f.e. instantiates a [RuntimeFusionObjectInstance] (from its [FusionPrototype]),
 * and a corresponding [DefaultImplementationRuntimeAccess] object (which holds the evaluation stack at this point). Then, when
 * the fusion object is evaluated, this in turn calls the Fusion Runtime for nested evaluation (through the [DefaultImplementationRuntimeAccess]
 * wrapper again).
 *
 * The runtime does NOT contain any evaluation state and is fully stateless during evaluation. Evaluation State is stored
 * inside the "Access" objects (like [DefaultImplementationRuntimeAccess]).
 *
 *
 *
 *
 *                                                    ┌─────────────────────────────────────────┐
 *                                                    │                                         │
 *                                          ┌─────────│   DefaultImplementationRuntimeAccess    │
 *                                          │         │                                         │
 *                                          │         └─────────────────────────────────────────┘
 *                                          │
 *                                          │         ┌────────────────────────────────────────┐
 *                               delegate to│         │                                        │
 *                                          ├─────────┤   DefaultEelThisPointerRuntimeAccess   │
 *        ┌───────────────────────┐         │         │                                        │
 *        │                       │         │         └────────────────────────────────────────┘
 *        │ DefaultFusionRuntime  │◀────────┤
 *        │                       │         │         ┌───────────────────────────────────────┐
 *        └───────────────────────┘         │         │                                       │
 *                                          ├─────────┤  DefaultEvaluationChainRuntimeAccess  │
 *                                          │         │                                       │
 *                                          │         └───────────────────────────────────────┘
 *                                          │         ┌────────────────────────────────────────────┐
 *                                          │         │                                            │
 *                                          └─────────│ DefaultContextInitializationRuntimeAccess  │
 *                                                    │                                            │
 *                                                    └────────────────────────────────────────────┘
 *
 */
interface FusionRuntime {

    companion object {
        fun unwrapLazy(evaluatedValue: Any?, level: Int = 0): Any? {
            return if (evaluatedValue is Lazy<*>)
                unwrapLazy(evaluatedValue.value, level + 1)
            else
                evaluatedValue
        }
    }

    val semanticallyNormalizedFusionModel: SemanticallyNormalizedFusionModel

    fun <TResult> evaluateLazy(path: AbsoluteFusionPathName, outputType: Class<TResult>): Lazy<TResult?> =
        evaluateLazy(path, outputType, FusionContext.empty())

    fun <TResult> evaluate(path: AbsoluteFusionPathName, outputType: Class<TResult>): TResult? =
        evaluate(path, outputType, FusionContext.empty())

    fun <TResult> evaluateLazy(
        path: AbsoluteFusionPathName,
        outputType: Class<TResult>,
        context: FusionContext
    ): Lazy<TResult?> =
        evaluateLazy(path, outputType, context, null)

    fun <TResult> evaluate(
        path: AbsoluteFusionPathName,
        outputType: Class<TResult>,
        context: FusionContext
    ): TResult? =
        evaluate(path, outputType, context, null)

    fun <TResult> evaluateLazy(
        path: AbsoluteFusionPathName,
        outputType: Class<TResult>,
        fusionContext: FusionContext,
        overridePrototype: QualifiedPrototypeName?
    ): Lazy<TResult?>

    fun <TResult> evaluate(
        path: AbsoluteFusionPathName,
        outputType: Class<TResult>,
        fusionContext: FusionContext,
        overridePrototype: QualifiedPrototypeName?
    ): TResult? {
        val lazyValue = evaluateLazy(path, outputType, fusionContext, overridePrototype)
        val evaluatedValue = lazyValue.value
        // auto-unwrap any created / unprocessed lazy value
        return outputType.cast(unwrapLazy(evaluatedValue))
    }

}

inline fun <reified TResult> FusionRuntime.evaluateTypedLazy(path: AbsoluteFusionPathName): Lazy<TResult?> =
    evaluateLazy(path, TResult::class.java)

inline fun <reified TResult> FusionRuntime.evaluateTyped(path: AbsoluteFusionPathName): TResult? =
    evaluate(path, TResult::class.java)

inline fun <reified TResult> FusionRuntime.evaluateTypedLazy(
    path: AbsoluteFusionPathName,
    context: FusionContext
): Lazy<TResult?> =
    evaluateLazy(path, TResult::class.java, context)

inline fun <reified TResult> FusionRuntime.evaluateTyped(
    path: AbsoluteFusionPathName,
    context: FusionContext
): TResult? =
    evaluate(path, TResult::class.java, context)

inline fun <reified TResult> FusionRuntime.evaluateTypedLazy(
    path: AbsoluteFusionPathName,
    context: FusionContext,
    overridePrototype: QualifiedPrototypeName?
): Lazy<TResult?> =
    evaluateLazy(path, TResult::class.java, context, overridePrototype)

inline fun <reified TResult> FusionRuntime.evaluateTyped(
    path: AbsoluteFusionPathName,
    context: FusionContext,
    overridePrototype: QualifiedPrototypeName?
): TResult? =
    evaluate(path, TResult::class.java, context, overridePrototype)

