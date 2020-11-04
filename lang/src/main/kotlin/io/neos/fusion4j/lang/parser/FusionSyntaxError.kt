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

package io.neos.fusion4j.lang.parser

import io.neos.fusion4j.lang.model.decl.AstReference
import io.neos.fusion4j.lang.util.toReadableSyntaxErrorOutput

data class FusionSyntaxError(
    val codePosition: AstReference.CodePosition,
    val errorType: ErrorType,
    val message: String,
    val offendingSymbol: OffendingSymbol?,
    val parserClass: Class<*>,
    val cause: Throwable?
) {

    fun getReadableDescription() = toReadableSyntaxErrorOutput(this)

    enum class ErrorType {
        LEXER,
        PARSER,
        MODEL
    }

    data class OffendingSymbol(
        val text: String,
        val codePosition: AstReference.CodePosition
    )
}


