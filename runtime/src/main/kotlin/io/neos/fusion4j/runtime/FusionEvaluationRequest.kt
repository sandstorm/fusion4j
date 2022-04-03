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
import io.neos.fusion4j.lang.model.FusionPathName
import io.neos.fusion4j.lang.model.QualifiedPrototypeName
import io.neos.fusion4j.lang.model.RelativeFusionPathName
import io.neos.fusion4j.lang.model.decl.FusionLangElement
import io.neos.fusion4j.lang.semantic.FusionObjectInstance
import io.neos.fusion4j.lang.semantic.FusionValueReference
import io.neos.fusion4j.lang.semantic.AppliedFusionAttribute
import io.neos.fusion4j.lang.semantic.DeclaredFusionAttribute
import io.neos.fusion4j.lang.semantic.FusionAttribute
import io.neos.fusion4j.runtime.model.RuntimeFusionObjectInstance

data class FusionEvaluationRequest<TResult>(
    val callee: FusionEvaluationCallee,
    val requestType: EvaluationRequestType,
    val additionalContextLayer: FusionContextLayer,
    val outputType: Class<TResult>,
    val overridePrototype: QualifiedPrototypeName?,
    val runtimeFusionObjectInstance: RuntimeFusionObjectInstance?,
    val parentRequest: FusionEvaluationRequest<*>?,
    val description: String? = null,
    val fusionObjectInstance: FusionObjectInstance? = runtimeFusionObjectInstance?.fusionObjectInstance
) {

    val referencedFusionValue: FusionValueReference? = if (requestType is FusionValueReferencePathRequest) {
        requestType.reference
    } else null

    companion object {
        fun <TResult> initial(
            path: AbsoluteFusionPathName,
            outputType: Class<TResult>,
            overridePrototype: QualifiedPrototypeName?
        ): FusionEvaluationRequest<TResult> =
            FusionEvaluationRequest(
                FusionEvaluationCallee.ENTRYPOINT,
                AbsoluteEvaluationPathRequest(path),
                FusionContextLayer.empty(),
                outputType,
                //null,
                overridePrototype,
                null,
                null,
                null
            )

        fun <TResult> implementationAbsolute(
            path: AbsoluteFusionPathName,
            parentRequest: FusionEvaluationRequest<*>,
            additionalContextLayer: FusionContextLayer,
            outputType: Class<TResult>,
            overridePrototype: QualifiedPrototypeName?,
            fusionObjectInstance: RuntimeFusionObjectInstance?
        ): FusionEvaluationRequest<TResult> =
            FusionEvaluationRequest(
                FusionEvaluationCallee.FUSION_OBJECT_IMPLEMENTATION,
                AbsoluteEvaluationPathRequest(path),
                additionalContextLayer,
                outputType,
                overridePrototype,
                fusionObjectInstance,
                parentRequest
            )

        fun <TResult> implementationRelative(
            path: RelativeFusionPathName,
            parentRequest: FusionEvaluationRequest<*>,
            additionalContextLayer: FusionContextLayer,
            outputType: Class<TResult>,
            overridePrototype: QualifiedPrototypeName?,
            fusionObjectInstance: RuntimeFusionObjectInstance?
        ): FusionEvaluationRequest<TResult> =
            FusionEvaluationRequest(
                FusionEvaluationCallee.FUSION_OBJECT_IMPLEMENTATION,
                // TODO correct parent path here?
                RelativeEvaluationPathRequest(path, parentRequest.requestType.absolutePath),
                additionalContextLayer,
                outputType,
                overridePrototype,
                fusionObjectInstance,
                parentRequest
            )

        fun <TResult> implementationAttribute(
            attribute: FusionAttribute,
            parentRequest: FusionEvaluationRequest<*>,
            additionalContextLayer: FusionContextLayer,
            outputType: Class<TResult>,
            overridePrototype: QualifiedPrototypeName?,
            fusionObjectInstance: RuntimeFusionObjectInstance?
        ): FusionEvaluationRequest<TResult> =
            FusionEvaluationRequest(
                FusionEvaluationCallee.FUSION_OBJECT_IMPLEMENTATION,
                buildRequestTypeFromAttribute(attribute),
                additionalContextLayer,
                outputType,
                overridePrototype,
                fusionObjectInstance,
                parentRequest
            )

        private fun buildRequestTypeFromAttribute(
            attribute: FusionAttribute
        ): EvaluationRequestType =
            when (attribute) {
                is DeclaredFusionAttribute ->
                    FusionValueReferencePathRequest(attribute.valueReference)
                is AppliedFusionAttribute ->
                    AppliedFusionValuePathRequest(
                        attribute.evaluatedValue,
                        attribute.valueDecl,
                        attribute.absolutePath,
                        attribute.relativePath
                    )
                else -> throw IllegalArgumentException("Unsupported attribute type: $attribute")
            }

        fun <TResult> chainAttribute(
            attribute: FusionValueReference,
            parentRequest: FusionEvaluationRequest<*>,
            subPath: RelativeFusionPathName,
            additionalContextLayer: FusionContextLayer,
            outputType: Class<TResult>,
            chainStepDescription: String,
            fusionObjectInstance: RuntimeFusionObjectInstance?
        ): FusionEvaluationRequest<TResult> =
            FusionEvaluationRequest(
                FusionEvaluationCallee.EVALUATION_CHAIN,
                FusionValueReferencePathRequest(attribute, subPath),
                additionalContextLayer,
                outputType,
                null,
                fusionObjectInstance,
                parentRequest,
                chainStepDescription
            )

        fun <TResult> contextInitializationAttribute(
            attribute: FusionValueReference,
            parentRequest: FusionEvaluationRequest<*>,
            subPath: RelativeFusionPathName,
            outputType: Class<TResult>,
            contextInitDescription: String,
            fusionObjectInstance: RuntimeFusionObjectInstance?
        ): FusionEvaluationRequest<TResult> =
            FusionEvaluationRequest(
                FusionEvaluationCallee.CONTEXT_INIT,
                FusionValueReferencePathRequest(attribute, subPath),
                FusionContextLayer.empty(),
                outputType,
                null,
                fusionObjectInstance,
                parentRequest,
                contextInitDescription
            )

        fun applyAttributeExpression(
            parentRequest: FusionEvaluationRequest<*>,
            applyName: RelativeFusionPathName,
            applyValueReference: FusionValueReference,
            fusionObjectInstance: FusionObjectInstance
        ): FusionEvaluationRequest<Any> =
            FusionEvaluationRequest(
                FusionEvaluationCallee.FUSION_OBJECT_APPLY_EXPRESSION,
                FusionValueReferencePathRequest(applyValueReference),
                parentRequest.additionalContextLayer,
                Any::class.java,
                null,
                null,
                parentRequest,
                "@apply.${applyName.propertyName}",
                fusionObjectInstance
            )

        fun eelThisInstance(
            attribute: FusionAttribute,
            fusionObjectInstance: RuntimeFusionObjectInstance?,
            parentRequest: FusionEvaluationRequest<*>
        ): FusionEvaluationRequest<Any> =
            FusionEvaluationRequest(
                FusionEvaluationCallee.EEL_THIS_POINTER,
                buildRequestTypeFromAttribute(attribute),
                FusionContextLayer.empty(),
                Any::class.java,
                null,
                fusionObjectInstance,
                parentRequest
            )

        fun eelThisPath(
            relativePath: RelativeFusionPathName,
            fusionObjectInstance: RuntimeFusionObjectInstance?,
            request: FusionEvaluationRequest<*>,
            thisPointerPath: AbsoluteFusionPathName,
        ): FusionEvaluationRequest<Any> {
            return FusionEvaluationRequest(
                FusionEvaluationCallee.EEL_THIS_POINTER,
                RelativeEvaluationPathRequest(relativePath, thisPointerPath),
                FusionContextLayer.empty(),
                Any::class.java,
                null,
                fusionObjectInstance,
                request
            )
        }

        fun resolveThisPointerPath(
            callstack: FusionRuntimeStack,
            request: FusionEvaluationRequest<*>
        ): AbsoluteFusionPathName =
            getLastPublicApiRequest(callstack, request)

        private fun getLastPublicApiRequest(
            callstack: FusionRuntimeStack,
            request: FusionEvaluationRequest<*>,
        ): AbsoluteFusionPathName =
            if (request.callee.publicApi) {
                request.requestType.absolutePath
            } else {
                if (request.parentRequest == null) {
                    throw FusionRuntimeException(
                        callstack,
                        "Could not resolve EEL this-pointer for evaluation request $request; no parent request given",
                        request.referencedFusionValue?.decl ?: callstack.currentStackItem?.associatedFusionLangElement
                    )
                } else {
                    getLastPublicApiRequest(callstack, request.parentRequest)
                }
            }
    }

    /*
    TODO move to printer if needed
    override fun toString(): String =
        "${callee.description} " +
                "(${if (callee.publicApi) "public" else "intern"}) " +
                "${requestType.absolutePath} ${description ?: ""}, output type: " +
                "${outputType.name}, additional context: $additionalContextLayer"

     */
}

enum class FusionEvaluationCallee(
    val publicApi: Boolean,
    val description: String
) {
    ENTRYPOINT(true, "ENTRYPNT"),
    FUSION_OBJECT_IMPLEMENTATION(true, "OBJ_IMPL"),
    FUSION_OBJECT_APPLY_EXPRESSION(false, "APPLY"),
    EEL_THIS_POINTER(false, "EEL_THIS"),
    CONTEXT_INIT(false, "CTX_INIT"),
    EVALUATION_CHAIN(false, "EVAL_CHN"),
}

interface EvaluationRequestType {
    val absolutePath: AbsoluteFusionPathName
}

data class AbsoluteEvaluationPathRequest(
    override val absolutePath: AbsoluteFusionPathName
) : EvaluationRequestType

data class RelativeEvaluationPathRequest(
    val relativePath: RelativeFusionPathName,
    val parentPath: AbsoluteFusionPathName
) : EvaluationRequestType {
    override val absolutePath: AbsoluteFusionPathName = parentPath + relativePath
}

data class FusionValueReferencePathRequest(
    val reference: FusionValueReference,
    val subPath: RelativeFusionPathName = FusionPathName.current()
) : EvaluationRequestType {
    override val absolutePath: AbsoluteFusionPathName = reference.absolutePath
    val relativePath: RelativeFusionPathName =
        RelativeFusionPathName.fromSegments(subPath.segments + reference.relativePath.segments)
}

data class AppliedFusionValuePathRequest(
    val appliedValue: Any?,
    val declaration: FusionLangElement,
    override val absolutePath: AbsoluteFusionPathName,
    val relativePath: RelativeFusionPathName
) : EvaluationRequestType
