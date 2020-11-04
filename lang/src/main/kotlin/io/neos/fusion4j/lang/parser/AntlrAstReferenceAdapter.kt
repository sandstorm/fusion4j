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

import io.neos.fusion4j.lang.antlr.FusionLexer
import io.neos.fusion4j.lang.antlr.FusionParser
import io.neos.fusion4j.lang.model.decl.AstReference
import io.neos.fusion4j.lang.model.decl.FusionLangElementIdentifier
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.TerminalNode

/**
 * This is just a marker for the whole file.
 */
internal class AntlrAstReferenceAdapter private constructor()

fun FusionParser.FusionFileContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "fusion file",
        code = text,
        startPosition = start.toCodePosition(),
        endPosition = stop.toEndCodePositionMultiLine()
    )

fun FusionParser.FusionConfigurationContext.toAstReferenceBody(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "path configuration body",
        code = fusionConfigurationBody().text,
        startPosition = fusionConfigurationBody().start.toCodePosition(false),
        endPosition = fusionConfigurationBody().FUSION_BODY_END().symbol.toEndCodePositionMultiLine()
    )

fun FusionParser.RootFusionConfigurationContext.toAstReferenceBody(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "root path configuration body",
        code = rootFusionConfigurationBody().text,
        startPosition = rootFusionConfigurationBody().start.toCodePosition(false),
        endPosition = rootFusionConfigurationBody().FUSION_BODY_END().symbol.toEndCodePositionMultiLine()
    )

fun FusionParser.PrototypeBodyContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "prototype declaration body",
        code = text,
        startPosition = start.toCodePosition(false),
        endPosition = FUSION_BODY_END().symbol.toEndCodePositionMultiLine()
    )

fun FusionParser.FusionValueBodyContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "value body",
        code = text,
        startPosition = FUSION_BODY_START().symbol.toCodePosition(false),
        endPosition = FUSION_BODY_END().symbol.toEndCodePositionMultiLine()
    )

fun FusionParser.RootFusionValueBodyContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "root value body",
        code = text,
        startPosition = ROOT_FUSION_BODY_START().symbol.toCodePosition(false),
        endPosition = FUSION_BODY_END().symbol.toEndCodePositionMultiLine()
    )

fun FusionParser.RootFusionConfigurationCopyContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "root path configuration copy",
        code = text,
        startPosition = start.toCodePosition(),
        endPosition = stop.toEndCodePositionMultiLine()
    )

fun FusionParser.FusionConfigurationCopyContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "path configuration copy",
        code = text,
        startPosition = start.toCodePosition(),
        endPosition = stop.toEndCodePositionMultiLine()
    )

fun FusionParser.RootFusionConfigurationCopyContext.toAstReferenceFusionPath(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "root path configuration copy path",
        code = rootFusionConfigurationPath().text,
        startPosition = rootFusionConfigurationPath().start.toCodePosition(),
        endPosition = rootFusionConfigurationPath().stop.toEndCodePositionMultiLine()
    )

fun FusionParser.FusionConfigurationCopyContext.toAstReferenceFusionPath(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "path configuration copy path",
        code = fusionConfigurationPath().text,
        startPosition = fusionConfigurationPath().start.toCodePosition(),
        endPosition = fusionConfigurationPath().stop.toEndCodePositionMultiLine()
    )

fun FusionParser.RootFusionConfigurationPathReferenceContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "root path configuration copy referenced path",
        code = text,
        startPosition = start.toCodePosition(),
        endPosition = stop.toEndCodePositionMultiLine()
    )

fun FusionParser.FusionConfigurationPathReferenceContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "path configuration copy referenced path",
        code = text,
        startPosition = start.toCodePosition(),
        endPosition = stop.toEndCodePositionMultiLine()
    )

fun FusionParser.RootPrototypeErasureContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "root prototype erasure",
        code = text,
        startPosition = start.toCodePosition(),
        endPosition = ROOT_FUSION_ERASURE().symbol.toEndCodePositionSingleLine()
    )

fun FusionParser.RootFusionConfigurationErasureContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "root configuration erasure",
        code = text,
        startPosition = start.toCodePosition(),
        endPosition = ROOT_FUSION_ERASURE().symbol.toEndCodePositionSingleLine()
    )

fun FusionParser.FusionConfigurationErasureContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "configuration erasure",
        code = text,
        startPosition = start.toCodePosition(),
        endPosition = FUSION_ERASURE().symbol.toEndCodePositionSingleLine()
    )

fun FusionParser.CodeCommentContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference {
    val comment = CODE_COMMENT() ?: ROOT_CODE_COMMENT()
    return AstReference(
        elementIdentifier = elementIdentifier,
        description = "code comment",
        code = text,
        startPosition = comment.symbol.toCodePosition(),
        endPosition = comment.symbol.toEndCodePositionMultiLine()
    )
}

fun FusionParser.RootPrototypeDeclContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "prototype declaration",
        code = text,
        startPosition = start.toCodePosition(),
        endPosition = stop.toEndCodePositionMultiLine()
    )

fun FusionParser.FusionConfigurationContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "path configuration",
        code = text,
        startPosition = start.toCodePosition(),
        endPosition = fusionConfigurationBody().FUSION_BODY_END().symbol.toEndCodePositionMultiLine()
    )

fun FusionParser.RootFusionConfigurationContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "root path configuration",
        code = text,
        startPosition = start.toCodePosition(),
        endPosition = rootFusionConfigurationBody().FUSION_BODY_END().symbol.toEndCodePositionMultiLine()
    )

fun FusionParser.FusionAssignContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    astReferenceForAssignment(
        elementIdentifier,
        this,
        "root path assignment"
    )

fun FusionParser.RootFusionAssignContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    astReferenceForAssignment(
        elementIdentifier,
        this,
        "path assignment"
    )

private fun astReferenceForAssignment(
    elementIdentifier: FusionLangElementIdentifier,
    context: ParserRuleContext,
    description: String
): AstReference = AstReference(
    elementIdentifier = elementIdentifier,
    description = description,
    code = context.text,
    startPosition = context.start.toCodePosition(),
    endPosition = context.stop.toEndCodePositionMultiLine()
)

fun FusionParser.FusionValueNullContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "null value",
        code = FUSION_VALUE_LITERAL_NULL().text,
        startPosition = FUSION_VALUE_LITERAL_NULL().symbol.toCodePosition(),
        endPosition = FUSION_VALUE_LITERAL_NULL().symbol.toEndCodePositionSingleLine()
    )

fun FusionParser.FusionValueBooleanContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "boolean value",
        code = FUSION_VALUE_BOOLEAN().text,
        startPosition = FUSION_VALUE_BOOLEAN().symbol.toCodePosition(),
        endPosition = FUSION_VALUE_BOOLEAN().symbol.toEndCodePositionSingleLine()
    )

fun FusionParser.FusionValueNumberContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "number value",
        code = FUSION_VALUE_NUMBER().text,
        startPosition = FUSION_VALUE_NUMBER().symbol.toCodePosition(),
        endPosition = FUSION_VALUE_NUMBER().symbol.toEndCodePositionSingleLine()
    )

fun FusionParser.FusionValueDslDelegateContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "number value",
        code = FUSION_VALUE_DSL_DELEGATE().text,
        startPosition = FUSION_VALUE_DSL_DELEGATE().symbol.toCodePosition(),
        endPosition = FUSION_VALUE_DSL_DELEGATE().symbol.toEndCodePositionMultiLine()
    )

fun FusionParser.FusionValueObjectContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "fusion object",
        code = text,
        startPosition = start.toCodePosition(),
        endPosition = (getChild(0) as TerminalNode).symbol.toEndCodePositionMultiLine()
    )

fun FusionParser.FusionValueStringSingleQuoteContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "string value (single quoted)",
        code = FUSION_VALUE_STRING_SQUOTE().text,
        startPosition = FUSION_VALUE_STRING_SQUOTE().symbol.toCodePosition(),
        endPosition = FUSION_VALUE_STRING_SQUOTE().symbol.toEndCodePositionSingleLine()
    )

fun FusionParser.FusionValueStringDoubleQuoteContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "string value (double quoted)",
        code = FUSION_VALUE_STRING_DQUOTE().text,
        startPosition = FUSION_VALUE_STRING_DQUOTE().symbol.toCodePosition(),
        endPosition = FUSION_VALUE_STRING_DQUOTE().symbol.toEndCodePositionSingleLine()
    )

fun FusionParser.FusionValueExpressionContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "expression value",
        code = text,
        startPosition = start.toCodePosition(),
        endPosition = stop.toEndCodePositionSingleLine()
    )

fun FusionParser.RootPrototypeErasureContext.toAstReferenceFusionPath(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "root prototype erasure path",
        code = rootPrototypeCall().text,
        startPosition = rootPrototypeCall().start.toCodePosition(),
        endPosition = rootPrototypeCall().stop.toEndCodePositionSingleLine()
    )

fun FusionParser.RootFusionConfigurationErasureContext.toAstReferenceFusionPath(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "root configuration erasure path",
        code = rootFusionConfigurationPath().text,
        startPosition = rootFusionConfigurationPath().start.toCodePosition(),
        endPosition = rootFusionConfigurationPath().stop.toEndCodePositionSingleLine()
    )

fun FusionParser.FusionConfigurationErasureContext.toAstReferenceFusionPath(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "configuration erasure path",
        code = fusionConfigurationPath().text,
        startPosition = fusionConfigurationPath().start.toCodePosition(),
        endPosition = fusionConfigurationPath().stop.toEndCodePositionSingleLine()
    )

fun FusionParser.RootFusionConfigurationContext.toAstReferenceFusionPath(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "root path configuration",
        code = rootFusionConfigurationPath().text,
        startPosition = rootFusionConfigurationPath().start.toCodePosition(),
        endPosition = rootFusionConfigurationPath().stop.toEndCodePositionSingleLine()
    )

fun FusionParser.FusionConfigurationContext.toAstReferenceFusionPath(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "path configuration",
        code = fusionConfigurationPath().text,
        startPosition = fusionConfigurationPath().start.toCodePosition(),
        endPosition = fusionConfigurationPath().stop.toEndCodePositionSingleLine()
    )

fun FusionParser.RootFusionAssignContext.toAstReferenceFusionPath(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "root assignment path",
        code = rootFusionAssignPath().text,
        startPosition = rootFusionAssignPath().start.toCodePosition(),
        endPosition = rootFusionAssignPath().stop.toEndCodePositionSingleLine()
    )

fun FusionParser.FusionAssignContext.toAstReferenceFusionPath(elementIdentifier: FusionLangElementIdentifier): AstReference {
    val pathContext = fusionAssignPath()
    return AstReference(
        elementIdentifier = elementIdentifier,
        description = "assignment path",
        code = pathContext.text,
        startPosition = pathContext.start.toCodePosition(),
        endPosition = pathContext.stop.toEndCodePositionSingleLine()
    )
}

fun FusionParser.PrototypeCallContext.toAstReferencePathNameSegment(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "path name segment (prototype call)",
        code = text,
        startPosition = start.toCodePosition(),
        endPosition = stop.toEndCodePositionSingleLine()
    )

fun FusionParser.RootPrototypeCallContext.toAstReferencePathNameSegment(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "path name segment (root prototype call)",
        code = text,
        startPosition = start.toCodePosition(),
        endPosition = stop.toEndCodePositionSingleLine()
    )

fun FusionParser.FusionMetaPropPathSegmentContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "path name segment (meta property)",
        code = text,
        startPosition = start.toCodePosition(),
        endPosition = stop.toEndCodePositionSingleLine()
    )

fun FusionParser.RootFusionMetaPropPathSegmentContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "path name segment (root meta property)",
        code = text,
        startPosition = start.toCodePosition(),
        endPosition = stop.toEndCodePositionSingleLine()
    )

fun FusionParser.FusionPathSegmentContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "path name segment",
        code = text,
        startPosition = start.toCodePosition(),
        endPosition = stop.toEndCodePositionSingleLine()
    )

fun FusionParser.RootFusionPathSegmentContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "path name segment (root)",
        code = text,
        startPosition = start.toCodePosition(),
        endPosition = stop.toEndCodePositionSingleLine()
    )

fun FusionParser.NamespaceAliasContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "namespace alias",
        code = text,
        startPosition = start.toCodePosition(),
        endPosition = stop.toEndCodePositionSingleLine()
    )

fun TerminalNode.toAstReferenceNamespaceNameAliasSource(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "namespace name (alias source)",
        code = text,
        startPosition = symbol.toCodePosition(),
        endPosition = symbol.toEndCodePositionSingleLine()
    )

fun TerminalNode.toAstReferenceNamespaceNameAliasTarget(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "namespace name (alias target)",
        code = text,
        startPosition = symbol.toCodePosition(),
        endPosition = symbol.toEndCodePositionSingleLine()
    )

fun TerminalNode.toAstReferenceSimplePrototypeName(elementIdentifier: FusionLangElementIdentifier): AstReference {
    val textSanitized = text.trim()
    return AstReference(
        elementIdentifier = elementIdentifier,
        description = "simple prototype name",
        code = if (textSanitized.contains(':'))
            textSanitized.substring(textSanitized.indexOf(':') + 1 until textSanitized.length)
        else
            textSanitized,
        startPosition = symbol.toCodePositionFromFirstChar(':'),
        endPosition = symbol.toEndCodePositionSingleLine()
    )
}

fun TerminalNode.toAstReferencePrototypeNamespace(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "prototype namespace",
        code = text.substring(0 until text.indexOf(':')),
        startPosition = symbol.toCodePosition(),
        endPosition = symbol.toEndCodePositionSingleLineUntilChar(':')
    )

fun TerminalNode.toAstReferenceQualifiedPrototypeName(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "qualified prototype name",
        code = text,
        startPosition = symbol.toCodePosition(),
        endPosition = symbol.toEndCodePositionSingleLine()
    )

fun TerminalNode.toAstReferenceFileIncludePattern(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "file include pattern",
        code = text,
        startPosition = symbol.toCodePosition(),
        endPosition = symbol.toEndCodePositionSingleLine()
    )

fun FusionParser.FileIncludeContext.toAstReference(elementIdentifier: FusionLangElementIdentifier): AstReference =
    AstReference(
        elementIdentifier = elementIdentifier,
        description = "file include",
        code = text,
        startPosition = start.toCodePosition(),
        endPosition = stop.toEndCodePositionSingleLine()
    )

// ---------- helpers

private fun Token.toCodePosition(ignoreLeadingWhitespaces: Boolean = true) = AstReference.CodePosition(
    line = line,
    charPositionInLine = charPositionInLine + 1 + if (ignoreLeadingWhitespaces)
        0
    else
        text.length - text.trimStart().length
)

private fun Token.toCodePositionFromFirstChar(char: Char) = AstReference.CodePosition(
    line = line,
    charPositionInLine = charPositionInLine + 1 + text.trim().indexOf(char)
)

private fun Token.toEndCodePositionSingleLine() = AstReference.CodePosition(
    line = line,
    charPositionInLine = charPositionInLine + 1 + when (type) {
        FusionLexer.EOF -> 0
        else -> text.trimEnd().length
    }
)

private fun Token.toEndCodePositionSingleLineUntilChar(char: Char): AstReference.CodePosition {
    val textSanitized = text.trim()
    return AstReference.CodePosition(
        line = line,
        charPositionInLine = charPositionInLine + 1 + when (type) {
            FusionLexer.EOF -> 0
            else -> textSanitized.substring(0 until textSanitized.indexOf(char)).trimEnd().length
        }
    )
}

private fun Token.toEndCodePositionMultiLine() = with(text.replace("\n$".toRegex(), "")) {
    AstReference.CodePosition(
        line = line + count { it == '\n' },
        charPositionInLine = 1 + when (type) {
            FusionLexer.EOF -> charPositionInLine
            else -> if (contains('\n'))
                subSequence(lastIndexOf('\n') + 1 until length).length
            else
                charPositionInLine + length
        }
    )
}