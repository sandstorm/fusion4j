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

import io.neos.fusion4j.lang.model.FusionPathName.Companion.attribute
import io.neos.fusion4j.lang.model.QualifiedPrototypeName
import io.neos.fusion4j.runtime.FusionObjectImplementation
import io.neos.fusion4j.runtime.FusionRuntimeImplementationAccess
import io.neos.fusion4j.runtime.evaluateRequiredAttributeValue

@Suppress("unused")
class MatcherImplementation : FusionObjectImplementation {
    companion object {
        val PROTOTYPE_NAME = QualifiedPrototypeName.fromString("Neos.Fusion:Matcher")
        private val ATTRIBUTE_CONDITION = attribute("condition")

        private fun getCondition(runtime: FusionRuntimeImplementationAccess): Boolean =
            runtime.evaluateRequiredAttributeValue(ATTRIBUTE_CONDITION)
    }

    override fun evaluate(runtime: FusionRuntimeImplementationAccess): Any? =
        if (getCondition(runtime)) {
            RendererImplementation.evaluateRenderer(runtime)
        } else {
            // we cannot return empty string or null here
            // see NoMatchResult.INSTANCE
            NoMatchResult.INSTANCE
        }
}