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

import java.util.Comparator.comparingInt

interface CodeIndexedElement : FusionLangElement {
    /**
     * The abstract index where this language element appears in its parent context.
     * It is used to remember the global order of code elements inside of a model,
     * that has multiple typed collections for inner elements.
     *
     * Example: A root fusion prototype declaration can contain multiple types of elements
     * in the inner code block: path assignments, path configurations and code comments.
     * Those are stored in typed list fields inside of [InnerFusionDecl]. To keep the global
     * order for later usage, this code index is used (e.g. for override rules or
     * comment-to-statement assignments).
     */
    val codeIndex: Int

    companion object {
        /**
         * The *last* of equally named Fusion path declarations wins.
         * Or: The highest code index wins.
         */
        val CODE_INDEX_COMPARATOR: Comparator<CodeIndexedElement> =
            comparingInt(CodeIndexedElement::codeIndex).reversed()
    }

}