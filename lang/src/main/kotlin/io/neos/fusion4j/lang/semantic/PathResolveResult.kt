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
import io.neos.fusion4j.lang.model.decl.FusionPathAssignmentDecl
import io.neos.fusion4j.lang.model.decl.FusionPathConfigurationDecl
import io.neos.fusion4j.lang.model.decl.FusionPathDecl
import io.neos.fusion4j.lang.model.decl.FusionPathErasureDecl
import io.neos.fusion4j.lang.model.values.ErasedValue
import io.neos.fusion4j.lang.model.values.FusionValue
import io.neos.fusion4j.lang.model.values.UntypedValue

/**
 * Internal result of a path resolving performed by the [RawFusionIndex].
 */
data class PathResolveResult private constructor(
    val requestedPath: AbsoluteFusionPathName,
    val resolvedPath: AbsoluteFusionPathName,
    val fusionValue: FusionValue,
    val decl: FusionPathDecl,
) {
    companion object {
        fun undefinedErased(
            path: AbsoluteFusionPathName,
            resolvedPath: AbsoluteFusionPathName,
            erasureDecl: FusionPathErasureDecl
        ): PathResolveResult =
            PathResolveResult(path, resolvedPath, ErasedValue(erasureDecl.astReference), erasureDecl)

        fun untyped(
            path: AbsoluteFusionPathName,
            resolvedPath: AbsoluteFusionPathName,
            configurationDecl: FusionPathConfigurationDecl
        ): PathResolveResult =
            PathResolveResult(path, resolvedPath, UntypedValue(configurationDecl.astReference), configurationDecl)

        fun assignment(
            path: AbsoluteFusionPathName,
            resolvedPath: AbsoluteFusionPathName,
            assignmentDecl: FusionPathAssignmentDecl
        ): PathResolveResult =
            PathResolveResult(path, resolvedPath, assignmentDecl.valueDeclaration.fusionValue, assignmentDecl)
    }

    fun toValueReference(parentPath: AbsoluteFusionPathName) = FusionValueReference(
        fusionValue = fusionValue,
        decl = decl,
        absolutePath = resolvedPath,
        relativePath = requestedPath.relativeTo(parentPath)
    )

}