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

package io.neos.fusion4j.neos.fusion.impl

import io.neos.fusion4j.lang.annotation.FusionApi
import io.neos.fusion4j.lang.model.FusionPathName.Companion.attribute
import io.neos.fusion4j.runtime.*
import io.neos.fusion4j.runtime.model.EvaluationResult
import mu.KLogger
import mu.KotlinLogging
import org.apache.commons.text.StringEscapeUtils
import java.io.StringWriter

private val log: KLogger = KotlinLogging.logger {}

@FusionApi
class TagImplementation : FusionObjectImplementation {

    companion object {
        private val DEFAULT_SELF_CLOSING_TAGS = setOf(
            "area",
            "base",
            "br",
            "col",
            "command",
            "embed",
            "hr",
            "img",
            "input",
            "keygen",
            "link",
            "meta",
            "param",
            "source",
            "track",
            "wbr"
        )

        private val ATTRIBUTE_TAG_NAME = attribute("tagName")
        private val ATTRIBUTE_CONTENT = attribute("content")
        private val ATTRIBUTE_OMIT_CLOSING_TAG = attribute("omitClosingTag")
        private val ATTRIBUTE_SELF_CLOSING_TAG = attribute("selfClosingTag")
        private val ATTRIBUTE_ATTRIBUTES = attribute("attributes")
        private val ATTRIBUTE_ALLOW_EMPTY_ATTRIBUTES = attribute("allowEmptyAttributes")

        private fun renderAttributes(
            tagName: String,
            attributes: List<Pair<String, Any?>>,
            allowEmptyAttributes: Boolean,
            writer: StringWriter
        ) {
            for (pair in attributes) {
                val attributeName = pair.first
                val attributeValue = pair.second
                val attributeValueUnpacked = when (attributeValue) {
                    is Lazy<*> -> FusionRuntime.unwrapLazy(attributeValue)
                    is EvaluationResult<*> -> EvaluationResult.unwrapEvaluationResult(attributeValue)
                    else -> attributeValue
                }
                if (attributeValueUnpacked == null || attributeValueUnpacked == false) {
                    //log.debug { "Attribute '$attributeName' of tag $tagName is not rendered; value $attributeValue is null or false" }
                } else {
                    writer.write(" $attributeName")
                    if (attributeValueUnpacked == true || attributeValueUnpacked == "") {
                        if (!allowEmptyAttributes) {
                            writer.write("=\"\"")
                        }
                    } else {
                        writer.write("=\"")
                        writer.write(StringEscapeUtils.escapeHtml4(attributeValueUnpacked.toString()))
                        writer.write("\"")
                    }
                }
            }
        }

        private fun getTagName(runtime: FusionRuntimeImplementationAccess): String =
            runtime.evaluateRequiredAttributeValue(ATTRIBUTE_TAG_NAME)

        private fun getContent(runtime: FusionRuntimeImplementationAccess): String? =
            runtime.evaluateAttributeValue(ATTRIBUTE_CONTENT)

        private fun isOmitClosingTag(runtime: FusionRuntimeImplementationAccess): Boolean =
            runtime.evaluateRequiredAttributeValue(ATTRIBUTE_OMIT_CLOSING_TAG)

        private fun isSelfClosingTag(runtime: FusionRuntimeImplementationAccess, tagName: String): Boolean =
            DEFAULT_SELF_CLOSING_TAGS.contains(tagName) ||
                    runtime.evaluateRequiredAttributeValue(ATTRIBUTE_SELF_CLOSING_TAG)

        private fun getAttributes(runtime: FusionRuntimeImplementationAccess): List<Pair<String, Any?>> =
            runtime.evaluateAttributeValue(ATTRIBUTE_ATTRIBUTES) ?: listOf()

        private fun isAllowEmptyAttributes(runtime: FusionRuntimeImplementationAccess): Boolean =
            runtime.evaluateAttributeValue(ATTRIBUTE_ALLOW_EMPTY_ATTRIBUTES) ?: true

    }

    override fun evaluate(runtime: FusionRuntimeImplementationAccess): String {

        val tagName = getTagName(runtime)
        val omitClosingTag = isOmitClosingTag(runtime)
        val selfClosingTag = isSelfClosingTag(runtime, tagName)
        val renderContentAndClosingTag = !omitClosingTag && !selfClosingTag
        val allowEmptyAttributes = isAllowEmptyAttributes(runtime)
        val attributesData = getAttributes(runtime)

        val writer = StringWriter()

        // opening tag
        writer.write("<")
        writer.write(tagName)
        // attributes
        renderAttributes(tagName, attributesData, allowEmptyAttributes, writer)
        // self-closing
        if (selfClosingTag) {
            writer.write("/")
        }
        // close opening tag
        writer.write(">")
        if (renderContentAndClosingTag) {
            // tag body
            //val start = System.currentTimeMillis()
            writer.write(getContent(runtime) ?: "")
            //log.warn { "took ${System.currentTimeMillis() - start} ms - " + runtime.callstack.currentEvaluationPath }
            // closing tag
            writer.write("</")
            writer.write(tagName)
            writer.write(">")
        }
        // FIXME slow as fuck
        return writer.buffer.toString()
    }

}
