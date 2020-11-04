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

package io.neos.fusion4j.lang.parser

import io.neos.fusion4j.lang.model.QualifiedPrototypeName
import io.neos.fusion4j.lang.model.decl.FusionDecl
import io.neos.fusion4j.lang.model.decl.FusionPathDecl
import io.neos.fusion4j.lang.model.decl.PrototypeDecl
import io.neos.fusion4j.lang.model.decl.RootFusionDecl

data class RawFusionModel(
    val declarations: Set<RootFusionDecl>
) {

    fun getAllPrototypeNames(): Set<QualifiedPrototypeName> {
        // all root prototype declarations
        val rootPrototypes = declarations
            .flatMap(RootFusionDecl::rootPrototypeDeclarations)
            .map(PrototypeDecl::qualifiedName)
            .toSet()

        // plus all path assignments / configurations / erasures / copies
        // that have at least one prototype call path segment
        val pathSegmentPrototypes = declarations
            .flatMap(FusionDecl::getAllPrototypeNamesFromPathSegments)
            .toSet()

        return rootPrototypes + pathSegmentPrototypes
    }

    fun getAllRootPrototypeDeclarationsForName(prototypeName: QualifiedPrototypeName): Set<PrototypeDecl> =
        declarations
            .flatMap(RootFusionDecl::rootPrototypeDeclarations)
            .filter { it.qualifiedName == prototypeName }
            .toSet()

    fun getAllPathExtensionsForPrototypeName(prototypeName: QualifiedPrototypeName): Set<FusionPathDecl> =
        declarations
            .flatMap { it.getAllPathExtensionsForPrototype(prototypeName) }
            .toSet()

}