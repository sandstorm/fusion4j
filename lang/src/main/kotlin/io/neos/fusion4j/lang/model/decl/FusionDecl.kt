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

import io.neos.fusion4j.lang.model.PathNameIndex
import io.neos.fusion4j.lang.model.QualifiedPrototypeName

/**
 * This is the meta model for a declaration block in Fusion.
 *
 * It can be one of: [RootFusionDecl] or [InnerFusionDecl]
 *
 * 1. the root code layer, meaning the outermost block in a Fusion file
 */
interface FusionDecl : FusionLangElement {
    val pathAssignments: List<FusionPathAssignmentDecl>
    val pathConfigurations: List<FusionPathConfigurationDecl>
    val pathCopyDeclarations: List<FusionPathCopyDecl>
    val pathErasures: List<FusionPathErasureDecl>
    val codeComments: List<CodeCommentDecl>
    val elementIndex: CodeElementIndex
    val pathNameIndex: PathNameIndex

    val empty: Boolean get() = elementIndex.mappedByIndex.isEmpty()

    fun getCorrelatedCodeCommentsForElementAt(index: Int): List<CodeCommentDecl> {
        val errorFactory = { message: String ->
            throw IllegalArgumentException("could not get correlated code comments for element at index $index; $message")
        }
        return when {
            index < 0 -> errorFactory("index negative")
            index == 0 -> errorFactory("first element cannot have comments")
            index > elementIndex.size - 1 -> errorFactory("out of bounds (max index: ${elementIndex.size - 1})")
            elementIndex.mappedByIndex[index] is CodeCommentDecl -> errorFactory("element is code comment itself: ${elementIndex.mappedByIndex[index]}")
            else -> elementIndex.asOrderedList
                .subList(0, index)
                .takeLastWhile { it is CodeCommentDecl }
                .map { it as CodeCommentDecl }
        }
    }

    fun getAllPrototypeNamesFromPathSegments(): Set<QualifiedPrototypeName> =
        pathNameIndex.getAllPrototypeNamesFromPathSegments()

    fun getAllPathExtensionsForPrototype(prototypeName: QualifiedPrototypeName): Set<FusionPathDecl> =
        pathNameIndex.getAllPathExtensionsForPrototype(prototypeName)
}