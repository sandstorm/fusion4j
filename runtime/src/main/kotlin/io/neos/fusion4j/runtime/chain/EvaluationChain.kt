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

package io.neos.fusion4j.runtime.chain

import io.neos.fusion4j.runtime.FusionRuntimeException
import io.neos.fusion4j.runtime.FusionRuntimeStack
import io.neos.fusion4j.runtime.LazyFusionEvaluation

class EvaluationChain<TResult>(
    private val runtimeAccess: EvaluationChainRuntimeAccess,
    private val outputType: Class<TResult>
) {

    companion object {
        private val IF_PRE_PROCESSOR = IfPreProcessor()
        private val PROCESS_POST_PROCESSOR = ProcessPostProcessor()
    }

    fun evaluateChain(lazyValue: LazyFusionEvaluation<Any?>): LazyFusionEvaluation<TResult?> {
        // @if
        val cancelled = IF_PRE_PROCESSOR.isEvaluationCancelled(lazyValue, runtimeAccess)
        if (cancelled) {
            return lazyValue.cancelEvaluation()
        }
        // @process
        val postProcessed = PROCESS_POST_PROCESSOR.postProcessValue(lazyValue, runtimeAccess)

        // return with auto cast mapper
        return postProcessed
            .mapResult("auto-cast") { untypedValue ->
                autoCast(untypedValue, outputType, runtimeAccess.callstack)
            }
    }

}

@Suppress("UNCHECKED_CAST")
private fun <T> boxedType(outputType: Class<T>): Class<T> =
    when (outputType.name) {
        "boolean" -> Boolean::class.javaObjectType
        "byte" -> Byte::class.javaObjectType
        "char" -> Char::class.javaObjectType
        "short" -> Short::class.javaObjectType
        "int" -> Int::class.javaObjectType
        "long" -> Long::class.javaObjectType
        "float" -> Float::class.javaObjectType
        "double" -> Double::class.javaObjectType
        else -> throw IllegalArgumentException("cannot get boxed type for primitive: $outputType")
    } as Class<T>

@Suppress("UNCHECKED_CAST")
private fun <T> autoCast(evaluatedValue: Any?, outputType: Class<T>, callstack: FusionRuntimeStack): T? {
    if (evaluatedValue == null) {
        return null as T
    }
    val autoBoxedOutputType = if (outputType.isPrimitive) {
        boxedType(outputType)
    } else {
        outputType
    }
    return when {
        autoBoxedOutputType.isAssignableFrom(evaluatedValue::class.java) ->
            autoBoxedOutputType.cast(evaluatedValue)
        autoBoxedOutputType == String::class.java ->
            evaluatedValue.toString() as T
        else ->
            throw FusionRuntimeException(
                callstack,
                "Cannot convert Fusion output from ${autoBoxedOutputType.name} to ${outputType.name}; value: $evaluatedValue",
                callstack.currentStackItem?.associatedFusionLangElement
            )
    }
}