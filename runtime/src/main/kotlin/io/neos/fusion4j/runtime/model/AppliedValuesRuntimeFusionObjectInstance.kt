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

package io.neos.fusion4j.runtime.model

import io.neos.fusion4j.lang.model.RelativeFusionPathName
import io.neos.fusion4j.lang.semantic.*


data class AppliedValuesRuntimeFusionObjectInstance(
    override val fusionObjectInstance: FusionObjectInstance,
    val appliedAttributes: Map<RelativeFusionPathName, AppliedFusionAttribute>
) : RuntimeFusionObjectInstance {

    override val isSideEffectFree: Boolean = appliedAttributes.isEmpty()

    private val attributesLazy: Lazy<Map<RelativeFusionPathName, FusionAttribute>> = lazy {
        fusionObjectInstance.declaredAttributes + appliedAttributes
    }
    override val attributes: Map<RelativeFusionPathName, FusionAttribute> by attributesLazy

    private val propertyAttributesLazy: Lazy<Map<RelativeFusionPathName, FusionAttribute>> = lazy {
        attributes
            .filterKeys { it.propertyAttribute }
    }

    override val propertyAttributes: Map<RelativeFusionPathName, FusionAttribute> by propertyAttributesLazy

    override val positionalArraySorter: PositionalArraySorter by lazy {
        if (isSideEffectFree) {
            fusionObjectInstance.positionalArraySorter
        } else {
            PositionalArraySorter.createSorter(
                propertyAttributes.keys,
                fusionObjectInstance.attributePositions
            )
        }
    }

    override val propertyAttributesSorted: List<FusionAttribute> by lazy {
        if (isSideEffectFree) {
            fusionObjectInstance.instancePropertyAttributesSorted
        } else {
            propertyAttributes.values
                .sortedWith { o1, o2 ->
                    positionalArraySorter
                        .compare(o1.relativePath, o2.relativePath)
                }
        }
    }

    /**
     * Fast implementation of get property attribute. We don't want to initialize the whole lazy
     * property map for a single attribute access. Usually, you EITHER iterate all attributes OR
     * access specific attributes in Fusion object implementations.
     */
    override fun getPropertyAttribute(pathName: RelativeFusionPathName): FusionAttribute? =
        when {
            isSideEffectFree -> fusionObjectInstance.declaredPropertyAttributes[pathName]
            propertyAttributesLazy.isInitialized() -> propertyAttributes[pathName]
            else -> appliedAttributes[pathName] ?: fusionObjectInstance.declaredPropertyAttributes[pathName]
        }

    override fun toString(): String = "AppliedValuesRuntimeFusionObjectInstance"
}