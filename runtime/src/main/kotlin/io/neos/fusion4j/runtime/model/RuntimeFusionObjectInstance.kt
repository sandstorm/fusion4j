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
import io.neos.fusion4j.lang.semantic.FusionObjectInstance
import io.neos.fusion4j.lang.semantic.PositionalArraySorter

/**
 * TODO performance / cleanup (probably remove AppliedAttributeSource and map directly to AppliedFusionAttribute)
 * A Fusion Object instance at a given fusionPath - with all the nested keys which exist at this path:
 * - descendants of the path
 * - attributes from the prototype (loaded from [FusionObjectInstance.attributes])
 */
data class RuntimeFusionObjectInstance(
    val fusionObjectInstance: FusionObjectInstance,
    val appliedAttributes: Map<RelativeFusionPathName, AppliedAttributeSource>,
) {
    private val attributesLazy: Lazy<Map<RelativeFusionPathName, FusionAttribute>> = lazy {
        fusionObjectInstance.attributes.mapValues { DeclaredFusionAttribute(it.value) } +
                appliedAttributes.mapValues {
                    AppliedFusionAttribute.fromAppliedAttributeSource(
                        // applied attributes are relative to the fusion object instance path
                        fusionObjectInstance.instanceDeclarationPath,
                        it.key,
                        it.value
                    )
                }
    }
    val attributes: Map<RelativeFusionPathName, FusionAttribute> by attributesLazy

    private val propertyAttributesLazy: Lazy<Map<RelativeFusionPathName, FusionAttribute>> = lazy {
        attributes
            .filterKeys { it.propertyAttribute }
    }

    val propertyAttributes: Map<RelativeFusionPathName, FusionAttribute> by propertyAttributesLazy

    val positionalArraySorter: PositionalArraySorter by lazy {
        PositionalArraySorter.createSorter(
            attributes.keys,
            fusionObjectInstance.attributePositions
        )
    }

    /**
     * Fast implementation of get property attribute. We don't want to initialize the whole lazy
     * property map for a single attribute access. Usually, you EITHER iterate all attributes OR
     * access specific attributes in Fusion object implementations.
     */
    fun getPropertyAttribute(pathName: RelativeFusionPathName): FusionAttribute? =
        if (propertyAttributesLazy.isInitialized()) {
            propertyAttributes[pathName]
        } else {
            val appliedAttribute = appliedAttributes[pathName]
            if (appliedAttribute != null) {
                AppliedFusionAttribute.fromAppliedAttributeSource(
                    // applied attributes are relative to the fusion object instance path
                    fusionObjectInstance.instanceDeclarationPath,
                    pathName,
                    appliedAttribute
                )
            } else {
                val declaredAttribute = fusionObjectInstance.attributes[pathName]
                if (declaredAttribute != null) {
                    DeclaredFusionAttribute(declaredAttribute)
                } else {
                    null
                }
            }
        }

}