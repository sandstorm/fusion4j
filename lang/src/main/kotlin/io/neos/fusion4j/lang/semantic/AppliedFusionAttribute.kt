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

package io.neos.fusion4j.lang.semantic

import io.neos.fusion4j.lang.model.AbsoluteFusionPathName
import io.neos.fusion4j.lang.model.RelativeFusionPathName
import io.neos.fusion4j.lang.model.decl.FusionLangElement

/**
 * Keeps track of an applied attribute value and its declaration source.
 * The declaration source is the `@apply.someKey = ${someArray}` expression
 * value that declared the applied attribute.
 *
 * It is later used to keep AST references for nice error messages that involves
 * an applied attribute.
 */
data class AppliedFusionAttribute(
    override val relativePath: RelativeFusionPathName,
    override val valueDecl: FusionLangElement,
    val absolutePath: AbsoluteFusionPathName,
    val evaluatedValue: Any?
) : FusionAttribute {
    override val untyped: Boolean = false
    override val fusionObjectType: Boolean = false

    companion object {
        fun create(
            basePath: AbsoluteFusionPathName,
            attributeKey: RelativeFusionPathName,
            valueDecl: FusionLangElement,
            evaluatedValue: Any?
        ): AppliedFusionAttribute =
            AppliedFusionAttribute(
                attributeKey,
                valueDecl,
                basePath + attributeKey,
                evaluatedValue
            )
    }
}