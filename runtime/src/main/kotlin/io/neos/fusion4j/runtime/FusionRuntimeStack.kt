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

import io.neos.fusion4j.lang.model.decl.FusionLangElement
import io.neos.fusion4j.lang.model.decl.FusionPathAssignmentDecl
import io.neos.fusion4j.lang.model.values.ExpressionValue
import io.neos.fusion4j.lang.model.values.FusionValue
import io.neos.fusion4j.lang.semantic.EvaluationPath
import io.neos.fusion4j.lang.semantic.FusionObjectInstance

/**
 * Implementation Detail of [FusionRuntime], stored inside [DefaultImplementationRuntimeAccess]. See these classes for further docs.
 *
 * Immutable!
 */
data class FusionRuntimeStack(
    val initialContext: FusionContext,
    val currentStack: List<FusionStackElement>,
    val currentStackItem: FusionStackElement? = null
) {
    val currentEvaluationPath: EvaluationPath?
        get() = currentStackItem?.evaluationPath

    val currentContext: FusionContext
        get() = currentStackItem?.context ?: initialContext

    private fun nextElement(
        contextLayer: FusionContextLayer,
        factory: (FusionContext, Int) -> FusionStackElement
    ): FusionRuntimeStack {
        val nextDepth = currentStack.size + 1
        val nextContext = currentContext.push(contextLayer)
        val nextStackElement = factory.invoke(nextContext, nextDepth)
        return FusionRuntimeStack(
            initialContext,
            currentStack + nextStackElement,
            nextStackElement
        )
    }

    fun nextFusionObjectInstanceEval(
        evaluationPath: EvaluationPath,
        fusionObjectInstance: FusionObjectInstance,
        contextLayer: FusionContextLayer
    ): FusionRuntimeStack =
        nextElement(contextLayer) { context, depth ->
            FusionObjectInstanceEvalStackElement(evaluationPath, context, depth, fusionObjectInstance)
        }

    fun nextEelExpressionEval(
        evaluationPath: EvaluationPath,
        expressionValue: ExpressionValue,
        declaration: FusionLangElement,
        contextLayer: FusionContextLayer
    ): FusionRuntimeStack =
        nextElement(contextLayer) { context, depth ->
            EelExpressionEvalStackElement(evaluationPath, context, depth, expressionValue, declaration)
        }

    fun nextPrimitiveValueEval(
        evaluationPath: EvaluationPath,
        primitiveValue: FusionValue,
        declaration: FusionLangElement,
        contextLayer: FusionContextLayer
    ): FusionRuntimeStack =
        nextElement(contextLayer) { context, depth ->
            PrimitiveValueEvalStackElement(evaluationPath, context, depth, primitiveValue, declaration)
        }

    fun nextAppliedValueEval(
        evaluationPath: EvaluationPath,
        appliedValueRequest: AppliedFusionValuePathRequest
    ): FusionRuntimeStack =
        nextElement(FusionContextLayer.empty()) { context, depth ->
            AppliedValueEvalStackElement(evaluationPath, context, depth, appliedValueRequest)
        }

    // TODO print on error
    fun print(): String {
        return "Fusion Stack:\n -> " + currentStack.joinToString("\n -  ") { it.print() }
    }

    companion object {
        fun initial(context: FusionContext) =
            FusionRuntimeStack(
                context,
                emptyList()
            )
    }

}

interface FusionStackElement {
    val evaluationPath: EvaluationPath
    val context: FusionContext
    val depth: Int
    val associatedFusionLangElement: FusionLangElement?

    fun print(): String
}

data class FusionObjectInstanceEvalStackElement(
    override val evaluationPath: EvaluationPath,
    override val context: FusionContext,
    override val depth: Int,
    val fusionObjectInstance: FusionObjectInstance,
) : FusionStackElement {

    override val associatedFusionLangElement: FusionLangElement? =
        fusionObjectInstance.instanceDeclaration

    override fun print(): String {
        return "${nestedDepth()} INSTANCE | $evaluationPath"
    }
}

data class EelExpressionEvalStackElement(
    override val evaluationPath: EvaluationPath,
    override val context: FusionContext,
    override val depth: Int,
    val eelExpression: ExpressionValue,
    val declaration: FusionLangElement
) : FusionStackElement {
    override val associatedFusionLangElement: FusionLangElement =
        declaration

    override fun print(): String {
        return "${nestedDepth()} EEL | $evaluationPath"
    }
}

data class PrimitiveValueEvalStackElement(
    override val evaluationPath: EvaluationPath,
    override val context: FusionContext,
    override val depth: Int,
    val primitiveValue: FusionValue,
    val declaration: FusionLangElement
) : FusionStackElement {
    override val associatedFusionLangElement: FusionLangElement =
        declaration

    override fun print(): String {
        return "${nestedDepth()} PRIMITIVE ${primitiveValue.getReadableType()} | $evaluationPath"
    }
}

data class AppliedValueEvalStackElement(
    override val evaluationPath: EvaluationPath,
    override val context: FusionContext,
    override val depth: Int,
    val appliedValueRequest: AppliedFusionValuePathRequest
) : FusionStackElement {
    override val associatedFusionLangElement: FusionLangElement = run {
        val declaration = appliedValueRequest.declaration
        if (declaration is FusionPathAssignmentDecl) {
            declaration.valueDeclaration
        } else {
            declaration
        }
    }

    override fun print(): String {
        return "${nestedDepth()} APPLIED | $evaluationPath"
    }
}

private fun FusionStackElement.nestedDepth(): String =
    "[$depth] "
