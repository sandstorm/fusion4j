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

import io.neos.fusion4j.lang.model.FusionPathName
import io.neos.fusion4j.lang.model.QualifiedPrototypeName
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class PrototypeExtensionScopeTest {
    private val testInheritanceChain = mapOf(
        QualifiedPrototypeName.fromString("Foo") to
                listOf(
                    QualifiedPrototypeName.fromString("Bar"),
                    QualifiedPrototypeName.fromString("VeryBase")
                ),
        QualifiedPrototypeName.fromString("Bar") to
                listOf(
                    QualifiedPrototypeName.fromString("VeryBase")
                ),
        QualifiedPrototypeName.fromString("Baz") to
                listOf(
                    QualifiedPrototypeName.fromString("VeryBase")
                ),
        QualifiedPrototypeName.fromString("VeryBase") to
                emptyList(),

        )

    @Test
    fun testInScopeOf_simplePath_exactMatch() {
        assertIsInScopeOf(
            "a.b.c",
            "a.b.c<Any>"
        )
    }

    @Test
    fun testInScopeOf_simplePath_exactMatchTyped() {
        assertIsInScopeOf(
            "a.b.c.prototype(Foo)",
            "a.b.c<Foo>"
        )
    }

    @Test
    fun testInScopeOf_prototypePath_exactTyped() {
        assertIsInScopeOf(
            "a.b.c.prototype(Foo)",
            "a<Bar>/b<Bar>/c<Foo>"
        )
    }

    @Test
    fun testInScopeOf_nestedPrototypePath_exactTyped() {
        assertIsInScopeOf(
            "prototype(Bar).b.c.prototype(Foo)",
            "a<Bar>/b<Bar>/c<Foo>"
        )
    }

    @Test
    fun testInScopeOf_nestedPrototypePath_inheritedTyped() {
        assertIsInScopeOf(
            "prototype(Bar).b.c.prototype(Foo)",
            "a<Bar>/b<Bar>/c<Bar>"
        )
    }

    @Test
    fun testInScopeOf_nestedSamePrototypePath_exactTyped() {
        assertIsInScopeOf(
            "prototype(Bar).prototype(Bar).prototype(Foo)",
            "a<Bar>/b<Bar>/c<Foo>"
        )
    }

    @Test
    fun testNotInScopeOf_nestedSamePrototypePath_exactTyped() {
        assertIsNotInScopeOf(
            "prototype(Bar).prototype(Bar).prototype(Foo)",
            "a<Bar>/c<Foo>"
        )
    }

    @Test
    fun testNotInScopeOf_childPath_wrongRoot() {
        assertIsNotInScopeOf(
            "a.b.c",
            "x.a.b.c"
        )
    }

    @Test
    fun testNotInScopeOf_childPrototypePath_wrongRoot() {
        assertIsNotInScopeOf(
            "a.b.c",
            "x<Foo>/a<Foo>/b<Foo>/c<Foo>"
        )
    }

    @Test
    fun testNotInScopeOf_simplePath_parentPath() {
        assertIsNotInScopeOf(
            "a.b.c",
            "a.b<Any>"
        )
    }

    private fun assertIsInScopeOf(
        scopePath: String,
        evaluationPath: String
    ) {
        val scopeOfPrototypeAttribute = PrototypeExtensionScope.createFromDeclarationPath(
            FusionPathName.parseAbsolute(scopePath),
            testInheritanceChain
        )
        assertTrue(
            "$evaluationPath must be in scope of $scopeOfPrototypeAttribute",
            scopeOfPrototypeAttribute.isInScope(EvaluationPath.parseFromString(evaluationPath))
        )
    }

    private fun assertIsNotInScopeOf(
        scopePath: String,
        evaluationPath: String
    ) {
        val scopeOfPrototypeAttribute = PrototypeExtensionScope.createFromDeclarationPath(
            FusionPathName.parseAbsolute(scopePath),
            testInheritanceChain
        )
        assertFalse(
            "$evaluationPath must not be in scope of $scopeOfPrototypeAttribute",
            scopeOfPrototypeAttribute.isInScope(EvaluationPath.parseFromString(evaluationPath))
        )
    }
}
