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

package io.neos.fusion4j.lang.parser.afx

import io.neos.fusion4j.lang.FusionErrorListener
import io.neos.fusion4j.lang.antlr.AfxLexer
import io.neos.fusion4j.lang.antlr.AfxParser
import io.neos.fusion4j.lang.model.AbsoluteFusionPathName
import io.neos.fusion4j.lang.model.DslName
import io.neos.fusion4j.lang.model.decl.FusionValueDecl
import io.neos.fusion4j.lang.model.decl.values.DslDelegateValueDecl
import io.neos.fusion4j.lang.parser.DslParser
import io.neos.fusion4j.lang.parser.FusionParseException
import io.neos.fusion4j.lang.parser.FusionSyntaxError
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream


class AfxDslParser : DslParser {

    override val dslName: DslName = AFX_DSL_NAME

    companion object {
        val AFX_DSL_NAME: DslName = DslName("afx")
    }

    override fun parse(dslValueDecl: DslDelegateValueDecl, currentPath: AbsoluteFusionPathName): FusionValueDecl {
        val lexerErrorListener = FusionErrorListener(FusionSyntaxError.ErrorType.LEXER)
        val parserErrorListener = FusionErrorListener(FusionSyntaxError.ErrorType.PARSER)

        val charStream = CharStreams.fromString(dslValueDecl.fusionValue.code)
        val lexer = AfxLexer(charStream)
        lexer.removeErrorListeners()
        lexer.addErrorListener(lexerErrorListener)

        val tokenStream = CommonTokenStream(lexer)
        val parser = AfxParser(tokenStream)
        parser.removeErrorListeners()
        parser.addErrorListener(parserErrorListener)

        val internalModel = parser.afxCode()

        val allErrors = lexerErrorListener.errors + parserErrorListener.errors
        if (allErrors.isNotEmpty()) {
            throw FusionParseException(dslValueDecl.sourceIdentifier, dslValueDecl.fusionValue.code, allErrors)
        }

        // do the transpiling
        val transpiler = AfxTranspiler(dslValueDecl, currentPath, internalModel)

        return transpiler.transpile()
    }

}