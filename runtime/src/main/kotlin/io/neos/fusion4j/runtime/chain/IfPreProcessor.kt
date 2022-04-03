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

class IfPreProcessor {

    companion object {
        private const val CONTEXT_VAR_NAME: String = "value"
    }

    fun isEvaluationCancelled(
        lazyValue: LazyFusionEvaluation<Any?>,
        runtimeAccess: EvaluationChainRuntimeAccess
    ): Boolean {
        // TODO sorted!
        //   there might be the use-case where you have multiple if conditions and want to
        //   evaluate a lower performance cost condition first!
        val allConditions = runtimeAccess
            .getAllAttributes(
                lazyValue.evaluationPath,
                FusionPaths.IF_META_ATTRIBUTE
            )
            .toList()
        if (allConditions.isEmpty()) {
            return false
        }

        val contextSubLayer = FusionContextLayer.layerOf(
            "@if-value",
            mapOf(CONTEXT_VAR_NAME to lazyValue.toLazy())
        )

        val firstFalseCondition = allConditions.firstOrNull { condition ->
            val conditionName = condition.first
            val conditionFusionValue = condition.second
            log.debug { "evaluating condition $conditionName" }
            val evaluatedConditionValue = runtimeAccess.evaluateAttribute(
                conditionFusionValue,
                FusionPaths.IF_META_ATTRIBUTE,
                contextSubLayer,
                Boolean::class.java,
                "@if"
            )
            if (evaluatedConditionValue.cancelled) {
                log.warn {"skipping condition $conditionName" }
                false
            } else {
                val value = evaluatedConditionValue.toLazy().value
                if (value == null) {
                    log.warn {"condition $conditionName evaluated to null" }
                    true
                } else {
                    !value
                }
            }
        }
        return firstFalseCondition != null
    }
}