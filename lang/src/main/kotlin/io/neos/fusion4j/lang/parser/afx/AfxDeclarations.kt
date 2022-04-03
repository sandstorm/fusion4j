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
import io.neos.fusion4j.lang.model.*
import io.neos.fusion4j.lang.model.decl.*
import io.neos.fusion4j.lang.model.decl.values.*
import io.neos.fusion4j.lang.model.values.*
import org.antlr.v4.runtime.ParserRuleContext
import kotlin.math.max
import kotlin.math.min

class AfxDeclarations(
    private val afxDeclaration: DslDelegateValueDecl,
    private val currentPath: AbsoluteFusionPathName
) {

    private val afxAstReferences = AfxAstReferences(afxDeclaration.astReference)

    fun rootNullValue(): NullValueDecl = NullValueDecl(
        afxDeclaration.elementIdentifier,
        afxDeclaration.parentElementIdentifier,
        NullValue(
            // TODO virtual AST reference as well?
            afxDeclaration.astReference
        ),
        afxDeclaration.body,
        afxDeclaration.sourceIdentifier,
        afxDeclaration.astReference
    )

    fun rootValue(afxValue: AfxValue): FusionValueDecl {
        return transpileValue(currentPath, afxValue, afxDeclaration.elementIdentifier)
    }

    fun rootJoin(afxValues: List<AfxValue>): FusionObjectValueDecl {
        val nameIdentifier = afxDeclaration.elementIdentifier.appendType("afx-join-name")
        val simpleNameIdentifier = nameIdentifier.appendType("afx-join-simple-name")
        val namespaceIdentifier = nameIdentifier.appendType("afx-join-namespace")
        return FusionObjectValueDecl(
            afxDeclaration.elementIdentifier,
            afxDeclaration.parentElementIdentifier,
            QualifiedPrototypeNameDecl(
                nameIdentifier,
                afxDeclaration.elementIdentifier,
                SimplePrototypeNameDecl(
                    simpleNameIdentifier,
                    nameIdentifier,
                    AfxFusionApi.JOIN_FUSION_OBJECT_NAME.simpleName,
                    nameIdentifier.fusionFile,
                    afxAstReferences.rootJoinSimpleName(afxValues, simpleNameIdentifier)
                ),
                PrototypeNamespaceDecl(
                    nameIdentifier.appendType("afx-join-namespace"),
                    nameIdentifier,
                    AfxFusionApi.JOIN_FUSION_OBJECT_NAME.namespace,
                    nameIdentifier.fusionFile,
                    afxAstReferences.rootJoinNamespace(afxValues, namespaceIdentifier)
                ),
                nameIdentifier.fusionFile,
                afxAstReferences.rootJoinName(afxValues, nameIdentifier)
            ),
            FusionObjectValue(
                AfxFusionApi.JOIN_FUSION_OBJECT_NAME,
                afxAstReferences.rootJoinValue(afxValues, nameIdentifier)
            ),
            buildBody(
                afxDeclaration.elementIdentifier,
                currentPath,
                afxValues.mapIndexed { idx, value ->
                    val valuePath = joinElementKeyName(idx, value)
                    AfxAssignment(
                        AfxPath(valuePath, value.parseResult.first()),
                        value,
                        value.parseResult
                    )
                }
            ),
            afxDeclaration.sourceIdentifier,
            afxAstReferences.rootJoin(afxValues, nameIdentifier)
        )
    }

    private fun transpileValue(
        path: AbsoluteFusionPathName,
        afxValue: AfxValue,
        parentIdentifier: FusionLangElementIdentifier
    ): FusionValueDecl {
        return when (afxValue) {
            is AfxString -> transpileString(parentIdentifier, afxValue)
            is AfxBoolean -> transpileBoolean(parentIdentifier, afxValue)
            is AfxExpressionValue -> transpileExpression(parentIdentifier, afxValue)
            is AfxFusionObject -> transpileFusionObject(path, parentIdentifier, afxValue)
            else -> throw AfxParserError("Unhandled AFX value $afxValue")
        }
    }

    private fun transpileString(parentIdentifier: FusionLangElementIdentifier, afxValue: AfxString): StringValueDecl {
        val identifier = identifierForValueString(parentIdentifier)
        return StringValueDecl(
            identifier,
            parentIdentifier,
            StringValue.fromRawString(
                afxValue.content,
                afxAstReferences.stringValue(afxValue, identifier)
            ),
            null,
            parentIdentifier.fusionFile,
            afxAstReferences.string(afxValue, identifier)
        )
    }

    private fun transpileBoolean(
        parentIdentifier: FusionLangElementIdentifier,
        afxValue: AfxBoolean
    ): BooleanValueDecl {
        val identifier = identifierForValueBoolean(parentIdentifier)
        return BooleanValueDecl(
            identifier,
            parentIdentifier,
            BooleanValue(
                afxValue.value,
                afxAstReferences.booleanValue(afxValue, identifier)
            ),
            null,
            parentIdentifier.fusionFile,
            afxAstReferences.boolean(afxValue, identifier)
        )
    }

    private fun transpileExpression(
        parentIdentifier: FusionLangElementIdentifier,
        afxValue: AfxExpressionValue
    ): ExpressionValueDecl {
        val identifier = identifierForValueExpression(parentIdentifier)
        val singleParseResult = afxValue.singleParseResult
        val valueAst = when (singleParseResult) {
            is AfxParser.HtmlAttributeContext -> afxAstReferences.tagAttributeExpressionValue(singleParseResult.htmlAttributeValue(), identifier)
            is AfxParser.TagAttributeExpressionValueContext -> afxAstReferences.tagAttributeExpressionValue(singleParseResult, identifier)
            else -> afxAstReferences.bodyExpressionValue(singleParseResult, identifier)
        }
        val bodyAst = when (singleParseResult) {
            is AfxParser.HtmlAttributeContext -> afxAstReferences.tagAttributeExpression(singleParseResult, identifier)
            is AfxParser.TagAttributeExpressionValueContext -> afxAstReferences.tagAttributeExpression(singleParseResult, identifier)
            else -> afxAstReferences.bodyExpression(singleParseResult, identifier)
        }
        return ExpressionValueDecl(
            identifier,
            parentIdentifier,
            ExpressionValue(
                afxValue.expressionString,
                valueAst
            ),
            null,
            parentIdentifier.fusionFile,
            bodyAst
        )
    }

    private fun transpileFusionObject(
        path: AbsoluteFusionPathName,
        parentIdentifier: FusionLangElementIdentifier,
        afxValue: AfxFusionObject
    ): FusionObjectValueDecl {
        val identifier = identifierForValueFusionObject(parentIdentifier, afxValue.prototypeName.qualifiedName)
        val nameIdentifier =
            identifierForQualifiedPrototypeName(identifier, afxValue.prototypeName.qualifiedName)
        val simpleNameIdentifier = nameIdentifier.appendType("afx-object-simple-name")
        val namespaceIdentifier = nameIdentifier.appendType("afx-object-namespace")
        val firstMeaningfulContentValueIdx =
            max(0, afxValue.contentValues.indexOfFirst { it !is AfxString || it.meaningful })
        val lastMeaningfulContentValueIdx =
            max(0, afxValue.contentValues.indexOfLast { it !is AfxString || it.meaningful })
        val contentValueCollector = afxValue.contentValues
            // filter non-meaningful whitespaces
            .subList(
                firstMeaningfulContentValueIdx,
                min(afxValue.contentValues.size, lastMeaningfulContentValueIdx + 1)
            )
            // separate content children that have an explicit @path, those are rendered directly
            // into the object body
            .fold(ContentValueCollector(), ContentValueCollector::collectValue)
        val contentValues = contentValueCollector.contentValues

        val contentAssignment: AfxAssignment? = when {
            // no content
            contentValues.isEmpty() -> null
            // single content
            contentValues.size == 1 -> {
                val singleContentValue = contentValues.single()

                if (singleContentValue is AfxFusionObject && singleContentValue.explicitKeyName != null) {
                    throw AfxParserError(
                        "explicit key name is forbidden for single nested " +
                                "content elements; parent: $this, child: $singleContentValue"
                    )
                }
                AfxAssignment(
                    AfxPath(getContentPath(afxValue), singleContentValue.parseResult.first()),
                    singleContentValue,
                    singleContentValue.parseResult
                )
            }
            // content join
            else -> {
                val contentPath = getContentPath(afxValue)
                AfxAssignment(
                    AfxPath(contentPath, contentValues.first().parseResult.first()),
                    AfxFusionObject(
                        AfxFusionApi.JOIN_FUSION_OBJECT_NAME,
                        contentValues.mapIndexed { idx, contentValue ->
                            val contentValueJoinKey = joinElementKeyName(idx, contentValue)

                            AfxAssignment(
                                AfxPath(contentValueJoinKey, contentValue.parseResult.first()),
                                contentValue,
                                contentValue.parseResult
                            )
                        },
                        emptyList(),
                        null,
                        false,
                        AfxFusionApi.JOIN_FUSION_OBJECT_NAME.qualifiedName,
                        contentValues.first().parseResult.first(),
                        contentValues.last().parseResult.last()
                    ),
                    contentValues.flatMap { it.parseResult }
                )
            }
        }

        val explicitPathBodyAssignments = contentValueCollector.bodyValues
            .map { explicitPathValue ->
                AfxAssignment(
                    AfxPath(explicitPathValue.explicitPath, explicitPathValue.parseResult),
                    explicitPathValue.afxValue,
                    explicitPathValue.afxValue.parseResult
                )
            }

        val bodyAssignments = afxValue.bodyAssignments +
                explicitPathBodyAssignments +
                listOfNotNull(contentAssignment)

        return FusionObjectValueDecl(
            identifier,
            parentIdentifier,
            QualifiedPrototypeNameDecl(
                nameIdentifier,
                afxDeclaration.elementIdentifier,
                SimplePrototypeNameDecl(
                    simpleNameIdentifier,
                    nameIdentifier,
                    afxValue.prototypeName.simpleName,
                    nameIdentifier.fusionFile,
                    afxAstReferences.objectSimpleName(afxValue, simpleNameIdentifier)
                ),
                PrototypeNamespaceDecl(
                    namespaceIdentifier,
                    nameIdentifier,
                    afxValue.prototypeName.namespace,
                    nameIdentifier.fusionFile,
                    afxAstReferences.objectNamespace(afxValue, namespaceIdentifier)
                ),
                nameIdentifier.fusionFile,
                afxAstReferences.objectName(afxValue, nameIdentifier)
            ),
            FusionObjectValue(
                afxValue.prototypeName,
                afxAstReferences.objectValue(afxValue, identifier)
            ),
            buildBody(
                identifier,
                path,
                bodyAssignments
            ),
            parentIdentifier.fusionFile,
            afxAstReferences.objectDecl(afxValue, identifier)
        )
    }

    private fun getContentPath(afxFusionObject: AfxFusionObject): RelativeFusionPathName =
        afxFusionObject.explicitChildrenPathName ?: AfxFusionApi.TAG_CONTENT_ATTRIBUTE

    private fun buildBody(
        parentElementIdentifier: FusionLangElementIdentifier,
        currentPath: AbsoluteFusionPathName,
        assignments: List<AfxAssignment>,
    ): InnerFusionDecl? {
        if (assignments.isEmpty()) {
            return null
        }

        val bodyIdentifier = identifierForValueBody(parentElementIdentifier)

        val assignmentDecls: List<FusionPathAssignmentDecl> = assignments.mapIndexed { idx, assignment ->
            val pathName = assignment.path.relativeFusionPathName
            val assignmentIdentifier = identifierForPathAssignment(bodyIdentifier, idx, pathName)
            val pathIdentifier = identifierForPathName(bodyIdentifier, pathName.pathAsString)
            val pathDecl = FusionPathNameDecl(
                pathIdentifier,
                bodyIdentifier,
                pathName.segments.map { segment ->
                    val segmentIdentifier = identifierForPathNameSegment(pathIdentifier, segment)
                    when (segment) {
                        is PropertyPathSegment ->
                            PathNameSegmentPropertyDecl(
                                segmentIdentifier,
                                pathIdentifier,
                                segment,
                                pathIdentifier.fusionFile,
                                afxAstReferences.assignmentPathSegment(assignment, segment, segmentIdentifier)
                            )
                        is MetaPropertyPathSegment ->
                            PathNameSegmentMetaPropertyDecl(
                                segmentIdentifier,
                                pathIdentifier,
                                segment,
                                pathIdentifier.fusionFile,
                                afxAstReferences.assignmentPathSegment(assignment, segment, segmentIdentifier)
                            )
                        else -> throw AfxParserError("Cannot transpile path segment: $segment")
                    }
                },
                currentPath,
                pathIdentifier.fusionFile,
                afxAstReferences.assignmentPath(assignment, pathIdentifier)
            )
            val path = currentPath + pathName
            FusionPathAssignmentDecl(
                assignmentIdentifier,
                parentElementIdentifier,
                pathDecl,
                path,
                transpileValue(path, assignment.value, assignmentIdentifier),
                idx,
                parentElementIdentifier.fusionFile,
                afxAstReferences.assignment(assignment, assignmentIdentifier)
            )
        }

        return InnerFusionDecl(
            bodyIdentifier,
            parentElementIdentifier,
            assignmentDecls,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            parentElementIdentifier.fusionFile,
            afxAstReferences.body(bodyIdentifier, assignments)
        )
    }

}

data class ContentValueCollector(
    val contentValues: List<AfxValue> = emptyList(),
    val bodyValues: List<ExplicitPathBodyValue> = emptyList()
) {

    fun collectValue(afxValue: AfxValue): ContentValueCollector =
        if (afxValue is AfxFusionObject && afxValue.explicitObjectPathName != null) {
            ContentValueCollector(
                contentValues, bodyValues + ExplicitPathBodyValue(
                    afxValue,
                    afxValue.explicitObjectPathName,
                    afxValue.collectedAttributes!!.pathAttribute!!
                )
            )
        } else {
            ContentValueCollector(contentValues + afxValue, bodyValues)
        }

    data class ExplicitPathBodyValue(
        val afxValue: AfxFusionObject,
        val explicitPath: RelativeFusionPathName,
        val parseResult: ParserRuleContext
    )

}

private fun joinElementKeyName(idx: Int, value: AfxValue): RelativeFusionPathName =
    if (value is AfxFusionObject && value.explicitKeyName != null) {
        FusionPathName.parseRelativePrependDot(value.explicitKeyName)
    } else {
        FusionPathName.attribute("item_${idx + 1}")
    }