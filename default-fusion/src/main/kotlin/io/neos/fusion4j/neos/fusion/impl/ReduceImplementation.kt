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
import io.neos.fusion4j.runtime.model.FusionDataStructure
import mu.KLogger
import mu.KotlinLogging

private val log: KLogger = KotlinLogging.logger {}

@Suppress("unused")
class ReduceImplementation : FusionObjectImplementation {

    companion object {
        private val ATTRIBUTE_ITEMS = attribute("items")
        private val ATTRIBUTE_ITEM_NAME = attribute("itemName")
        private val ATTRIBUTE_ITEM_KEY = attribute("itemKey")
        private val ATTRIBUTE_CARRY_NAME = attribute("carryName")
        private val ATTRIBUTE_INITIAL_VALUE = attribute("initialValue")
        private val ATTRIBUTE_ITERATION_NAME = attribute("iterationName")
        private val ATTRIBUTE_ITEM_REDUCER = attribute("itemReducer")

        fun evaluateReduce(runtime: FusionRuntimeImplementationAccess): Any? {
            val initialValue = runtime.evaluateRequiredAttributeOptionalValue<Any?>(ATTRIBUTE_INITIAL_VALUE)
            val items = getItems(runtime) ?: return initialValue
            val itemName = getItemName(runtime)
            val itemKey = getItemKey(runtime)
            val carryName = getCarryName(runtime)
            val iterationName = getIterationName(runtime)
            return items
                .foldIndexed(initialValue) { idx, result, keyAndValue ->
                    val iterationInformation = IterationInformation(idx, items.size)
                    val iterationContext = FusionContextLayer.layerOf(
                        "reduce-iteration",
                        mapOf(
                            iterationName to iterationInformation,
                            itemKey to keyAndValue.first,
                            itemName to keyAndValue.second,
                            carryName to result
                        )
                    )

                    val renderedItemResult =
                        runtime.evaluateRequiredAttribute<Any?>(ATTRIBUTE_ITEM_REDUCER, iterationContext)
                    if (!renderedItemResult.cancelled) {
                        // lazy value is not resolved here
                        renderedItemResult.lazyValue
                    } else {
                        log.warn { "Evaluation of mapping path $ATTRIBUTE_ITEM_REDUCER and key/value $keyAndValue was cancelled" }
                        // TODO throw error here?
                        // throw runtime.createRuntimeError("Evaluation of mapping path $itemRendererAttribute and key/value $keyAndValue was cancelled")
                        return null
                    }
                }
        }

        private fun getItems(runtime: FusionRuntimeImplementationAccess): FusionDataStructure<Any?>? =
            FusionDataStructure.fromAny(runtime.evaluateAttributeValue(ATTRIBUTE_ITEMS))

        private fun getItemName(runtime: FusionRuntimeImplementationAccess): String =
            runtime.evaluateRequiredAttributeValue(ATTRIBUTE_ITEM_NAME)

        private fun getItemKey(runtime: FusionRuntimeImplementationAccess): String =
            runtime.evaluateRequiredAttributeValue(ATTRIBUTE_ITEM_KEY)

        private fun getCarryName(runtime: FusionRuntimeImplementationAccess): String =
            runtime.evaluateRequiredAttributeValue(ATTRIBUTE_CARRY_NAME)

        private fun getIterationName(runtime: FusionRuntimeImplementationAccess): String =
            runtime.evaluateRequiredAttributeValue(ATTRIBUTE_ITERATION_NAME)
    }

    override fun evaluate(runtime: FusionRuntimeImplementationAccess): Any? =
        evaluateReduce(runtime)
}
