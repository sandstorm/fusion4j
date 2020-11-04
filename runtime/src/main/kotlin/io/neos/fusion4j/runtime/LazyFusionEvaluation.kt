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

import io.neos.fusion4j.lang.model.values.ExpressionValue
import io.neos.fusion4j.lang.model.values.FusionValue
import io.neos.fusion4j.lang.semantic.EvaluationPath
import mu.KLogger
import mu.KotlinLogging

private val log: KLogger = KotlinLogging.logger {}

class LazyFusionEvaluation<TResult> private constructor(
    val request: FusionEvaluationRequest<*>,
    private val currentStack: FusionRuntimeStack,
    private val evaluationFunction: EvaluationFunction<TResult>,
    val cancelled: Boolean = false
) {

    val evaluationPath: EvaluationPath = evaluationFunction.evaluationPath

    companion object {
        fun <TResult> createEvaluation(
            request: FusionEvaluationRequest<*>,
            currentStack: FusionRuntimeStack,
            evaluationFunction: EvaluationFunction<TResult>,
        ): LazyFusionEvaluation<TResult> =
            LazyFusionEvaluation(
                request,
                currentStack,
                evaluationFunction
            )
    }

    fun toLazy(): Lazy<TResult?> {
        return lazy {
            val value = evaluationFunction.eval()
            log.debug { "evaluated $request with $value" }
            value
        }
    }

    fun flatMapEvaluation(
        description: String,
        mapper: (() -> TResult?) -> TResult?
    ): LazyFusionEvaluation<TResult> =
        LazyFusionEvaluation(
            request,
            currentStack,
            FlatMapEvaluation(evaluationFunction, description, mapper)
        )

    fun <TOut> mapResult(
        description: String,
        mapper: (TResult?) -> TOut
    ): LazyFusionEvaluation<TOut> =
        LazyFusionEvaluation(
            request,
            currentStack,
            ChainMapEvaluation(evaluationFunction, description, mapper)
        )

    fun <T> cancelEvaluation(): LazyFusionEvaluation<T> =
        LazyFusionEvaluation(
            request,
            currentStack,
            EmptyEvaluation(evaluationFunction.evaluationPath),
            true
        )

}

interface EvaluationFunction<TResult> {
    val evaluationPath: EvaluationPath
    fun eval(): TResult?
}

class PrimitiveEvaluation<TResult>(
    override val evaluationPath: EvaluationPath,
    private val fusionValue: FusionValue,
    private val function: () -> TResult,
) : EvaluationFunction<TResult> {
    override fun eval(): TResult =
        function.invoke()

    override fun toString(): String = "eval primitive $fusionValue"
}

class AppliedValueEvaluation<TResult>(
    override val evaluationPath: EvaluationPath,
    private val appliedValueRequest: AppliedFusionValuePathRequest
) : EvaluationFunction<TResult> {
    @Suppress("UNCHECKED_CAST")
    override fun eval(): TResult =
        appliedValueRequest.appliedValue as TResult

    override fun toString(): String = "eval applied ${appliedValueRequest.appliedValue}"
}

class EelEvaluation<TResult>(
    override val evaluationPath: EvaluationPath,
    val expressionValue: ExpressionValue,
    private val function: () -> TResult
) : EvaluationFunction<TResult> {
    override fun eval(): TResult = function.invoke()

    override fun toString(): String = "eval EEL $expressionValue"
}

class FusionObjectEvaluation<TResult>(
    override val evaluationPath: EvaluationPath,
    private val function: () -> TResult
) : EvaluationFunction<TResult> {
    override fun eval(): TResult = function.invoke()

    override fun toString(): String = "eval Fusion object instance $evaluationPath"
}

class EmptyEvaluation<TResult>(
    override val evaluationPath: EvaluationPath
) : EvaluationFunction<TResult> {
    override fun eval(): TResult? {
        return null
    }

    override fun toString(): String = "empty evaluation"
}

class ChainMapEvaluation<TIn, TOut>(
    private val previousEvaluation: EvaluationFunction<TIn>,
    private val description: String,
    private val mapper: (TIn?) -> TOut
) : EvaluationFunction<TOut> {
    override val evaluationPath: EvaluationPath = previousEvaluation.evaluationPath

    override fun eval(): TOut? {
        val value = previousEvaluation.eval()
        log.debug("mapping $value with $description")
        val mappedValue = mapper.invoke(value)
        log.debug("mapping result: $mappedValue")
        return mappedValue
    }

    override fun toString(): String = "map $description"
}

class FlatMapEvaluation<TOut>(
    private val previousEvaluation: EvaluationFunction<TOut>,
    private val description: String,
    private val mapper: (() -> TOut?) -> TOut?
) : EvaluationFunction<TOut> {
    override val evaluationPath: EvaluationPath = previousEvaluation.evaluationPath

    override fun eval(): TOut? {
        val mappedValue = mapper.invoke(previousEvaluation::eval)
        log.debug("flat mapping result: $mappedValue")
        return mappedValue
    }

    override fun toString(): String = "map $description"
}