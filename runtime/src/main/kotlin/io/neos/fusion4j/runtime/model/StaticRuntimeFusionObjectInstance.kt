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

data class StaticRuntimeFusionObjectInstance(
    override val fusionObjectInstance: FusionObjectInstance
) : RuntimeFusionObjectInstance {

    override val isSideEffectFree: Boolean = true

    override val attributes: Map<RelativeFusionPathName, FusionAttribute>
        get() = fusionObjectInstance.declaredAttributes

    override val propertyAttributes: Map<RelativeFusionPathName, FusionAttribute>
        get() = fusionObjectInstance.declaredPropertyAttributes

    override val positionalArraySorter: PositionalArraySorter
        get() = fusionObjectInstance.positionalArraySorter

    override val propertyAttributesSorted: List<FusionAttribute>
        get() = fusionObjectInstance.instancePropertyAttributesSorted

    override fun getPropertyAttribute(pathName: RelativeFusionPathName): FusionAttribute? =
        fusionObjectInstance.declaredPropertyAttributes[pathName]

    override fun toString(): String = "StaticRuntimeFusionObjectInstance"

}
