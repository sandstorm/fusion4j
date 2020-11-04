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
import io.neos.fusion4j.lang.model.QualifiedPrototypeName
import io.neos.fusion4j.runtime.FusionObjectImplementation
import io.neos.fusion4j.runtime.FusionRuntimeImplementationAccess
import io.neos.fusion4j.runtime.evaluateAttribute
import io.neos.fusion4j.runtime.evaluateAttributeValue
import io.neos.fusion4j.runtime.model.EvaluationResult
import java.io.StringWriter

@Suppress("unused")
class JoinImplementation : FusionObjectImplementation {
    companion object {
        private val PROTOTYPE_NAME: QualifiedPrototypeName = QualifiedPrototypeName.fromString("Neos.Fusion:Join")
        private val META_ATTRIBUTE_GLUE = metaAttribute("glue")

        fun evaluateJoin(runtime: FusionRuntimeImplementationAccess): String {
            val attributeSorted = runtime.propertyAttributesSorted
            val glue = getGlue(runtime)

            val writer = StringWriter()
            var first = true
            for (attribute in attributeSorted) {
                if (first) {
                    first = false
                } else {
                    writer.write(glue)
                }
                val valueResult: EvaluationResult<String?> = if (attribute.untyped) {
                    runtime.evaluateAttribute(attribute, PROTOTYPE_NAME)
                } else {
                    runtime.evaluateAttribute(attribute)
                }
                if (valueResult.cancelled) {
                    continue
                }
                val value = valueResult.lazyValue.value
                if (value != null) {
                    writer.write(value)
                }
            }
            return writer.buffer.toString()
        }

        private fun getGlue(runtime: FusionRuntimeImplementationAccess): String =
            runtime.evaluateAttributeValue(META_ATTRIBUTE_GLUE) ?: ""
    }

    override fun evaluate(runtime: FusionRuntimeImplementationAccess): String =
        evaluateJoin(runtime)
}