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
import org.antlr.v4.runtime.ParserRuleContext
import kotlin.math.min

data class AfxString internal constructor(
    val content: String,
    override val parseResult: List<ParserRuleContext>
) : AfxValue {

    val meaningful: Boolean = content.isNotEmpty()

    companion object {
        fun trimAttributeValueQuotes(attributeCtx: AfxParser.HtmlAttributeContext): String? {
            val contentRaw = attributeCtx.htmlAttributeValue()?.text
            return if (contentRaw != null) {
                val singleQuoted: Boolean = contentRaw.startsWith("'") && contentRaw.endsWith("'")
                val doubleQuoted: Boolean = contentRaw.startsWith("\"") && contentRaw.endsWith("\"")

                if (singleQuoted || doubleQuoted) {
                    contentRaw.substring(1, contentRaw.length - 1)
                } else {
                    contentRaw
                }
            } else {
                null
            }
        }

        fun fromAttributeValue(attributeCtx: AfxParser.HtmlAttributeContext): AfxString {
            val attributeValueCtx = attributeCtx.htmlAttributeValue()
            return if (attributeValueCtx == null) {
                AfxString(
                    "",
                    listOf(attributeCtx)
                )
            } else {
                AfxString(
                    sanitizeString(trimAttributeValueQuotes(attributeCtx)!!),
                    listOf(attributeValueCtx)
                )
            }
        }

        fun fromTextElement(fragment: ParserRuleContext): AfxString =
            AfxString(
                sanitizeString(fragment.text),
                listOf(fragment)
            )

        fun transpiledValue(
            value: String,
            parseResult: ParserRuleContext
        ): AfxString =
            AfxString(
                sanitizeString(value),
                listOf(parseResult)
            )

        /**
         * script tags are transpiled into a Fusion string
         */
        fun createScriptString(scriptContext: AfxParser.ScriptContext): AfxString =
            AfxString(
                scriptContext.text,
                listOf(scriptContext)
            )

        /**
         * style tags are transpiled into a Fusion string
         */
        fun createStyleString(styleContext: AfxParser.StyleContext): AfxString =
            AfxString(
                styleContext.text,
                listOf(styleContext)
            )

        /**
         * style tags are transpiled into a Fusion string
         */
        fun createHtmlCommentString(htmlCommentContext: AfxParser.HtmlCommentContext): AfxString =
            AfxString(
                htmlCommentContext.text,
                listOf(htmlCommentContext)
            )

        private val WHITESPACE_PATTERN = Regex("\\s+")

        private fun sanitizeString(rawContent: String): String =
            if (rawContent.contains("\n") && rawContent.trim().isEmpty()) {
                // newline whitespace noise is removed
                ""
            } else {
                val firstNonWhitespaceCharIdx = rawContent.indices.firstOrNull { !rawContent[it].isWhitespace() } ?: 0
                val lastNonWhitespaceCharIdx = min(
                    (rawContent.indices.lastOrNull { !rawContent[it].isWhitespace() } ?: rawContent.length) + 1,
                    rawContent.length
                )
                val whitespaceHead = rawContent.substring(0, firstNonWhitespaceCharIdx)
                val whitespaceTail = rawContent.substring(lastNonWhitespaceCharIdx)
                val contentRawTrimmed = rawContent.substring(firstNonWhitespaceCharIdx, lastNonWhitespaceCharIdx)
                val head = trimWrap(whitespaceHead)
                val tail = trimWrap(whitespaceTail)
                val content = head + contentRawTrimmed
                    .replace(WHITESPACE_PATTERN, " ") +
                        tail
                content.ifEmpty { " " }
            }

        private fun trimWrap(wrap: String): String =
            if (wrap.isEmpty() || wrap.contains("\n")) "" else " "
    }

    fun appendString(fragment: ParserRuleContext): AfxString =
        AfxString(sanitizeString(content + fragment.text), parseResult + fragment)

}