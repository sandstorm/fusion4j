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

package io.neos.fusion4j.lang.model.decl

/**
 * This is the abstraction layer for "element to source code" syntax tree references.
 * LoC, source code abstract, token start/end position etc.
 *
 * The file/package name is stored in the [FusionLangElementIdentifier].
 *
 * The usage of AST references is a way to produce very verbose error messages that makes
 * bug-hunting easier in your Fusion code.
 *
 * The current parser implementation with antlr has an adapter for translating internal antlr
 * code information to the public [AstReference] API.
 * @see io.neos.fusion4j.lang.parser.AntlrAstReferenceAdapter
 *
 * For AFX, there is a separate adapter.
 * @see io.neos.fusion4j.lang.parser.afx.AfxAstReferences
 *
 */
data class AstReference(
    val description: String,
    val code: String,
    val startPosition: CodePosition,
    val endPosition: CodePosition,
    val elementIdentifier: FusionLangElementIdentifier
) {

    data class CodePosition(
        val line: Int,
        val charPositionInLine: Int
    ) {
        override fun toString(): String {
            return "line $line char $charPositionInLine"
        }
    }

    companion object {
        fun codeOffset(
            description: String,
            code: String,
            startPosition: CodePosition,
            endPosition: CodePosition,
            elementIdentifier: FusionLangElementIdentifier,
            offsetReference: AstReference
        ): AstReference =
            AstReference(
                description,
                code,
                CodePosition(
                    startPosition.line + offsetReference.startPosition.line,
                    startPosition.charPositionInLine + 1 +
                            if (offsetReference.startPosition.line == 0)
                                offsetReference.startPosition.charPositionInLine
                            else 0
                ),
                CodePosition(
                    endPosition.line + offsetReference.endPosition.line,
                    endPosition.charPositionInLine + 1 +
                            if (offsetReference.endPosition.line == 0)
                                offsetReference.startPosition.charPositionInLine
                            else 0
                ),
                elementIdentifier
            )
    }

    override fun toString(): String = "$description, from $startPosition to $endPosition"
}