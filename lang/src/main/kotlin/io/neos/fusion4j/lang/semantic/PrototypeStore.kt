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

import io.neos.fusion4j.lang.model.FusionPathName
import io.neos.fusion4j.lang.model.QualifiedPrototypeName
import io.neos.fusion4j.lang.model.RelativeFusionPathName
import io.neos.fusion4j.lang.model.decl.PrototypeDecl
import io.neos.fusion4j.lang.parser.RawFusionModel
import mu.KLogger
import mu.KotlinLogging

private val log: KLogger = KotlinLogging.logger {}

/**
 * All prototypes are loaded once initially.
 *
 * The whole inheritance graph is initialized and all prototype extensions are loaded.
 */
data class PrototypeStore internal constructor(
    private val prototypes: Map<QualifiedPrototypeName, FusionPrototype>,
    private val inheritanceIndex: Map<QualifiedPrototypeName, List<QualifiedPrototypeName>>
) {

    val size: Int = prototypes.size
    val prototypeNames: Set<QualifiedPrototypeName> = prototypes.keys

    fun get(prototypeName: QualifiedPrototypeName): FusionPrototype =
        prototypes[prototypeName]
            ?: throw FusionIndexError("Cannot get Fusion prototype from store; unknown prototype $prototypeName")

    fun isPrototypeDeclared(prototypeName: QualifiedPrototypeName): Boolean =
        prototypes.containsKey(prototypeName)

    companion object {

        fun create(
            rawFusionModel: RawFusionModel,
            fusionIndex: RawFusionIndex,
            configuration: PrototypeStoreConfiguration
        ): PrototypeStore {
            // preparation - collect all root prototype names
            val allPrototypeNames = rawFusionModel.getAllPrototypeNames()

            // step 1 load declared attributes and create inheritance reference
            val prototypesWithInheritanceReference = allPrototypeNames
                .associateWith {
                    initializePrototype(it, rawFusionModel, fusionIndex, configuration)
                }

            // step 2 load extension attributes
            val inheritanceIndex = allPrototypeNames
                .associateWith {
                    findInheritanceChain(it, prototypesWithInheritanceReference)
                }
            val linkedPrototypes = initializeExtensions(
                prototypesWithInheritanceReference,
                rawFusionModel,
                fusionIndex,
                inheritanceIndex
            )

            // step 3 link actual inherited Prototypes
            val prototypes = linkInheritedPrototypes(linkedPrototypes)

            return PrototypeStore(prototypes, inheritanceIndex)
        }

        /**
         * Step 1
         */
        private fun initializePrototype(
            prototypeName: QualifiedPrototypeName,
            rawFusionModel: RawFusionModel,
            fusionIndex: RawFusionIndex,
            configuration: PrototypeStoreConfiguration
        ): PrototypeDeclaredValueInitializationStep1 {
            // find all attributes by
            //   - looking up the current path for nested configurations
            //   - resolving path copy operations for the path and all parent paths
            //   - considering path erasures for the path and all parent paths
            //   - find path and prototype specific extensions
            //   - considering load order

            val rootPrototypePath = FusionPathName.rootPrototype(prototypeName)

            val prototypeAttributes = fusionIndex.resolveChildPathFusionValues(rootPrototypePath)
            val inheritedPrototypeName =
                findInheritedPrototype(prototypeName, rawFusionModel, fusionIndex.loadOrder, configuration)

            val prototypeInitialization = PrototypeDeclaredValueInitializationStep1(
                prototypeName,
                // we keep the declarations as reference
                rawFusionModel.getAllRootPrototypeDeclarationsForName(prototypeName)
                    .sortedWith(fusionIndex.loadOrder.elementOrder),
                inheritedPrototypeName,
                prototypeAttributes
            )

            log.debug("initialized prototype: $prototypeInitialization")

            return prototypeInitialization
        }

        private fun findInheritedPrototype(
            prototypeName: QualifiedPrototypeName,
            rawFusionModel: RawFusionModel,
            loadOrder: FusionLoadOrder,
            configuration: PrototypeStoreConfiguration
        ): QualifiedPrototypeName? {
            val prototypeDeclarations = rawFusionModel.getAllRootPrototypeDeclarationsForName(prototypeName)
                .sortedWith(loadOrder.elementOrder)

            val inheritanceDeclarations = prototypeDeclarations
                .mapNotNull { it.inheritPrototype }

            // validate possible inheritance problems
            val allInheritedPrototypeNames = inheritanceDeclarations
                .map { it.qualifiedName }
                .toSet()
            if (allInheritedPrototypeNames.size > 1) {
                log.warn(
                    "multiple inheritance declarations for prototype $prototypeName " +
                            "detected: $allInheritedPrototypeNames, strict mode: ${configuration.errorOnMultiInheritance}"
                )
                if (configuration.errorOnMultiInheritance) {
                    throw FusionIndexError(
                        "multiple inheritance declarations for prototype " +
                                "$prototypeName is forbidden due to strict configuration; " +
                                "inherited prototypes: $allInheritedPrototypeNames"
                    )
                }
            }

            return inheritanceDeclarations.firstOrNull()?.qualifiedName
        }

        private fun findInheritanceChain(
            prototypeName: QualifiedPrototypeName,
            initializedPrototypes: Map<QualifiedPrototypeName, PrototypeDeclaredValueInitializationStep1>,
            currentPrototypeName: QualifiedPrototypeName = prototypeName
        ): List<QualifiedPrototypeName> {
            val initializedPrototype = initializedPrototypes[currentPrototypeName]
                ?: throw FusionIndexError("Could not find prototype declaration for $currentPrototypeName")
            return if (initializedPrototype.inheritedPrototypeName != null) {
                listOf(currentPrototypeName) + findInheritanceChain(
                    prototypeName,
                    initializedPrototypes,
                    initializedPrototype.inheritedPrototypeName
                )
            } else {
                if (currentPrototypeName != prototypeName) {
                    listOf(currentPrototypeName)
                } else {
                    emptyList()
                }
            }
        }


        /**
         * Step 2
         */
        private fun initializeExtensions(
            linkedPrototypes: Map<QualifiedPrototypeName, PrototypeDeclaredValueInitializationStep1>,
            rawFusionModel: RawFusionModel,
            fusionIndex: RawFusionIndex,
            inheritanceIndex: Map<QualifiedPrototypeName, List<QualifiedPrototypeName>>
        ): Map<QualifiedPrototypeName, PrototypeExtensionInitializationStep2> =
            linkedPrototypes
                .mapValues { prototypeInitEntry ->
                    val prototypeName = prototypeInitEntry.key
                    val allPathExtensionsForPrototypeName =
                        rawFusionModel.getAllPathExtensionsForPrototypeName(prototypeName)
                            .map { it.absolutePath }
                            .toSet()
                    val groupedByExtensionPath = allPathExtensionsForPrototypeName
                        .groupBy { it.prototypeExtensionScopePath }
                        .mapValues { it.value.toSet() }

                    val groupedByScope = groupedByExtensionPath
                        .mapKeys { PrototypeExtensionScope.createFromDeclarationPath(it.key, inheritanceIndex) }

                    val scopedValueReferences = groupedByScope
                        .mapValues { scopedPaths ->
                            scopedPaths.value
                                .map {
                                    fusionIndex.getFusionValueReferenceForPath(it, it.prototypeExtensionPrototypePath)
                                }
                                .associateBy { it.relativePath }
                        }

                    prototypeInitEntry.value.initializeExtensionAttributesStep2(scopedValueReferences)
                }

        private fun linkInheritedPrototypes(
            prototypeInitializations: Map<QualifiedPrototypeName, PrototypeExtensionInitializationStep2>
        ): Map<QualifiedPrototypeName, FusionPrototype> {
            val basePrototypes = prototypeInitializations
                .filterValues { it.initialization.inheritedPrototypeName == null }
                .mapValues { it.value.finalizePrototypeStep3(null) }

            val inheritingPrototypesWithReferences = prototypeInitializations
                .filterValues { it.initialization.inheritedPrototypeName != null }
                .map { it.value }

            return internalLinkInheritedPrototypes(inheritingPrototypesWithReferences, basePrototypes)
        }

        private fun internalLinkInheritedPrototypes(
            todo: List<PrototypeExtensionInitializationStep2>,
            done: Map<QualifiedPrototypeName, FusionPrototype>
        ): Map<QualifiedPrototypeName, FusionPrototype> {
            val currentResults = todo.fold(InheritanceResolveResult()) { result, prototypeWithRef ->
                val inheritedPrototype = done[prototypeWithRef.initialization.inheritedPrototypeName]
                if (inheritedPrototype != null) {
                    result.foundPrototype(
                        prototypeWithRef.finalizePrototypeStep3(inheritedPrototype)
                    )
                } else {
                    result.notJetResolvable(prototypeWithRef)
                }
            }

            val newDone = done + currentResults.foundPrototypes.associateBy { it.prototypeName }

            val newTodo = currentResults.notJetResolvable
            return if (newTodo.isEmpty()) {
                newDone
            } else {
                internalLinkInheritedPrototypes(newTodo, newDone)
            }
        }

    }

}

/**
 * Step 1 - initialize declared attributes / keep a reference to the inherited prototype name that is
 *          resolved in the final step.
 */
data class PrototypeDeclaredValueInitializationStep1(
    val prototypeName: QualifiedPrototypeName,
    val rootPrototypeDeclarations: List<PrototypeDecl>,
    val inheritedPrototypeName: QualifiedPrototypeName?,
    val declaredChildPaths: Map<RelativeFusionPathName, FusionValueReference>
) {
    /**
     * Step 2 - initialize scoped extensions
     */
    fun initializeExtensionAttributesStep2(
        declaredExtensionChildPaths: Map<PrototypeExtensionScope, Map<RelativeFusionPathName, FusionValueReference>>
    ): PrototypeExtensionInitializationStep2 =
        PrototypeExtensionInitializationStep2(this, declaredExtensionChildPaths)
}

data class PrototypeExtensionInitializationStep2 internal constructor(
    val initialization: PrototypeDeclaredValueInitializationStep1,
    val declaredExtensionChildPaths: Map<PrototypeExtensionScope, Map<RelativeFusionPathName, FusionValueReference>>
) {
    /**
     * Step 3 - finalize and link inherited prototype
     */
    fun finalizePrototypeStep3(
        inheritedPrototype: FusionPrototype?
    ): FusionPrototype =
        if (initialization.inheritedPrototypeName == inheritedPrototype?.prototypeName) {
            FusionPrototype(
                initialization.prototypeName,
                initialization.rootPrototypeDeclarations,
                inheritedPrototype,
                initialization.declaredChildPaths,
                declaredExtensionChildPaths
            )
        } else {
            throw IllegalArgumentException("Invalid prototype inheritance initialization; declared inherited prototype '${initialization.inheritedPrototypeName}' must be equal to loaded prototype ${inheritedPrototype?.prototypeName}")
        }
}

data class InheritanceResolveResult(
    val foundPrototypes: List<FusionPrototype> = emptyList(),
    val notJetResolvable: List<PrototypeExtensionInitializationStep2> = emptyList()
) {
    fun foundPrototype(found: FusionPrototype): InheritanceResolveResult =
        InheritanceResolveResult(foundPrototypes + found, notJetResolvable)

    fun notJetResolvable(reference: PrototypeExtensionInitializationStep2): InheritanceResolveResult =
        InheritanceResolveResult(foundPrototypes, notJetResolvable + reference)
}