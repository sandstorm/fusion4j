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

import io.neos.fusion4j.lang.antlr.AfxParser
import io.neos.fusion4j.lang.model.FusionPathNameSegment
import io.neos.fusion4j.lang.model.decl.AstReference
import io.neos.fusion4j.lang.model.decl.AstReference.Companion.codeOffset
import io.neos.fusion4j.lang.model.decl.FusionLangElementIdentifier
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token

/**
 * Adapter for translating antlr AST code information from the AFX parser to [AstReference]s.
 */
class AfxAstReferences(
    private val rootAstReference: AstReference
) {

    fun rootJoinSimpleName(
        afxValues: List<AfxValue>,
        simpleNameIdentifier: FusionLangElementIdentifier
    ): AstReference =
        codeOffset(
            "AFX root join simple name",
            afxValues.flatMap { it.parseResult }.joinToString("") { it.text },
            afxValues.first().parseResult.first().start.toCodePosition(),
            afxValues.last().parseResult.last().stop.toCodePosition(),
            simpleNameIdentifier,
            rootAstReference
        )

    fun rootJoinNamespace(
        afxValues: List<AfxValue>,
        namespaceIdentifier: FusionLangElementIdentifier
    ): AstReference =
        codeOffset(
            "AFX root join namespace",
            afxValues.flatMap { it.parseResult }.joinToString("") { it.text },
            afxValues.first().parseResult.first().start.toCodePosition(),
            afxValues.last().parseResult.last().stop.toCodePosition(),
            namespaceIdentifier,
            rootAstReference
        )

    fun rootJoinName(
        afxValues: List<AfxValue>,
        nameIdentifier: FusionLangElementIdentifier
    ): AstReference =
        codeOffset(
            "AFX root join qualified name",
            afxValues.flatMap { it.parseResult }.joinToString("") { it.text },
            afxValues.first().parseResult.first().start.toCodePosition(),
            afxValues.last().parseResult.last().stop.toCodePosition(),
            nameIdentifier,
            rootAstReference
        )

    fun rootJoinValue(
        afxValues: List<AfxValue>,
        nameIdentifier: FusionLangElementIdentifier
    ): AstReference =
        codeOffset(
            "AFX root join value",
            afxValues.flatMap { it.parseResult }.joinToString("") { it.text },
            afxValues.first().parseResult.first().start.toCodePosition(),
            afxValues.last().parseResult.last().stop.toCodePosition(),
            nameIdentifier,
            rootAstReference
        )

    fun rootJoin(
        afxValues: List<AfxValue>,
        nameIdentifier: FusionLangElementIdentifier
    ): AstReference =
        codeOffset(
            "AFX root join",
            afxValues.flatMap { it.parseResult }.joinToString("") { it.text },
            afxValues.first().parseResult.first().start.toCodePosition(),
            afxValues.last().parseResult.last().stop.toCodePosition(),
            nameIdentifier,
            rootAstReference
        )

    fun bodyExpression(
        parseTree: ParserRuleContext,
        expressionIdentifier: FusionLangElementIdentifier
    ): AstReference =
        codeOffset(
            "AFX body expression",
            parseTree.text,
            parseTree.start.toCodePosition(),
            parseTree.stop.toCodePosition(),
            expressionIdentifier,
            rootAstReference
        )

    fun bodyExpressionValue(
        parseTree: ParserRuleContext,
        expressionIdentifier: FusionLangElementIdentifier
    ): AstReference =
        codeOffset(
            "AFX body expression value",
            parseTree.text,
            parseTree.start.toCodePosition(),
            parseTree.stop.toCodePosition(),
            expressionIdentifier,
            rootAstReference
        )

    fun tagAttributeExpressionValue(
        parseTree: ParserRuleContext,
        expressionIdentifier: FusionLangElementIdentifier
    ): AstReference {
        return codeOffset(
            "AFX tag attribute expression value",
            parseTree.text,
            parseTree.start.toCodePosition(1),
            parseTree.stop.toCodePosition(),
            expressionIdentifier,
            rootAstReference
        )
    }

    fun tagAttributeExpression(
        parseTree: ParserRuleContext,
        expressionIdentifier: FusionLangElementIdentifier
    ): AstReference {
        return codeOffset(
            "AFX tag attribute expression",
            parseTree.text,
            parseTree.start.toCodePosition(),
            parseTree.stop.toCodePosition(),
            expressionIdentifier,
            rootAstReference
        )
    }

    fun htmlTag(
        ctx: AfxParser.TagStartContext,
        tagObjectIdentifier: FusionLangElementIdentifier
    ): AstReference =
        codeOffset(
            "AFX HTML tag",
            ctx.text,
            ctx.start.toCodePosition(),
            ctx.stop.toCodePosition(),
            tagObjectIdentifier,
            rootAstReference
        )

    fun stringValue(
        afxString: AfxString,
        identifier: FusionLangElementIdentifier
    ): AstReference {
        return codeOffset(
            "AFX string value",
            afxString.parseResult.joinToString("") { it.text },
            afxString.parseResult.first().start.toCodePosition(),
            afxString.parseResult.last().stop.toCodePosition(),
            identifier,
            rootAstReference
        )
    }

    fun string(
        afxString: AfxString,
        identifier: FusionLangElementIdentifier
    ): AstReference =
        codeOffset(
            "AFX string",
            afxString.parseResult.joinToString("") { it.text },
            afxString.parseResult.first().start.toCodePosition(),
            afxString.parseResult.last().stop.toCodePosition(),
            identifier,
            rootAstReference
        )

    fun booleanValue(
        afxBoolean: AfxBoolean,
        identifier: FusionLangElementIdentifier
    ): AstReference {
        return codeOffset(
            "AFX boolean value",
            afxBoolean.parseResult.joinToString("") { it.text },
            afxBoolean.parseResult.first().start.toCodePosition(),
            afxBoolean.parseResult.last().stop.toCodePosition(),
            identifier,
            rootAstReference
        )
    }

    fun boolean(
        afxBoolean: AfxBoolean,
        identifier: FusionLangElementIdentifier
    ): AstReference =
        codeOffset(
            "AFX boolean",
            afxBoolean.parseResult.joinToString("") { it.text },
            afxBoolean.parseResult.first().start.toCodePosition(),
            afxBoolean.parseResult.last().stop.toCodePosition(),
            identifier,
            rootAstReference
        )

    fun objectSimpleName(
        afxFusionObject: AfxFusionObject,
        identifier: FusionLangElementIdentifier
    ): AstReference =
        codeOffset(
            "AFX object simple name",
            afxFusionObject.parseResult.joinToString("") { it.text },
            afxFusionObject.parseResult.first().start.toCodePosition(),
            afxFusionObject.parseResult.last().stop.toCodePosition(),
            identifier,
            rootAstReference
        )

    fun objectNamespace(
        afxFusionObject: AfxFusionObject,
        identifier: FusionLangElementIdentifier
    ): AstReference =
        codeOffset(
            "AFX object namespace",
            afxFusionObject.parseResult.joinToString("") { it.text },
            afxFusionObject.parseResult.first().start.toCodePosition(),
            afxFusionObject.parseResult.last().stop.toCodePosition(),
            identifier,
            rootAstReference
        )

    fun objectName(
        afxFusionObject: AfxFusionObject,
        identifier: FusionLangElementIdentifier
    ): AstReference =
        codeOffset(
            "AFX object qualified name",
            afxFusionObject.parseResult.joinToString("") { it.text },
            afxFusionObject.parseResult.first().start.toCodePosition(),
            afxFusionObject.parseResult.last().stop.toCodePosition(),
            identifier,
            rootAstReference
        )

    fun objectDecl(
        afxFusionObject: AfxFusionObject,
        identifier: FusionLangElementIdentifier
    ): AstReference =
        codeOffset(
            "AFX object",
            afxFusionObject.parseResult.joinToString("") { it.text },
            afxFusionObject.parseResult.first().start.toCodePosition(),
            afxFusionObject.parseResult.last().stop.toCodePosition(),
            identifier,
            rootAstReference
        )

    fun objectValue(
        afxFusionObject: AfxFusionObject,
        identifier: FusionLangElementIdentifier
    ): AstReference =
        codeOffset(
            "AFX object value",
            afxFusionObject.parseResult.joinToString("") { it.text },
            afxFusionObject.parseResult.first().start.toCodePosition(),
            afxFusionObject.parseResult.last().stop.toCodePosition(),
            identifier,
            rootAstReference
        )

    fun assignmentPathSegment(
        afxAssignment: AfxAssignment,
        segment: FusionPathNameSegment,
        identifier: FusionLangElementIdentifier
    ): AstReference =
        codeOffset(
            "AFX assignment path segment",
            segment.segmentAsString,
            afxAssignment.parseResult.first().start.toCodePosition(),
            afxAssignment.parseResult.last().stop.toCodePosition(),
            identifier,
            rootAstReference
        )

    fun assignmentPath(
        afxAssignment: AfxAssignment,
        identifier: FusionLangElementIdentifier
    ): AstReference =
        codeOffset(
            "AFX assignment path",
            afxAssignment.path.toString(),
            afxAssignment.path.parseResult.start.toCodePosition(),
            afxAssignment.path.parseResult.stop.toCodePosition(),
            identifier,
            rootAstReference
        )

    fun assignment(
        afxAssignment: AfxAssignment,
        identifier: FusionLangElementIdentifier
    ): AstReference =
        codeOffset(
            "AFX assignment",
            afxAssignment.parseResult.joinToString("") { it.text },
            afxAssignment.parseResult.first().start.toCodePosition(),
            afxAssignment.parseResult.last().stop.toCodePosition(),
            identifier,
            rootAstReference
        )

    fun body(
        bodyIdentifier: FusionLangElementIdentifier,
        assignments: List<AfxAssignment>
    ): AstReference = codeOffset(
        "AFX object body",
        assignments.flatMap { it.parseResult }.joinToString("") { it.text },
        assignments.first().parseResult.first().start.toCodePosition(),
        assignments.last().parseResult.last().stop.toCodePosition(),
        bodyIdentifier,
        rootAstReference
    )

}

private fun Token.toCodePosition(offset: Int = 0) = AstReference.CodePosition(
    line = line,
    charPositionInLine = charPositionInLine + offset
)
