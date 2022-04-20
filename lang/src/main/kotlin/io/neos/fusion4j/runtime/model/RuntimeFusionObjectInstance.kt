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
import io.neos.fusion4j.lang.semantic.FusionAttribute
import io.neos.fusion4j.lang.semantic.FusionObjectInstance
import io.neos.fusion4j.lang.semantic.PositionalArraySorter

/**
 * TODO performance / cleanup (probably remove AppliedAttributeSource and map directly to AppliedFusionAttribute)
 * A Fusion Object instance at a given fusionPath - with all the nested keys which exist at this path:
 * - descendants of the path
 * - attributes from the prototype (loaded from [FusionObjectInstance.attributes])
 */
interface RuntimeFusionObjectInstance {

    val fusionObjectInstance: FusionObjectInstance
    val isSideEffectFree: Boolean

    val attributes: Map<RelativeFusionPathName, FusionAttribute>
    val propertyAttributes: Map<RelativeFusionPathName, FusionAttribute>
    val positionalArraySorter: PositionalArraySorter
    val propertyAttributesSorted: List<FusionAttribute>

    fun getPropertyAttribute(pathName: RelativeFusionPathName): FusionAttribute?

}