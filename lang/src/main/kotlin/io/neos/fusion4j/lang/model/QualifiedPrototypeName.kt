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

package io.neos.fusion4j.lang.model

data class QualifiedPrototypeName(
    val simpleName: SimplePrototypeName,
    val namespace: PrototypeNamespace
) {

    val qualifiedName: String get() = if (!namespace.isEmpty()) "${namespace.name}:${simpleName.name}" else simpleName.name

    override fun toString(): String = qualifiedName

    companion object {
        fun fromString(qualifiedNameText: String): QualifiedPrototypeName {
            val simpleNameText = if (qualifiedNameText.contains(':'))
                qualifiedNameText.substring(qualifiedNameText.indexOf(':') + 1 until qualifiedNameText.length)
            else
                qualifiedNameText
            val namespaceText = if (qualifiedNameText.contains(':'))
                qualifiedNameText.substring(0 until qualifiedNameText.indexOf(':'))
            else
                null
            return QualifiedPrototypeName(SimplePrototypeName(simpleNameText), PrototypeNamespace(namespaceText ?: ""))
        }

        const val NAME_OR_NAMESPACE_PATTERN: String = "[a-zA-Z0-9]+(\\.[a-zA-Z0-9]+)*"
    }
}