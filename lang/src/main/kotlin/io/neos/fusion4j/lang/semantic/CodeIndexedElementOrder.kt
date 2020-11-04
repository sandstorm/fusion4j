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

import io.neos.fusion4j.lang.model.decl.CodeIndexedElement

/**
 * Part of the [FusionLoadOrder]. Comparator for lines of Fusion code in the same file.
 * Semantically equivalent lines that appear later in the file are considered "more important" meaning their
 * declaration will win over lines that appear sooner in the file. Semantically equal means they effectively declare the
 * same absolute Fusion path.
 *
 * Example:
 *
 * ```neosfusion
 * a = 1
 * a = 2
 * ```
 * The line `a = 2` will win over the line `a = 1`, since it appears below.
 *
 * Each language element has the information in which LoC its declaration has been written.
 * See also [CodeIndexedElement].
 */
class CodeIndexedElementOrder : Comparator<CodeIndexedElement> {
    override fun compare(o1: CodeIndexedElement?, o2: CodeIndexedElement?): Int {
        checkNotNull(o1)
        checkNotNull(o2)
        // elements from different files / packages are not comparable
        assert(o1.sourceIdentifier.packageName == o2.sourceIdentifier.packageName)
        { "package names must be equal, but was: ${o1.sourceIdentifier.packageName} and ${o2.sourceIdentifier.packageName}" }
        if (o1.sourceIdentifier.resourceName != o2.sourceIdentifier.resourceName) {
            return 0
        }
        // same package / file -> then by index
        return CodeIndexedElement.CODE_INDEX_COMPARATOR.compare(o1, o2)
    }
}