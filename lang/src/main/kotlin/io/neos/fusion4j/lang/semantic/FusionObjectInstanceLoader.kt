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

import io.neos.fusion4j.lang.model.RelativeFusionPathName
import io.neos.fusion4j.lang.model.decl.FusionLangElement
import mu.KLogger
import mu.KotlinLogging

private val log: KLogger = KotlinLogging.logger {}

/**
 * Responsible for loading Fusion object instances including all attributes, etc.
 */
class FusionObjectInstanceLoader(
    private val semanticallyNormalizedFusionModel: SemanticallyNormalizedFusionModel
) {

    companion object {
        private const val CACHE_ENABLED = true
    }

    private val instanceCache: MutableMap<EvaluationPath, FusionObjectInstance> = mutableMapOf()

    fun loadInstance(
        evaluationPath: EvaluationPath,
        instanceDeclaration: FusionLangElement
    ): FusionObjectInstance {

        if (CACHE_ENABLED) {
            val cached = instanceCache[evaluationPath]
            if (cached != null) {
                return cached
            }
        }

        val prototypeName = evaluationPath.currentPrototypeName
        if (log.isDebugEnabled) {
            log.debug("Loading Fusion Object instance for evaluation path '$evaluationPath' and prototype $prototypeName")
        }

        val prototype = semanticallyNormalizedFusionModel.prototypeStore.get(prototypeName)

        val instancePath = evaluationPath.toDeclarationPath()

        val instanceChildPaths = semanticallyNormalizedFusionModel.rawIndex.resolveChildPathFusionValues(instancePath)
        val evaluationPathInstanceAttributes = getAllEvaluationPathInstanceAttributes(evaluationPath)

        val fusionObjectInstance = FusionObjectInstance(
            evaluationPath,
            instanceDeclaration,
            prototype,
            instanceChildPaths,
            evaluationPathInstanceAttributes,
            // TODO customizable?
            FusionPaths.POSITION_META_ATTRIBUTE
        )

        if (CACHE_ENABLED) {
            instanceCache[evaluationPath] = fusionObjectInstance
        }

        return fusionObjectInstance
    }

    /**
     * Evaluation path instance attributes example:
     *
     * foo = Value {
     *    // the following two lines are called "evaluation path instance attributes"
     *    // they are not part of the actual instance body, but (one of) the parent instance(s)
     *    value.0 = 'a'
     *    value.1 = 'b'
     *
     *    value = Join {
     *      2 = 'c'
     *    }
     * }
     *
     * WARNING: this is unplanned extensibility where you declare internal paths of another Fusion object. This should
     * be used with care!
     */
    private fun getAllEvaluationPathInstanceAttributes(evaluationPath: EvaluationPath): Map<RelativeFusionPathName, FusionValueReference> {
        return evaluationPath.segments.subList(0, evaluationPath.segments.size - 1)
            .foldIndexed(emptyMap()) { idx, result, parentSegment ->
                if (parentSegment.type == null) {
                    result
                } else {
                    val parentPrototype = semanticallyNormalizedFusionModel.prototypeStore.get(parentSegment.type)
                    val nestedPath = evaluationPath.segments.subList(
                        idx + 1,
                        evaluationPath.segments.size
                    )
                        .map(EvaluationPathSegment::nestedPath)
                        .reduce { path1, path2 -> path1 + path2 }
                    val allPathsFromInheritanceChain = parentPrototype.getChildPaths(evaluationPath, nestedPath)
                    result + allPathsFromInheritanceChain
                }
            }
    }

}