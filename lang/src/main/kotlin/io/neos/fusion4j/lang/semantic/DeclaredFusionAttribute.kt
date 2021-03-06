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
import io.neos.fusion4j.lang.model.values.FusionObjectValue
import io.neos.fusion4j.lang.model.values.FusionValue
import io.neos.fusion4j.lang.model.values.UntypedValue

data class DeclaredFusionAttribute(
    val valueReference: FusionValueReference
) : FusionAttribute {
    override val relativePath: RelativeFusionPathName = valueReference.relativePath
    override val untyped: Boolean = valueReference.fusionValue is UntypedValue
    override val valueDecl: FusionLangElement = valueReference.decl
    override val fusionObjectType: Boolean = valueReference.fusionValue is FusionObjectValue

    companion object {
        fun runtimeAttribute(
            fusionValue: FusionValue,
            basePath: AbsoluteFusionPathName,
            path: RelativeFusionPathName,
            decl: FusionLangElement
        ) =
            DeclaredFusionAttribute(FusionValueReference(fusionValue, decl, basePath + path, path))
    }
}