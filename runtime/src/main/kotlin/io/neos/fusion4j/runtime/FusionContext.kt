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

package io.neos.fusion4j.runtime


data class FusionContext(
    private val layers: List<String>,
    override val currentContextMap: Map<String, Any?>
) : FusionContextAccess {

    /*
    override val currentContextMap: Map<String, Any?> by lazy {
        contextMatroschka
            .reversed()
            // main layer
            .fold(emptyMap()) { result, current ->
                result + current.data
                    // sub layer
                    .fold(emptyMap()) { resultInner, currentInner ->
                        resultInner + currentInner.data
                    }
            }
    }
     */

    fun push(nextContext: FusionContextLayer): FusionContext =
        if (nextContext.empty) {
            this
        } else {
            FusionContext(
                layers + nextContext.subLayers,
                currentContextMap + nextContext.effectiveContextMap
            )
        }

    override fun toString(): String = "FusionContext"

    companion object {
        fun create(initialContext: Map<String, Any?>, layerName: String = "init"): FusionContext =
            FusionContext(listOf(layerName), initialContext)

        fun empty(): FusionContext = FusionContext(emptyList(), emptyMap())
    }
}
