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

import io.neos.fusion4j.lang.model.AbsoluteFusionPathName
import io.neos.fusion4j.lang.model.decl.*

/**
 * Part of the [PathIndex]. One entry contains all Fusion path declarations for a single path.
 * Declarations may come from different files and/or packages and are combined here in a unified index.
 * The declarations are already pre-sorted with the loading order.
 */
data class FusionPathIndexEntry(
    val path: AbsoluteFusionPathName,
    val assignments: List<FusionPathAssignmentDecl>,
    val configurations: List<FusionPathConfigurationDecl>,
    val erasures: List<FusionPathErasureDecl>,
    val copies: List<FusionPathCopyDecl>
) {

    fun getEffectiveAssignment(loadOrder: FusionLoadOrder): FusionPathAssignmentDecl? =
        getEffectivePathDecl(assignments, loadOrder)

    fun getEffectiveErasure(loadOrder: FusionLoadOrder): FusionPathErasureDecl? =
        getEffectivePathDecl(erasures, loadOrder)

    fun getEffectiveCopy(loadOrder: FusionLoadOrder): FusionPathCopyDecl? =
        getEffectivePathDecl(copies, loadOrder)

    companion object {
        private fun <T : FusionPathDecl> getEffectivePathDecl(all: List<T>, loadOrder: FusionLoadOrder): T? =
            all.sortedWith(loadOrder.elementOrder).firstOrNull()
    }

}