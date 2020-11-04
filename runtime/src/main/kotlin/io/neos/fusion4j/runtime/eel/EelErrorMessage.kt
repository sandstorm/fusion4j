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

package io.neos.fusion4j.runtime.eel

import io.neos.fusion4j.lang.file.FusionSourceFileIdentifier
import io.neos.fusion4j.lang.model.decl.FusionLangElementIdentifier
import io.neos.fusion4j.lang.model.values.ExpressionValue
import java.lang.IllegalArgumentException

class EelErrorMessage(
    expressionValue: ExpressionValue,
    val offendingExpression: String,
    val offendingLine: Int,
    val offendingCharPositionInLine: Int,
    val problemDescription: ProblemDescription
) {
    val expression: String = "\${${expressionValue.eelExpression}}"
    val astReference = expressionValue.astReference
    val elementIdentifier: FusionLangElementIdentifier = astReference.elementIdentifier
    val fusionFile: FusionSourceFileIdentifier = elementIdentifier.fusionFile

    val readable: String
        get() =
            """
            ${problemDescription.rootCauseMessage}
            source: $fusionFile
            hints: ${fusionFile.buildHintMessage(astReference)}
            expression: $expression
            offending: '$offendingExpression' at line $offendingLine char $offendingCharPositionInLine
            problem: ${problemDescription.fullMessage}
            element: $elementIdentifier
            code: $astReference
            """.trimIndent()

    override fun toString(): String = readable

    data class ProblemDescription(
        val errorMessages: List<String>
    ) {
        init {
            if (errorMessages.isEmpty()) {
                throw IllegalArgumentException("Problem description messages must not be empty")
            }
        }

        val rootCauseMessage: String = errorMessages.last()
        val fullMessage: String = errorMessages.joinToString(" -> ")
    }

}