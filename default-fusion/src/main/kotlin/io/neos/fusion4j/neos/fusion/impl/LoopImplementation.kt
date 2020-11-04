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

package io.neos.fusion4j.neos.fusion.impl

import io.neos.fusion4j.lang.model.FusionPathName.Companion.metaAttribute
import io.neos.fusion4j.runtime.FusionObjectImplementation
import io.neos.fusion4j.runtime.FusionRuntimeImplementationAccess
import io.neos.fusion4j.runtime.evaluateAttributeValue
import java.io.StringWriter

@Suppress("unused")
class LoopImplementation : FusionObjectImplementation {
    companion object {
        private val META_ATTRIBUTE_GLUE = metaAttribute("glue")

        fun evaluateLoop(runtime: FusionRuntimeImplementationAccess): String? {
            val evaluatedMap = MapImplementation.evaluateMap(runtime, String::class.java)
            val writer = StringWriter()
            if (evaluatedMap == null) return null

            val glue = getGlue(runtime)
            var first = true
            for (pair in evaluatedMap.data) {
                if (first) {
                    first = false
                } else {
                    writer.write(glue)
                }
                val valueLazy = pair.second
                val valueEager = if (valueLazy is Lazy<*>) {
                    valueLazy.value
                } else {
                    valueLazy
                }
                if (valueEager != null) {
                    writer.write(valueEager.toString())
                }
            }
            return writer.buffer.toString()
        }


        private fun getGlue(runtime: FusionRuntimeImplementationAccess): String =
            runtime.evaluateAttributeValue(META_ATTRIBUTE_GLUE) ?: ""
    }

    override fun evaluate(runtime: FusionRuntimeImplementationAccess): String? =
        evaluateLoop(runtime)
}