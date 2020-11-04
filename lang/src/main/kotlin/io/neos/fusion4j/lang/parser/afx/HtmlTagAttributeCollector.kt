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
import io.neos.fusion4j.lang.model.RelativeFusionPathName
import io.neos.fusion4j.lang.parser.afx.AfxFusionApi.Companion.CHILDREN_META_ATTRIBUTE_NAME
import io.neos.fusion4j.lang.parser.afx.AfxFusionApi.Companion.KEY_META_ATTRIBUTE_NAME
import io.neos.fusion4j.lang.parser.afx.AfxFusionApi.Companion.PATH_META_ATTRIBUTE_NAME
import io.neos.fusion4j.lang.parser.afx.AfxFusionApi.Companion.TAG_ATTRIBUTES_ATTRIBUTE
import io.neos.fusion4j.lang.parser.afx.AfxFusionApi.Companion.isTagMetaAttribute
import io.neos.fusion4j.lang.semantic.FusionPaths

data class HtmlTagAttributeCollector(
    val attributes: List<AfxTagAttribute> = emptyList(),
    val keyAttribute: AfxParser.HtmlAttributeContext? = null,
    val pathAttribute: AfxParser.HtmlAttributeContext? = null,
    val childrenAttribute: AfxParser.HtmlAttributeContext? = null
) {

    companion object {

        fun collectParsedAttribute(
            index: Int,
            result: HtmlTagAttributeCollector,
            attributeCtx: AfxParser.HtmlAttributeContext
        ): HtmlTagAttributeCollector {
            // Spread expressions are only supported on object tags but are parsed for
            // all tags (not only Fusion Object tags). We want to throw an explicit
            // errors and be able to parse a common user error
            // when spread expressions are used on regular HTML tags.
            return if (attributeCtx.tagAttributeSpreadExpression() != null) {
                HtmlTagAttributeCollector(
                    result.attributes + AfxTagAttribute(index, null, attributeCtx),
                    result.keyAttribute,
                    result.pathAttribute,
                    result.childrenAttribute
                )
            } else when (val attributeName = attributeCtx.htmlAttributeName().text) {
                KEY_META_ATTRIBUTE_NAME -> {
                    if (result.keyAttribute != null) {
                        throw attributeError("HTML tag key attribute '$attributeName' cannot be defined multiple times")
                    }
                    HtmlTagAttributeCollector(
                        result.attributes,
                        attributeCtx,
                        result.pathAttribute,
                        result.childrenAttribute
                    )
                }
                PATH_META_ATTRIBUTE_NAME -> {
                    if (result.pathAttribute != null) {
                        throw attributeError("HTML tag path attribute '$attributeName' cannot be defined multiple times")
                    }
                    HtmlTagAttributeCollector(
                        result.attributes,
                        result.keyAttribute,
                        attributeCtx,
                        result.childrenAttribute
                    )
                }
                CHILDREN_META_ATTRIBUTE_NAME -> {
                    if (result.childrenAttribute != null) {
                        throw attributeError("HTML tag children attribute '$attributeName' cannot be defined multiple times")
                    }
                    HtmlTagAttributeCollector(
                        result.attributes,
                        result.keyAttribute,
                        result.pathAttribute,
                        attributeCtx
                    )
                }
                else -> {
                    if (result.attributes.any { it.name != null && it.name == attributeName }) {
                        throw attributeError("HTML tag attribute '$attributeName' cannot be defined multiple times")
                    }
                    HtmlTagAttributeCollector(
                        result.attributes + AfxTagAttribute(index, attributeName, attributeCtx),
                        result.keyAttribute,
                        result.pathAttribute,
                        result.childrenAttribute
                    )
                }
            }
        }

        private fun attributeError(reason: String): RuntimeException {
            // TODO better message / AST ref
            throw AfxParserError(reason)
        }


    }

    fun fusionObjectTagAssignments(): List<AfxAssignment> = convertCollectedAttributesToAssignments { attribute ->
        val attributeName = attribute.name
        if (attributeName == null) {
            // spread attribute -> '@apply.spread_X'
            // spread attributes only support expression values
            if (attribute.parseResult.tagAttributeSpreadExpression() == null) {
                throw attributeError("Spread attribute only supports expression values; but was: ${attribute.parseResult.text}")
            }
            FusionPaths.APPLY_META_ATTRIBUTE.builder()
                .property("spread_${attribute.index + 1}").build()
        } else {
            FusionPathName.parseSimpleRelative(attributeName)
        }
    }

    fun htmlTagAssignments(): List<AfxAssignment> =
        convertCollectedAttributesToAssignments { attribute ->
            val attributeName = attribute.name
            when {
                // spread attribute not allowed for HTML tags
                attributeName == null ->
                    throw attributeError("HTML tags do not support spread syntax; but was: $attribute")
                // all tag preprocessor and conditional meta property attributes are written to the Fusion object body
                isTagMetaAttribute(attributeName) ->
                    FusionPathName.current()
                // all "regular" HTML tag attributes are written to
                else -> TAG_ATTRIBUTES_ATTRIBUTE
            } + FusionPathName.parseSimpleRelative(attributeName)
        }

    private fun convertCollectedAttributesToAssignments(
        relativeBasePathFactory: (AfxTagAttribute) -> RelativeFusionPathName
    ): List<AfxAssignment> =
        attributes
            .map { attribute ->

                val attributeCtx = attribute.parseResult
                val attributeNameCtx = attributeCtx.htmlAttributeName()
                val attributeValueCtx = attributeCtx.htmlAttributeValue()

                val afxValue = when {
                    // spread expressions
                    attributeCtx.tagAttributeSpreadExpression() != null ->
                        AfxExpressionValue.fromParsedSpreadExpressionValue(attributeCtx.tagAttributeSpreadExpression())
                    // attribute value expressions
                    attributeValueCtx?.tagAttributeExpressionValue() != null ->
                        AfxExpressionValue.fromParsedExpressionValue(attributeValueCtx.tagAttributeExpressionValue())
                    // TODO other attribute value types / boolean etc? maybe primitive numbers
                    else -> AfxString.fromAttributeValue(attributeCtx)
                }
                val nameContext = attributeNameCtx ?: attributeCtx

                val path = relativeBasePathFactory(attribute)

                AfxAssignment(
                    AfxPath(path, nameContext),
                    afxValue,
                    listOf(attributeCtx)
                )
            }

}

data class AfxTagAttribute(
    val index: Int,
    val name: String?,
    val parseResult: AfxParser.HtmlAttributeContext
)