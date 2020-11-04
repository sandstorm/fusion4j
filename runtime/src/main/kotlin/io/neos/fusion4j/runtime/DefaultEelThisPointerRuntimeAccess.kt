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
import io.neos.fusion4j.lang.model.PropertyPathSegment
import io.neos.fusion4j.lang.model.RelativeFusionPathName
import io.neos.fusion4j.lang.semantic.RawFusionIndex
import io.neos.fusion4j.runtime.eel.EelThisPointerRuntimeAccess
import io.neos.fusion4j.runtime.model.RuntimeFusionObjectInstance

/**
 * See [EelThisPointerRuntimeAccess] for docs.
 *
 * TODO make lazy as well via JEXL arithmetic
 */
class DefaultEelThisPointerRuntimeAccess(
    private val rawFusionIndex: RawFusionIndex,
    private val runtime: DefaultFusionRuntime,
    override val callstack: FusionRuntimeStack,
    private val fusionObjectInstance: RuntimeFusionObjectInstance?,
    private val request: FusionEvaluationRequest<*>
) : EelThisPointerRuntimeAccess {

    private val thisPointerPath: AbsoluteFusionPathName =
        FusionEvaluationRequest.resolveThisPointerPath(callstack, request)

    override fun hasThisPointerAttribute(pathSegment: PropertyPathSegment): Boolean {
        val relativePath = RelativeFusionPathName.fromSegments(listOf(pathSegment))
        // object instance evaluation
        return fusionObjectInstance?.attributes?.containsKey(relativePath)
        // path evaluation
            ?: rawFusionIndex.pathIndex.isDeclared(thisPointerPath + relativePath)
    }

    override fun evaluateThisPointer(pathSegment: PropertyPathSegment): Lazy<Any?>? {
        val relativePath = RelativeFusionPathName.fromSegments(listOf(pathSegment))
        val nextRequest = if (fusionObjectInstance != null) {
            // object instance evaluation
            val attribute = fusionObjectInstance.getAttribute(
                relativePath
            ) ?: return null // non-strict mode
            FusionEvaluationRequest.eelThisInstance(
                attribute,
                fusionObjectInstance,
                request
            )
        } else {
            // path evaluation
            if (!rawFusionIndex.pathIndex.isDeclared(thisPointerPath + relativePath)) {
                // non-strict mode
                return null
            }
            FusionEvaluationRequest.eelThisPath(relativePath, fusionObjectInstance, request, thisPointerPath)
        }
        val lazyResult = runtime.internalEvaluateFusionValue(
            callstack,
            nextRequest
        )
        // TODO make lazy too, when our JEXL EEL Evaluator supports lazy arithmetics
        return lazyResult.toLazy()
    }

}