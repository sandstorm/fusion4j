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
import io.neos.fusion4j.lang.model.values.FusionValue
import io.neos.fusion4j.lang.model.values.UntypedValue

/**
 * A reference to a declared Fusion value loaded by the [RawFusionIndex].
 * Additional to holding the [FusionValue] itself, it also keeps track of:
 * <ul>
 *     <li>the declaration that, well, declared this value</li>
 *     <li>the [AbsoluteFusionPathName] of the value</li>
 *     <li>the [RelativeFusionPathName] of the value, relative to the base path it has been loaded from</li>
 * </ul>
 */
data class FusionValueReference(
    val fusionValue: FusionValue,
    val decl: FusionLangElement,
    val absolutePath: AbsoluteFusionPathName,
    val relativePath: RelativeFusionPathName
) {

    companion object {
        fun virtual(
            basePath: AbsoluteFusionPathName,
            path: RelativeFusionPathName,
            decl: FusionLangElement
        ): FusionValueReference =
            FusionValueReference(
                UntypedValue(decl.astReference),
                decl,
                basePath + path,
                path
            )
    }

    init {
        if (relativePath.segments.isEmpty()) {
            throw IllegalArgumentException("Prototype attribute must have at least one segmented relative path; but was: $relativePath")
        }
    }

    val untyped: Boolean = fusionValue is UntypedValue

    fun relativeTo(relativeBase: RelativeFusionPathName): FusionValueReference =
        FusionValueReference(
            fusionValue,
            decl,
            absolutePath,
            relativePath.relativeTo(relativeBase)
        )
}