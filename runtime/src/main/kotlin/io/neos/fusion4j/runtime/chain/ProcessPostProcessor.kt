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

import io.neos.fusion4j.lang.semantic.FusionPaths
import io.neos.fusion4j.runtime.FusionContextLayer
import io.neos.fusion4j.runtime.LazyFusionEvaluation
import mu.KLogger
import mu.KotlinLogging

private val log: KLogger = KotlinLogging.logger {}

class ProcessPostProcessor {

    companion object {
        private const val CONTEXT_VAR_NAME: String = "value"
    }

    fun postProcessValue(
        lazyValue: LazyFusionEvaluation<Any?>,
        runtimeAccess: EvaluationChainRuntimeAccess
    ): LazyFusionEvaluation<Any?> {
        // TODO sorted!
        val allProcessors = runtimeAccess.getAllAttributes(
            lazyValue.evaluationPath,
            FusionPaths.PROCESS_META_ATTRIBUTE
        ).values.toList()

        if (allProcessors.isEmpty()) {
            return lazyValue
        }

        val processed = allProcessors
            .fold(lazyValue) { result, processorAttribute ->
                val contextSubLayer = FusionContextLayer.layerOf(
                    "@process-value",
                    mapOf(CONTEXT_VAR_NAME to result.toLazy())
                )
                //log.debug { "processing value $result with ${processorAttribute.relativePath}" }
                val processorResult = runtimeAccess.evaluateAttribute(
                    processorAttribute,
                    FusionPaths.PROCESS_META_ATTRIBUTE,
                    contextSubLayer,
                    "@process"
                )
                // @if working - a false condition on the processor itself result in a pass-through processor
                if (processorResult.cancelled) {
                    //log.debug {"skipping post-processor since chain is cancelled $processorResult" }
                    result
                } else {
                    result.mapResult("@process-${processorAttribute.relativePath}") {
                        processorResult.toLazy().value
                    }
                }
            }

        return processed
    }
}