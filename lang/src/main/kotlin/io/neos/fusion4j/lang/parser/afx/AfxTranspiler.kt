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
import io.neos.fusion4j.lang.model.AbsoluteFusionPathName
import io.neos.fusion4j.lang.model.decl.FusionValueDecl
import io.neos.fusion4j.lang.model.decl.values.DslDelegateValueDecl
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import kotlin.math.max

class AfxTranspiler(
    afxDeclaration: DslDelegateValueDecl,
    currentPath: AbsoluteFusionPathName,
    private val parsedModel: AfxParser.AfxCodeContext
) {

    private val afxDeclarations: AfxDeclarations = AfxDeclarations(afxDeclaration, currentPath)

    fun transpile(): FusionValueDecl {

        val fragments = parsedModel.afxFragment()
            .map { it.getChild(0) as ParserRuleContext }

        val afxValues = fragments
            .foldIndexed(emptyList()) { idx: Int, resultList: List<AfxValue>, fragment: ParserRuleContext ->
                val currentResult = resultList.lastOrNull()
                val finishedResults = resultList.subList(0, max(0, resultList.size - 1))

                // plain text content
                if (isTextElement(fragment)) {
                    // append / collect multiple text elements to single strings
                    return@foldIndexed when {
                        currentResult is AfxString -> {
                            finishedResults + currentResult.appendString(fragment)
                        }
                        currentResult is AfxFusionObject && !currentResult.closed ->
                            finishedResults + currentResult.appendTextContent(fragment)
                        else -> {
                            val newTextElement = AfxString.fromTextElement(fragment)
                            // trim start whitespaces
                            if (newTextElement.meaningful) {
                                resultList + newTextElement
                            } else {
                                resultList
                            }
                        }
                    }
                }

                // body expressions
                if (fragment is AfxParser.BodyExpressionValueContext) {
                    return@foldIndexed if (currentResult is AfxFusionObject && !currentResult.closed) {
                        finishedResults + currentResult.appendExpressionContent(fragment)
                    } else {
                        resultList + AfxExpressionValue.fromParsedExpressionValue(fragment)
                    }
                }

                // opening HTML tag
                if (fragment is AfxParser.TagStartContext) {
                    val tagObject = AfxFusionObject.startParsedHtmlTag(fragment)
                    return@foldIndexed if (currentResult is AfxFusionObject && !currentResult.closed) {
                        finishedResults + currentResult.appendInnerTag(tagObject)
                    } else {
                        finishedResults + listOfNotNull(
                            currentResult,
                            tagObject
                        )
                    }
                }

                // closing HTML tag
                if (fragment is AfxParser.TagEndContext || fragment is AfxParser.FusionObjectTagEndContext) {
                    if (currentResult is AfxFusionObject && !currentResult.closed) {
                        val closedObject = currentResult.closeObject(fragment)
                        return@foldIndexed finishedResults + closedObject
                    } else {
                        throw AfxParserError("Invalid closing HTML tag ${fragment.text}")
                    }
                }

                // opening Fusion object tag
                if (fragment is AfxParser.FusionObjectTagStartContext) {
                    val fusionObject = AfxFusionObject.startParsedObjectTag(fragment)
                    return@foldIndexed if (currentResult is AfxFusionObject && !currentResult.closed) {
                        finishedResults + currentResult.appendInnerTag(fusionObject)
                    } else {
                        finishedResults + listOfNotNull(
                            currentResult,
                            fusionObject
                        )
                    }
                }

                // HTML script tag
                if (fragment is AfxParser.ScriptContext) {
                    val scriptString = AfxString.createScriptString(fragment)
                    return@foldIndexed if (currentResult is AfxFusionObject && !currentResult.closed) {
                        finishedResults + currentResult.appendAfxString(scriptString)
                    } else {
                        finishedResults + listOfNotNull(
                            currentResult,
                            scriptString
                        )
                    }
                }

                // HTML style tag
                if (fragment is AfxParser.StyleContext) {
                    val styleString = AfxString.createStyleString(fragment)
                    return@foldIndexed if (currentResult is AfxFusionObject && !currentResult.closed) {
                        finishedResults + currentResult.appendAfxString(styleString)
                    } else {
                        finishedResults + listOfNotNull(
                            currentResult,
                            styleString
                        )
                    }
                }

                // HTML comments
                if (fragment is AfxParser.HtmlCommentContext) {
                    val htmlCommentString = AfxString.createHtmlCommentString(fragment)
                    return@foldIndexed if (currentResult is AfxFusionObject && !currentResult.closed) {
                        finishedResults + currentResult.appendAfxString(htmlCommentString)
                    } else {
                        finishedResults + listOfNotNull(
                            currentResult,
                            htmlCommentString
                        )
                    }
                }

                throw AfxParserError("Unknown AFX fragment of type ${fragment::class}: ${fragment.text}")
            }

        if (afxValues.isEmpty()) {
            return afxDeclarations.rootNullValue()
        }
        val last = afxValues.last()
        if (last is AfxFusionObject && !last.closed) {
            throw AfxParserError("Invalid non-closed Fusion object / HTML tag ${last.tagName}")
        }
        return if (afxValues.size == 1) {
            afxDeclarations.rootValue(afxValues.single())
        } else {
            afxDeclarations.rootJoin(afxValues)
        }
    }

    companion object {
        private fun isTextElement(fragment: ParseTree): Boolean =
            fragment is AfxParser.HtmlChardataContext ||
                    fragment is AfxParser.ScriptletContext ||
                    fragment is AfxParser.XhtmlCDATAContext ||
                    fragment is AfxParser.XmlContext ||
                    fragment is AfxParser.DtdContext

    }

}

