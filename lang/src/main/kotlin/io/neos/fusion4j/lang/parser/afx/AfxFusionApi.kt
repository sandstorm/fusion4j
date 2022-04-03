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

import io.neos.fusion4j.lang.model.FusionPathName
import io.neos.fusion4j.lang.model.QualifiedPrototypeName
import io.neos.fusion4j.lang.model.RelativeFusionPathName
import io.neos.fusion4j.lang.semantic.FusionPaths

interface AfxFusionApi {

    companion object {
        /**
         * Works on HTML and object tags.
         */
        const val KEY_META_ATTRIBUTE_NAME: String = "@key"

        /**
         * Works on HTML and object tags.
         */
        const val PATH_META_ATTRIBUTE_NAME: String = "@path"

        /**
         * Only works on object tags.
         */
        const val CHILDREN_META_ATTRIBUTE_NAME: String = "@children"

        val JOIN_FUSION_OBJECT_NAME = QualifiedPrototypeName.fromString("Neos.Fusion:Join")

        val TAG_FUSION_OBJECT_NAME = QualifiedPrototypeName.fromString("Neos.Fusion:Tag")
        val TAG_ATTRIBUTES_ATTRIBUTE: RelativeFusionPathName = FusionPathName.attribute("attributes")
        val TAG_CONTENT_ATTRIBUTE: RelativeFusionPathName = FusionPathName.attribute("content")
        val TAG_NAME_ATTRIBUTE: RelativeFusionPathName = FusionPathName.attribute("tagName")
        val TAG_SELF_CLOSING_ATTRIBUTE: RelativeFusionPathName = FusionPathName.attribute("selfClosingTag")

        private val TAG_META_ATTRIBUTES: Set<RelativeFusionPathName> = setOf(
            FusionPaths.PROCESS_META_ATTRIBUTE,
            FusionPaths.IF_META_ATTRIBUTE,
            // TODO is @context supported in AFX as well -> I think not, right?
            //FusionPaths.CONTEXT_META_ATTRIBUTE
        )

        fun isTagMetaAttribute(attributeName: String): Boolean =
            TAG_META_ATTRIBUTES.any { attributeName.startsWith("${it.pathAsString}.") }

    }

}