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
import io.neos.fusion4j.lang.model.RelativeFusionPathName
import io.neos.fusion4j.lang.semantic.PrototypeStore
import io.neos.fusion4j.lang.semantic.RawFusionIndex
import io.neos.fusion4j.runtime.model.EvaluationResult
import io.neos.fusion4j.lang.semantic.FusionAttribute
import io.neos.fusion4j.runtime.model.RuntimeFusionObjectInstance

/**
 * * See [FusionRuntimeImplementationAccess] for docs.
 */
class DefaultImplementationRuntimeAccess(
    override val runtimeInstance: RuntimeFusionObjectInstance,
    override val rawFusionIndex: RawFusionIndex,
    override val prototypeStore: PrototypeStore,
    override val callstack: FusionRuntimeStack,
    private val runtime: DefaultFusionRuntime,
    private val request: FusionEvaluationRequest<*>,
) : FusionRuntimeImplementationAccess {

    override fun <TResult> evaluateAbsolutePath(
        path: AbsoluteFusionPathName,
        outputType: Class<TResult>,
        contextLayer: FusionContextLayer,
        overridePrototype: QualifiedPrototypeName?
    ): EvaluationResult<TResult?> =
        EvaluationResult(
            runtime.internalEvaluatePathWithOverridePrototypeHandling(
                callstack,
                FusionEvaluationRequest.implementationAbsolute(
                    path,
                    request,
                    contextLayer,
                    outputType,
                    overridePrototype,
                    runtimeInstance
                )
            )
        )

    override fun <TResult> evaluateRelativePath(
        path: RelativeFusionPathName,
        outputType: Class<TResult>,
        contextLayer: FusionContextLayer,
        overridePrototype: QualifiedPrototypeName?
    ): EvaluationResult<TResult?> =
        EvaluationResult(
            runtime.internalEvaluatePathWithOverridePrototypeHandling(
                callstack,
                FusionEvaluationRequest.implementationRelative(
                    path,
                    request,
                    contextLayer,
                    outputType,
                    overridePrototype,
                    runtimeInstance
                )
            )
        )

    override fun <TResult> evaluateAttribute(
        attribute: FusionAttribute,
        outputType: Class<TResult>,
        contextLayer: FusionContextLayer,
        overridePrototype: QualifiedPrototypeName?
    ): EvaluationResult<TResult?> =
        EvaluationResult(
            runtime.internalEvaluatePathWithOverridePrototypeHandling(
                // FusionValue from attribute
                callstack,
                FusionEvaluationRequest.implementationAttribute(
                    attribute,
                    request,
                    contextLayer,
                    outputType,
                    overridePrototype,
                    runtimeInstance
                )
            )
        )
}