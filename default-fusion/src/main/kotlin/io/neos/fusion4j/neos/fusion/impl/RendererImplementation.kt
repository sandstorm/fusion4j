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

import io.neos.fusion4j.lang.model.AbsoluteFusionPathName
import io.neos.fusion4j.lang.model.FusionPathName
import io.neos.fusion4j.lang.model.FusionPathName.Companion.attribute
import io.neos.fusion4j.lang.model.QualifiedPrototypeName
import io.neos.fusion4j.lang.model.RelativeFusionPathName
import io.neos.fusion4j.runtime.*

@Suppress("unused")
class RendererImplementation : FusionObjectImplementation {
    companion object {
        private val ATTRIBUTE_RENDERER = attribute("renderer")
        private val ATTRIBUTE_RENDER_PATH = attribute("renderPath")
        private val ATTRIBUTE_TYPE = attribute("type")
        private val RUNTIME_ATTRIBUTE_ELEMENT = attribute("element")

        fun evaluateRenderer(runtime: FusionRuntimeImplementationAccess): Any? {

            // 'renderer' mode
            val rendererAttribute = runtime.attributes[ATTRIBUTE_RENDERER]
            val evaluationResult = if (rendererAttribute != null) {
                runtime.evaluateAttribute<Any?>(rendererAttribute)
            } else {

                // 'renderPath' mode
                val renderPathString = getRenderPath(runtime)
                if (renderPathString != null) {
                    val pathParsed = try {
                        when {
                            renderPathString.startsWith("/") ->
                                FusionPathName.parseAbsolute(renderPathString.substring(1))
                            renderPathString.startsWith(".") ->
                                FusionPathName.parseRelative(renderPathString)
                            else -> FusionPathName.parseRelativePrependDot(renderPathString)
                        }
                    } catch (renderPathParseError: Throwable) {
                        throw runtime.createRuntimeError(
                            "Could not parse render path $renderPathString; cause: ${renderPathParseError.message}",
                            renderPathParseError
                        )
                    }
                    when (pathParsed) {
                        is AbsoluteFusionPathName -> runtime.evaluateAbsolutePath<Any?>(pathParsed)
                        is RelativeFusionPathName -> runtime.evaluateRelativePath<Any?>(pathParsed)
                        else -> throw runtime.createRuntimeError("Unknown path type: $pathParsed")
                    }
                } else {
                    // 'type' mode
                    val typeAttribute = runtime.getRequiredPropertyAttribute(ATTRIBUTE_TYPE)
                    val evaluatedType = runtime.evaluateAttribute<String>(typeAttribute)
                    val prototypeName = QualifiedPrototypeName.fromString(
                        evaluatedType.lazyValue.value ?: throw runtime.createNullAttributeError(ATTRIBUTE_TYPE)
                    )
                    val elementRuntimeAttribute = runtime.createFusionObjectRuntimeAttribute(
                        prototypeName,
                        typeAttribute,
                        RUNTIME_ATTRIBUTE_ELEMENT
                    )
                    runtime.evaluateAttribute<Any?>(elementRuntimeAttribute)
                }
            }

            return if (evaluationResult.cancelled) {
                null
            } else {
                evaluationResult.lazyValue.value
            }
        }

        private fun getRenderPath(runtime: FusionRuntimeImplementationAccess): String? =
            runtime.evaluateAttributeValue(ATTRIBUTE_RENDER_PATH)

    }

    override fun evaluate(runtime: FusionRuntimeImplementationAccess): Any? =
        evaluateRenderer(runtime)
}