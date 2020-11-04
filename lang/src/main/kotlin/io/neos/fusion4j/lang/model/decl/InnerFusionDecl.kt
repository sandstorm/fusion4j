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

package io.neos.fusion4j.lang.model.decl

import io.neos.fusion4j.lang.file.FusionSourceFileIdentifier
import io.neos.fusion4j.lang.model.PathNameIndex


/**
 * Inner declaration of a Fusion code block. Basically everything inside '{' and '}'.
 * It can occur in ether a Fusion object declaration (A), or in a path configuration (B).
 *
 * A:
 * myPath = Some.Fusion:Object {
 *      // fusion code declaration
 * }
 *
 * B:
 * myPath {
 *      // fusion code declaration
 * }
 *
 * Note that in contrast to a [RootFusionDecl], no file includes are allowed.
 */
data class InnerFusionDecl(
    override val elementIdentifier: FusionLangElementIdentifier,
    override val parentElementIdentifier: FusionLangElementIdentifier,
    override val pathAssignments: List<FusionPathAssignmentDecl>,
    override val pathConfigurations: List<FusionPathConfigurationDecl>,
    override val pathCopyDeclarations: List<FusionPathCopyDecl>,
    override val pathErasures: List<FusionPathErasureDecl>,
    override val codeComments: List<CodeCommentDecl>,
    override val sourceIdentifier: FusionSourceFileIdentifier,
    override val astReference: AstReference
) : FusionDecl, InnerFusionLangElement {

    override val elementIndex: CodeElementIndex = CodeElementIndex.buildCodeElementIndex(
        pathAssignments,
        pathConfigurations,
        pathCopyDeclarations,
        pathErasures,
        codeComments
    )

    override val pathNameIndex: PathNameIndex = PathNameIndex.buildPathNameIndex(
        pathAssignments,
        pathConfigurations,
        pathCopyDeclarations,
        pathErasures
    )

}