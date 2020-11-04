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
import io.neos.fusion4j.runtime.*
import io.neos.fusion4j.runtime.model.FusionAttribute
import io.neos.fusion4j.runtime.model.FusionDataStructure

@Suppress("unused")
class ComponentImplementation : FusionObjectImplementation {
    companion object {
        private val ATTRIBUTE_RENDERER = attribute("renderer")
        private const val PROPS_CONTEXT_VAR_NAME = "props"

        private fun includeAttributeInProps(attribute: FusionAttribute): Boolean =
            // exclude untyped and meta attributes
            attribute.relativePath.propertyAttribute && !attribute.untyped
                    // exclude "renderer"
                    && attribute.relativePath != ATTRIBUTE_RENDERER
    }

    override fun evaluate(runtime: FusionRuntimeImplementationAccess): Any? {
        val attributePathsSorted = runtime.propertyAttributesSorted
        val props = attributePathsSorted
            .filter(::includeAttributeInProps)
            .map {
                it.relativePath.propertyName to runtime.evaluateAttribute<Any?>(it)
            }
            .filter { !it.second.cancelled }
            .map { it.first to it.second.lazyValue }

        return runtime.evaluateRequiredAttributeValue(
            ATTRIBUTE_RENDERER,
            FusionContextLayer.layerOf(
                "Component props",
                mapOf(
                    PROPS_CONTEXT_VAR_NAME to FusionDataStructure(props)
                )
            )
        )
    }
}