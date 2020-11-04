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

package io.neos.fusion4j.lang.semantic

import io.neos.fusion4j.lang.file.FusionResourceName
import io.neos.fusion4j.lang.model.FusionPackageName
import io.neos.fusion4j.lang.file.FusionSourceFileIdentifier
import io.neos.fusion4j.lang.parser.RawFusionModel

/**
 * This class bundles the semantics of Fusion.
 *
 * There are <b>three</b> main ingredients that adds up the semantics:
 * <ul>
 *     <li>effective path resolving</li>
 *     <li>prototype loading</li>
 *     <li>Fusion object instance loading</li>
 * </ul>
 *
 * The [RawFusionModel], the package load order + entrypoint files are translated into the main state to access actual Fusion paths
 * and Fusion Object instances / Prototypes.
 *
 * The [RawFusionIndex] is mainly responsible for resolving paths.
 * Simple example:
 *
 * ```neosfusion
 * a = 1
 * a = 2
 * ```
 * The [RawFusionIndex.resolvePath] for `a` will give you the [FusionValueReference] `a = 2`
 * while the [RawFusionModel] still contains both declarations.
 *
 * The [PrototypeStore] initially loads all prototypes and holds them for later access.
 *
 * The [FusionObjectInstanceLoader] loads [FusionObjectInstance]s with all its descendent paths.
 *
 * The only thing that cannot be performed here is the `@apply` semantic, since the Fusion runtime context is needed
 * to perform apply-logic. See the RuntimeFusionObjectInstance class in the runtime package for more info.
 */
data class SemanticallyNormalizedFusionModel(
    val fusionDeclaration: RawFusionModel,
    val packageLoadOrder: List<FusionPackageName>,
    val packageEntrypoints: Map<FusionPackageName, FusionResourceName>,
    val prototypeStoreConfiguration: PrototypeStoreConfiguration = PrototypeStoreConfiguration()
) {

    val rawIndex: RawFusionIndex = RawFusionIndex.build(fusionDeclaration, packageLoadOrder, packageEntrypoints)
    val prototypeStore: PrototypeStore = PrototypeStore.create(fusionDeclaration, rawIndex, prototypeStoreConfiguration)
    val fusionObjectInstanceLoader: FusionObjectInstanceLoader =
        FusionObjectInstanceLoader(this)

}