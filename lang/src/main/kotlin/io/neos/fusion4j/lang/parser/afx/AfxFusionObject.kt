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
import io.neos.fusion4j.lang.model.FusionPathName
import io.neos.fusion4j.lang.model.QualifiedPrototypeName
import io.neos.fusion4j.lang.model.RelativeFusionPathName
import org.antlr.v4.runtime.ParserRuleContext
import kotlin.math.max

data class AfxFusionObject(
    val prototypeName: QualifiedPrototypeName,
    val bodyAssignments: List<AfxAssignment>,
    val contentValues: List<AfxValue>,
    val collectedAttributes: HtmlTagAttributeCollector?,
    val selfClosing: Boolean,
    val tagName: String,
    private val startContext: ParserRuleContext,
    private val endContext: ParserRuleContext?
) : AfxValue {

    override val parseResult: List<ParserRuleContext> = listOfNotNull(startContext, endContext)
    val explicitKeyName: String? =
        getOptionalAttributeValue(collectedAttributes?.keyAttribute) { it }
    val explicitChildrenPathName: RelativeFusionPathName? =
        getOptionalAttributeValue(collectedAttributes?.childrenAttribute, FusionPathName::attribute)
    val explicitObjectPathName: RelativeFusionPathName? =
        getOptionalAttributeValue(collectedAttributes?.pathAttribute, FusionPathName::attribute)

    val closed: Boolean = selfClosing || endContext != null

    companion object {

        private fun <T> getOptionalAttributeValue(
            attributeCtx: AfxParser.HtmlAttributeContext?,
            mapper: (String) -> T
        ): T? {
            return if (attributeCtx != null) {
                val attributeValueString = AfxString.trimAttributeValueQuotes(attributeCtx)
                if (attributeValueString != null) {
                    mapper(attributeValueString)
                } else {
                    null
                }
            } else {
                null
            }
        }

        fun startParsedHtmlTag(
            tagContext: AfxParser.TagStartContext
        ): AfxFusionObject {
            val tagNameContext = tagContext.htmlTagName()
            val tagName = tagNameContext.text
            val selfClosing = tagContext.TAG_SLASH_CLOSE() != null
            val collectedAttributes: HtmlTagAttributeCollector = tagContext.htmlAttribute()
                .foldIndexed(HtmlTagAttributeCollector(), HtmlTagAttributeCollector::collectParsedAttribute)

            val tagApiAssignments = listOfNotNull(
                // tag name
                AfxAssignment(
                    AfxPath(AfxFusionApi.TAG_NAME_ATTRIBUTE, tagNameContext),
                    AfxString.transpiledValue(tagName, tagNameContext),
                    listOf(tagNameContext)
                ),
                // self-closing
                if (selfClosing) {
                    AfxAssignment(
                        AfxPath(AfxFusionApi.TAG_SELF_CLOSING_ATTRIBUTE, tagNameContext),
                        AfxBoolean(true, listOf(tagNameContext)),
                        listOf(tagContext)
                    )
                } else {
                    null
                }
            )
            val attributeAssignments = collectedAttributes.htmlTagAssignments()

            return AfxFusionObject(
                AfxFusionApi.TAG_FUSION_OBJECT_NAME,
                tagApiAssignments + attributeAssignments,
                emptyList(),
                collectedAttributes,
                selfClosing,
                tagName,
                tagContext,
                null
            )
        }


        fun startParsedObjectTag(
            tagContext: AfxParser.FusionObjectTagStartContext
        ): AfxFusionObject {
            val tagPrototypeNameContext = tagContext.fusionObjectTagName()
            val tagPrototypeName = QualifiedPrototypeName.fromString(tagPrototypeNameContext.text)
            val selfClosing = tagContext.TAG_SLASH_CLOSE() != null

            val collectedAttributes: HtmlTagAttributeCollector = tagContext.htmlAttribute()
                .foldIndexed(HtmlTagAttributeCollector(), HtmlTagAttributeCollector::collectParsedAttribute)

            val bodyAssignments = collectedAttributes.fusionObjectTagAssignments()

            return AfxFusionObject(
                tagPrototypeName,
                bodyAssignments,
                emptyList(),
                collectedAttributes,
                selfClosing,
                tagPrototypeName.qualifiedName,
                tagContext,
                null
            )
        }

    }

    private fun <T> expectOpen(code: () -> T): T =
        if (closed) {
            throw AfxParserError("Could not modify object $this after closing")
        } else {
            code()
        }

    private fun appendContentValue(valuesMapper: (AfxValue?, List<AfxValue>) -> List<AfxValue>): AfxFusionObject =
        expectOpen {
            val currentResult = contentValues.lastOrNull()
            val finishedResults = contentValues.subList(0, max(0, contentValues.size - 1))

            AfxFusionObject(
                prototypeName,
                bodyAssignments,
                valuesMapper(currentResult, finishedResults),
                collectedAttributes,
                selfClosing,
                tagName,
                startContext,
                endContext
            )
        }

    fun appendTextContent(fragment: ParserRuleContext): AfxFusionObject =
        appendContentValue { current, finished ->
            when {
                current is AfxFusionObject && !current.closed -> finished + current.appendTextContent(fragment)
                current is AfxString -> finished + current.appendString(fragment)
                else -> {
                    val textElement = AfxString.fromTextElement(fragment)
                    finished + listOfNotNull(
                        current,
                        if (textElement.meaningful) textElement else null
                    )
                }
            }
        }

    fun appendAfxString(afxString: AfxString): AfxFusionObject =
        appendContentValue { current, finished ->
            finished + listOfNotNull(
                current,
                afxString
            )
        }

    fun appendExpressionContent(fragment: ParserRuleContext): AfxFusionObject =
        appendContentValue { current, finished ->
            when {
                current == null -> finished + AfxExpressionValue.fromParsedExpressionValue(fragment)
                current is AfxFusionObject && !current.closed -> finished + current.appendExpressionContent(fragment)
                else -> finished + current + AfxExpressionValue.fromParsedExpressionValue(fragment)
            }
        }

    fun appendInnerTag(tagObject: AfxFusionObject): AfxFusionObject =
        appendContentValue { current, finished ->
            when {
                current == null -> finished + tagObject
                current is AfxFusionObject && !current.closed -> finished + current.appendInnerTag(tagObject)
                else -> {
                    finished + current + tagObject
                }
            }
        }

    fun closeObject(fragment: ParserRuleContext): AfxFusionObject =
        expectOpen {
            val current = contentValues.lastOrNull()
            val finishedContentValues = contentValues.subList(0, max(0, contentValues.size - 1))
            AfxFusionObject(
                prototypeName,
                bodyAssignments,
                if (current is AfxFusionObject && !current.closed)
                    finishedContentValues + current.closeObject(fragment)
                else
                    contentValues,
                collectedAttributes,
                selfClosing,
                tagName,
                startContext,
                if (current is AfxFusionObject && !current.closed)
                    endContext
                else
                    fragment
            )
        }
}