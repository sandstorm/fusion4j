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

import io.neos.fusion4j.lang.model.RelativeFusionPathName
import io.neos.fusion4j.lang.semantic.EvaluationPath
import io.neos.fusion4j.lang.semantic.FusionPaths
import io.neos.fusion4j.lang.semantic.FusionValueReference
import io.neos.fusion4j.lang.semantic.RawFusionIndex
import io.neos.fusion4j.runtime.DefaultFusionRuntime
import io.neos.fusion4j.runtime.FusionEvaluationRequest
import io.neos.fusion4j.runtime.FusionRuntimeStack
import io.neos.fusion4j.runtime.LazyFusionEvaluation
import io.neos.fusion4j.runtime.model.RuntimeFusionObjectInstance

/**
 * See [ContextInitializationRuntimeAccess] for docs.
 */
class DefaultContextInitializationRuntimeAccess(
    private val rawFusionIndex: RawFusionIndex,
    private val runtime: DefaultFusionRuntime,
    override val callstack: FusionRuntimeStack,
    private val request: FusionEvaluationRequest<*>,
    private val runtimeInstance: RuntimeFusionObjectInstance?
) : ContextInitializationRuntimeAccess {

    override fun getFusionValue(
        evaluationPath: EvaluationPath,
        subPath: RelativeFusionPathName
    ): FusionValueReference? {
        TODO("Not yet implemented")
    }

    override fun getAllAttributes(
        evaluationPath: EvaluationPath,
        subPath: RelativeFusionPathName
    ): Map<RelativeFusionPathName, FusionValueReference> {
        return if (runtimeInstance != null) {
            // fusion object evaluation
            when (subPath) {
                FusionPaths.CONTEXT_META_ATTRIBUTE -> runtimeInstance.fusionObjectInstance.contextAttributes
                // this means probably a custom chain element
                // TODO maybe log here for performance warning
                else -> {
                    runtimeInstance.fusionObjectInstance.getSubAttributes(evaluationPath, subPath)
                }
            }
        } else {
            // path evaluation
            rawFusionIndex.resolveNestedAttributeFusionValues(request.requestType.absolutePath + subPath)
        }
    }

    override fun <TResult> evaluateAttribute(
        attribute: FusionValueReference,
        subPath: RelativeFusionPathName,
        outputType: Class<TResult>,
        chainStepDescription: String
    ): LazyFusionEvaluation<TResult?> =
        runtime.internalEvaluateFusionValue(
            callstack,
            FusionEvaluationRequest.contextInitializationAttribute(
                attribute,
                request,
                subPath,
                outputType,
                chainStepDescription,
                runtimeInstance
            )
        )
}