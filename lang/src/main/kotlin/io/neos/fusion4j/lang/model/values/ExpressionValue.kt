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

package io.neos.fusion4j.lang.model.values

import io.neos.fusion4j.lang.model.decl.AstReference
import kotlin.math.max
import kotlin.math.min

class ExpressionValue(
    val eelExpression: String,
    override val astReference: AstReference
) : FusionValue {

    companion object {
        val SYMBOL_CHARS: Set<Char> =
            CharRange('a', 'z').toSet() +
                    CharRange('A', 'Z').toSet() +
                    CharRange('0', '9').toSet() +
                    setOf('.', '_')
    }

    override fun getReadableType(): String = "[EEL]"
    override fun getReadableValue(): String = "\${$eelExpression}"
    override fun toString(): String = getReadableValue()

    fun getOffendingSymbol(startPosition: Int): String {
        val restExpression = if (startPosition > 0 && startPosition < eelExpression.length) {
            eelExpression.substring(startPosition - 1)
        } else {
            eelExpression
        }
        val indexOfFirstNonSymbolChar = restExpression.indexOfFirst { !SYMBOL_CHARS.contains(it) }
        return if (indexOfFirstNonSymbolChar < 1) {
            restExpression
        } else {
            restExpression.substring(0, indexOfFirstNonSymbolChar)
        }
    }

    fun getOffendingExpressionAround(
        startPosition: Int,
        numberOfLeadingContextChars: Int = 4,
        numberOfTrailingContextChars: Int = 20
    ): String {
        if (startPosition < 0 || startPosition > eelExpression.length) {
            throw IllegalArgumentException(
                "Could not get offending expression, " +
                        "illegal starting position $startPosition; must be between 0 and ${eelExpression.length} "
            )
        }
        val leadingPart = eelExpression.substring(
            max(0, startPosition - numberOfLeadingContextChars),
            max(0, startPosition - 1)
        )
        val problemPart = eelExpression.substring(
            max(0, startPosition - 1),
            min(eelExpression.length, startPosition - 1 + numberOfTrailingContextChars)
        )
        return "($leadingPart)$problemPart ..."
    }
}