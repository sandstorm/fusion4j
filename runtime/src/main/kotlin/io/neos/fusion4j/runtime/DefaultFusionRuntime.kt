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
import io.neos.fusion4j.lang.model.FusionPathNameBuilder
import io.neos.fusion4j.lang.model.QualifiedPrototypeName
import io.neos.fusion4j.lang.model.RelativeFusionPathName
import io.neos.fusion4j.lang.model.decl.FusionLangElement
import io.neos.fusion4j.lang.model.values.*
import io.neos.fusion4j.lang.semantic.*
import io.neos.fusion4j.lang.util.FusionProfiler
import io.neos.fusion4j.runtime.chain.*
import io.neos.fusion4j.runtime.eel.EelEvaluator
import io.neos.fusion4j.runtime.eel.EelThisPointerRuntimeAccess
import io.neos.fusion4j.runtime.model.AppliedAttributeSource
import io.neos.fusion4j.runtime.model.FusionDataStructure
import io.neos.fusion4j.runtime.model.RuntimeFusionObjectInstance
import mu.KLogger
import mu.KotlinLogging

private val log: KLogger = KotlinLogging.logger {}

/**
 * Immutable, thread-safe Fusion Runtime implementation. See [FusionRuntime] for docs.
 */
class DefaultFusionRuntime(
    override val semanticallyNormalizedFusionModel: SemanticallyNormalizedFusionModel,
    private val eelEvaluator: EelEvaluator,
    private val contextInitializer: FusionContextInitializer = DefaultFusionContextInitializer(),
    private val implementationFactory: FusionObjectImplementationFactory = ClassLoadingReflectionImplementationFactory(),
    private val fusionProfiler: FusionProfiler
) : FusionRuntime {

    private val fusionObjectImplementationCache: Map<String, FusionObjectImplementation> = run {
        log.info { "Initializing Fusion object implementation cache" }
        semanticallyNormalizedFusionModel.rawIndex.allEffectiveFusionObjectImplementationClassNames
            .filterNot { it.contains("\\") || it.contains("/") }
            .mapNotNull {
                try {
                    it to implementationFactory.createInstance(it)
                } catch (error: Throwable) {
                    log.error { "Could not pre-cache Fusion object implementation class '$it'; ${error.message}" }
                    null
                }
            }
            .toMap()
    }

    override fun <TResult> evaluateLazy(
        path: AbsoluteFusionPathName,
        outputType: Class<TResult>,
        fusionContext: FusionContext,
        overridePrototype: QualifiedPrototypeName?
    ): Lazy<TResult?> =
        internalEvaluatePathWithOverridePrototypeHandling(
            // entrypoint starts a new Fusion callstack
            FusionRuntimeStack.initial(fusionContext),
            FusionEvaluationRequest.initial(path, outputType, overridePrototype)
        ).toLazy()

    private fun createEvaluationPath(
        callstack: FusionRuntimeStack,
        request: FusionEvaluationRequest<*>,
        prototypeName: QualifiedPrototypeName?,
        langElement: FusionLangElement?
    ): EvaluationPath {
        val requestType = request.requestType
        return if (requestType is AbsoluteEvaluationPathRequest) {
            // absolute paths do create a new evaluation path
            EvaluationPath.initialAbsolute(requestType.absolutePath, prototypeName)
        } else {
            // relative paths do extend the previous evaluation path
            val relativePath = when (requestType) {
                is RelativeEvaluationPathRequest -> requestType.relativePath
                is FusionValueReferencePathRequest -> when (request.callee) {
                    FusionEvaluationCallee.CONTEXT_INIT -> requestType.absolutePath.relativeToRoot()
                    else -> requestType.relativePath
                }
                is AppliedFusionValuePathRequest -> requestType.relativePath
                else -> throw FusionRuntimeException(
                    callstack,
                    "Unknown evaluation request type $requestType",
                    langElement
                )
            }
            val currentEvaluationPath = callstack.currentEvaluationPath
            val baseEvaluationPathSegments = when {
                // context evaluation gets an own evaluation path
                request.callee == FusionEvaluationCallee.CONTEXT_INIT -> emptyList()
                currentEvaluationPath == null -> emptyList()
                // TODO that might not work recursively -> test multiple this-pointer nesting depths
                request.callee == FusionEvaluationCallee.EEL_THIS_POINTER -> currentEvaluationPath.segments.dropLast(1)
                else -> currentEvaluationPath.segments
            }
            EvaluationPath(
                baseEvaluationPathSegments +
                        EvaluationPathSegment(relativePath, prototypeName)
            )
        }
    }


    private fun <TResult> evaluateWithChain(
        nextStack: FusionRuntimeStack,
        request: FusionEvaluationRequest<TResult>,
        evaluationFunction: EvaluationFunction<Any?>,
        runtimeInstance: RuntimeFusionObjectInstance? = null
    ): LazyFusionEvaluation<TResult?> {

        val runtimeAccessForChain = DefaultEvaluationChainRuntimeAccess(
            semanticallyNormalizedFusionModel.rawIndex,
            this,
            nextStack,
            request,
            runtimeInstance
        )
        if (runtimeInstance != null) {
            log.debug("new chain for instance: ${request.requestType.absolutePath}")
        } else {
            log.debug("new chain for path: ${request.requestType.absolutePath}")
        }

        val evaluationChain: EvaluationChain<TResult> =
            EvaluationChain(runtimeAccessForChain, request.outputType)
        val lazyFusionEvaluation = LazyFusionEvaluation.createEvaluation(
            request,
            nextStack,
            evaluationFunction
        )

        // two stages:
        //   1. create context based on previous layer
        //   2. process chain with immutable context
        //  both steps have a common runtime stack element
        val chainResult = evaluationChain.evaluateChain(lazyFusionEvaluation)
        // from this point on, the context cannot change anymore for this evaluation

        log.debug(
            "chain result for ${nextStack.currentStack.first()} will be evaluated " +
                    "with context:\n    ${nextStack.currentContext.currentContextMap}"
        )

        return chainResult
    }

    internal fun <TResult> internalEvaluatePathWithOverridePrototypeHandling(
        callstack: FusionRuntimeStack,
        request: FusionEvaluationRequest<TResult>
    ): LazyFusionEvaluation<TResult?> {
        return if (request.overridePrototype != null) {
            evaluateFusionObject(
                callstack,
                request,
                request.overridePrototype
            )
        } else {
            internalEvaluateFusionValue(
                callstack,
                request
            )
        }
    }

    internal fun <TResult> internalEvaluateFusionValue(
        callstack: FusionRuntimeStack,
        request: FusionEvaluationRequest<TResult>
    ): LazyFusionEvaluation<TResult?> {

        if (request.requestType is AppliedFusionValuePathRequest) {
            return internalEvaluateAppliedFusionValue(
                callstack,
                request
            )
        }

        // TODO log if unreferenced value is used? may impact performance
        val effectiveFusionValue = request.referencedFusionValue?.fusionValue
            ?: getAbsolutePathFusionValue(request).fusionValue

        return when (effectiveFusionValue) {
            // --- fusion object -> possible rendering recursion here
            is FusionObjectValue -> evaluateFusionObject(
                callstack,
                request,
                effectiveFusionValue.qualifiedPrototypeName
            )
            // --- TODO remove DSL from runtime model, since it is only parse time relevant
            is DslDelegateValue -> TODO("(runtime) DSLs are unsupported for now")
            // --- EEL Expressions
            is ExpressionValue -> evaluateEelExpression(
                callstack,
                request,
                effectiveFusionValue,
            )
            // ####### everything below is a pure fusion value, that is a leaf in the fusion evaluation tree
            // --- primitive Fusion values
            else -> internalEvaluatePrimitiveFusionValue(
                callstack,
                request,
                effectiveFusionValue
            )
        }
    }

    private fun getAbsolutePathFusionValue(request: FusionEvaluationRequest<*>) =
        semanticallyNormalizedFusionModel.rawIndex
            .getFusionValueForPath(request.requestType.absolutePath)

    private fun initializeContextLayer(
        callstack: FusionRuntimeStack,
        request: FusionEvaluationRequest<*>,
        evaluationPath: EvaluationPath,
        runtimeInstance: RuntimeFusionObjectInstance? = null
    ): FusionContextLayer {
        // initialize context

        val contextInitRuntimeAccess = DefaultContextInitializationRuntimeAccess(
            semanticallyNormalizedFusionModel.rawIndex,
            this,
            // for context initialization the old call stack is used
            callstack,
            request,
            runtimeInstance
        )
        val lazyContextLayer: FusionContextLayer = contextInitializer.initializeLazyContext(
            evaluationPath,
            callstack,
            request,
            contextInitRuntimeAccess
        )
        return lazyContextLayer
    }

    private fun <TResult> internalEvaluateAppliedFusionValue(
        callstack: FusionRuntimeStack,
        request: FusionEvaluationRequest<TResult>
    ): LazyFusionEvaluation<TResult?> {
        val appliedValueRequest = request.requestType as AppliedFusionValuePathRequest
        val evaluationPath = createEvaluationPath(callstack, request, null, appliedValueRequest.declaration)
        val nextStack = callstack.nextAppliedValueEval(
            evaluationPath,
            appliedValueRequest
        )
        return evaluateWithChain(
            nextStack,
            request,
            AppliedValueEvaluation(evaluationPath, appliedValueRequest)
        )
    }

    private fun <TResult> internalEvaluatePrimitiveFusionValue(
        callstack: FusionRuntimeStack,
        request: FusionEvaluationRequest<TResult>,
        effectiveFusionValue: FusionValue,
    ): LazyFusionEvaluation<TResult?> {
        val declaration = request.referencedFusionValue?.decl
            ?: getAbsolutePathFusionValue(request).decl
        val evaluationPath = createEvaluationPath(callstack, request, null, declaration)
        val contextLayer = initializeContextLayer(callstack, request, evaluationPath)
        val nextStack = callstack.nextPrimitiveValueEval(
            evaluationPath,
            effectiveFusionValue,
            declaration,
            contextLayer
        )
        val chainResult = evaluateWithChain(
            nextStack,
            request,
            PrimitiveEvaluation(evaluationPath, effectiveFusionValue) {
                // context is unused here
                when (effectiveFusionValue) {
                    is NullValue -> null
                    // path erasure
                    is ErasedValue -> null
                    is PrimitiveFusionValue<*> -> effectiveFusionValue.value
                    else -> throw FusionRuntimeException(
                        callstack,
                        "Cannot evaluate fusion value $effectiveFusionValue",
                        declaration
                    )
                }
            }
        )
        return if (fusionProfiler.enabled) {
            chainResult.flatMapEvaluation("profiling") { eval ->
                fusionProfiler.profile(
                    FusionProfiler.PROFILER_EVALUATE_PRIMITIVE,
                    "evaluate primitive $evaluationPath",
                    eval
                )
            }
        } else {
            chainResult
        }
    }


    private fun <TResult> evaluateEelExpression(
        callstack: FusionRuntimeStack,
        request: FusionEvaluationRequest<TResult>,
        expressionValue: ExpressionValue
    ): LazyFusionEvaluation<TResult?> {
        val declaration = request.referencedFusionValue?.decl
            ?: getAbsolutePathFusionValue(request).decl
        val evaluationPath = createEvaluationPath(callstack, request, null, declaration)
        val contextLayer = initializeContextLayer(callstack, request, evaluationPath)
        val nextStack = callstack.nextEelExpressionEval(
            evaluationPath,
            expressionValue,
            declaration,
            contextLayer
        )
        return evaluateWithChain(
            nextStack,
            request,
            EelEvaluation(evaluationPath, expressionValue) {
                val runtimeAccess: EelThisPointerRuntimeAccess = DefaultEelThisPointerRuntimeAccess(
                    semanticallyNormalizedFusionModel.rawIndex,
                    this,
                    nextStack,
                    request.runtimeFusionObjectInstance,
                    request
                )

                eelEvaluator.evaluateEelExpression(
                    runtimeAccess,
                    expressionValue,
                    nextStack.currentContext
                )
            }
        )
    }

    private fun <TResult> evaluateFusionObject(
        callstack: FusionRuntimeStack,
        request: FusionEvaluationRequest<TResult>,
        prototypeName: QualifiedPrototypeName
    ): LazyFusionEvaluation<TResult?> {
        val declaration: FusionLangElement = findDeclaration(request, callstack)
        val evaluationPath = createEvaluationPath(callstack, request, prototypeName, declaration)
        val fusionObjectInstance = try {
            semanticallyNormalizedFusionModel.fusionObjectInstanceLoader.loadInstance(
                evaluationPath,
                declaration
            )
        } catch (semanticError: FusionSemanticError) {
            throw FusionRuntimeException(callstack, "Could not load Fusion object instance", declaration, semanticError)
        }
        val runtimeInstance = try {
            toRuntimeInstance(fusionObjectInstance, callstack, request)
        } catch (semanticError: FusionSemanticError) {
            throw FusionRuntimeException(
                callstack,
                "Could not load Fusion object runtime instance",
                declaration,
                semanticError
            )
        }

        val contextLayer = initializeContextLayer(callstack, request, evaluationPath, runtimeInstance)
        val nextStack = callstack.nextFusionObjectInstanceEval(evaluationPath, fusionObjectInstance, contextLayer)
        // TODO cache instances!!!!

        val chainResult = evaluateWithChain(
            nextStack,
            request,
            FusionObjectEvaluation(evaluationPath) {
                // load prototype and Fusion object instance
                val runtimeAccess = DefaultImplementationRuntimeAccess(
                    runtimeInstance,
                    semanticallyNormalizedFusionModel.rawIndex,
                    semanticallyNormalizedFusionModel.prototypeStore,
                    nextStack,
                    this,
                    request
                )
                // find and instantiate prototype implementation class
                val implementationClassName = fusionObjectInstance.implementationClass

                val implementation = fusionObjectImplementationCache[implementationClassName]
                    ?: try {
                        implementationFactory.createInstance(implementationClassName)
                    } catch (error: Throwable) {
                        throw FusionRuntimeException(
                            nextStack,
                            "Could not instantiate implementation class Fusion object '${prototypeName}' - ${error::class.java.name}: ${error.message}",
                            declaration,
                            error
                        )
                    }
                val value = implementation.evaluate(runtimeAccess)
                value
            },
            runtimeInstance
        )

        return if (fusionProfiler.enabled) {
            chainResult.flatMapEvaluation("profiling") { eval ->
                fusionProfiler.profile(
                    FusionProfiler.PROFILER_EVALUATE_FUSION_OBJECT,
                    "evaluate fusion object $evaluationPath",
                    eval
                )
            }
        } else {
            chainResult
        }
    }

    private fun <TResult> findDeclaration(
        request: FusionEvaluationRequest<TResult>,
        callstack: FusionRuntimeStack
    ) = when (val requestType = request.requestType) {
        is FusionValueReferencePathRequest -> requestType.reference.decl
        is AppliedFusionValuePathRequest -> requestType.declaration
        is AbsoluteEvaluationPathRequest -> semanticallyNormalizedFusionModel.rawIndex.getFusionValueForPath(
            requestType.absolutePath
        ).decl
        is RelativeEvaluationPathRequest -> semanticallyNormalizedFusionModel.rawIndex.getFusionValueForPath(
            requestType.absolutePath
        ).decl
        else -> throw FusionRuntimeException(
            callstack,
            "No declaration found for instance request: $request",
            callstack.currentStackItem?.associatedFusionLangElement
        )
    }

    private fun toRuntimeInstance(
        fusionObjectInstance: FusionObjectInstance,
        nextStack: FusionRuntimeStack,
        request: FusionEvaluationRequest<*>
    ): RuntimeFusionObjectInstance {
        val appliedAttributes: Map<RelativeFusionPathName, AppliedAttributeSource> =
            fusionObjectInstance.applyAttributes
                // already sorted via @position
                .map { applyEntry ->
                    val applyName = applyEntry.first
                    val applyValueReference = applyEntry.second
                    val declaration = applyValueReference.decl
                    val applyValue = applyValueReference.fusionValue
                    if (applyValue !is ExpressionValue) {
                        throw FusionRuntimeException(
                            nextStack,
                            "Fusion value of @apply.$applyName must be an expression; given: ${applyValueReference.fusionValue}",
                            declaration
                        )
                    } else {
                        val lazyApplyResult = evaluateEelExpression(
                            nextStack,
                            FusionEvaluationRequest.applyAttributeExpression(
                                request,
                                applyName,
                                applyValueReference,
                                fusionObjectInstance
                            ),
                            applyValue
                        )
                        when (val applyResult = FusionRuntime.unwrapLazy(lazyApplyResult.toLazy())) {
                            is List<*> -> renderApplyResult(applyName, applyResult, nextStack, declaration)
                            is FusionDataStructure<*> -> renderApplyResult(
                                applyName,
                                applyResult,
                                nextStack,
                                declaration
                            )
                            null -> emptyList()
                            else -> throw FusionRuntimeException(
                                nextStack,
                                "Evaluated Fusion value of @apply.${applyName.propertyName} only supports data structures /" +
                                        " List<Pair<*, *>>; but was of type ${applyResult::class.java}, value: $applyResult",
                                declaration
                            )
                        }
                    }
                }
                .fold(emptyMap()) { result, current ->
                    result + current.toMap()
                }
        return RuntimeFusionObjectInstance(
            fusionObjectInstance,
            appliedAttributes
        )
    }

    private fun renderApplyResult(
        applyName: RelativeFusionPathName,
        applyResult: List<*>,
        nextStack: FusionRuntimeStack,
        declaration: FusionLangElement
    ): List<Pair<RelativeFusionPathName, AppliedAttributeSource>> =
        applyResult
            .filterNotNull()
            .map {
                if (it is Pair<*, *>) {
                    val property = FusionPathNameBuilder.relative().property(
                        if (it.first is String) it.first as String else it.first!!.toString()
                    ).build()
                    property to AppliedAttributeSource(
                        it.second,
                        declaration
                    )
                } else {
                    throw FusionRuntimeException(
                        nextStack,
                        "Evaluated Fusion value of @apply.${applyName.propertyName} only supports data structures" +
                                " / List<Pair<*, *>>; but was: List<${it::class.java}>",
                        declaration
                    )
                }
            }

}