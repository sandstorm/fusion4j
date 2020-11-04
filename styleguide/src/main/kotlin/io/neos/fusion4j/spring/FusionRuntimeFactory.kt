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

package io.neos.fusion4j.spring

import io.neos.fusion4j.lang.DEFAULT_DSL_PARSERS
import io.neos.fusion4j.lang.file.FusionPackageDefinition
import io.neos.fusion4j.lang.file.FusionPackageLoader
import io.neos.fusion4j.lang.file.FusionResourceName
import io.neos.fusion4j.lang.model.DslName
import io.neos.fusion4j.lang.model.EelHelperContextName
import io.neos.fusion4j.lang.model.FusionPackageName
import io.neos.fusion4j.lang.parseFusionPackages
import io.neos.fusion4j.lang.parser.DslParser
import io.neos.fusion4j.lang.parser.RawFusionModel
import io.neos.fusion4j.lang.semantic.PrototypeStoreConfiguration
import io.neos.fusion4j.lang.semantic.SemanticallyNormalizedFusionModel
import io.neos.fusion4j.lang.util.FusionProfiler
import io.neos.fusion4j.lang.util.TimeMeasureUtil
import io.neos.fusion4j.runtime.DefaultFusionRuntime
import io.neos.fusion4j.runtime.FusionObjectImplementationFactory
import io.neos.fusion4j.runtime.FusionRuntime
import io.neos.fusion4j.runtime.chain.DefaultFusionContextInitializer
import io.neos.fusion4j.runtime.eel.EelHelperFactory
import io.neos.fusion4j.runtime.eel.JexlEelEvaluator
import mu.KLogger
import mu.KotlinLogging
import java.nio.file.Path

private val log: KLogger = KotlinLogging.logger {}

class FusionRuntimeFactory internal constructor(
    private val packageLoadOrder: List<FusionPackageName>,
    private val dslParsers: Map<DslName, DslParser>,
    private val packageLoaders: List<Pair<FusionPackageName, FusionPackageLoader>>,
    private val prototypeStoreConfiguration: PrototypeStoreConfiguration,
    val eelHelpers: Map<EelHelperContextName, Class<out Any>>,
    private val fileSystemPackages: Map<FusionPackageName, Path>,
    private val fusionProfiler: FusionProfiler
) {

    private val entrypoints: Map<FusionPackageName, FusionResourceName> =
        packageLoaders.associate { it.first to it.second.descriptor.entrypoint }

    private fun parseFusionPackages(classLoader: ClassLoader): SemanticallyNormalizedFusionModel {
        val parseResultAndTime = TimeMeasureUtil.measureTime {
            val loadedPackages = packageLoaders
                .map { loaderPair ->
                    val fileSystemPath = fileSystemPackages[loaderPair.first]
                    val loadedPackage = if (fileSystemPath != null) {
                        log.warn {
                            "Fusion package ${loaderPair.first} will be loaded from file system " +
                                    "path $fileSystemPath - this should NOT BE ACTIVE IN PRODUCTION"
                        }
                        FusionPackageDefinition.loadAsFileSystemPackage(
                            loaderPair.second.descriptor,
                            fileSystemPath,
                        )
                    } else {
                        loaderPair.second.loadPackage(classLoader)
                    }
                    log.info {
                        "loaded Fusion package ${loaderPair.first} with ${loadedPackage.fusionFiles.size} files"
                    }
                    log.debug {
                        "files for package ${loaderPair.first}\n" +
                                loadedPackage.fusionFiles
                                    .map { it.identifier.resourceName.name }
                                    .sorted()
                                    .joinToString("\n") { "  - $it" }
                    }
                    loadedPackage
                }
                .toSet()
            log.info { "successfully loaded ${loadedPackages.size} Fusion packages" }
            val parseResult = parseFusionPackages(
                loadedPackages,
                dslParsers
            )
            // check for parse errors
            val parseErrors = parseResult
                .mapNotNull { it.error }
                .onEach {
                    log.error { "Fusion parse error:\n${it.toReadableOutput()}" }
                }
            if (parseErrors.isNotEmpty()) {
                throw IllegalArgumentException("there are ${parseErrors.size} Fusion parse errors: $parseErrors")
            }
            val rawModel = RawFusionModel(parseResult.map { it.success!! }.toSet())
            log.info { "successfully parsed ${rawModel.declarations.size} Fusion files" }

            // prepare the runtime state
            SemanticallyNormalizedFusionModel(
                fusionDeclaration = rawModel,
                packageLoadOrder = packageLoadOrder,
                packageEntrypoints = entrypoints,
                prototypeStoreConfiguration = prototypeStoreConfiguration
            )
        }

        log.info { parseResultAndTime.buildDurationMessage("parse Fusion packages") }

        return parseResultAndTime.result
    }

    fun createRuntime(
        classLoader: ClassLoader,
        runtimeProperties: RuntimeProperties,
        eelHelperFactory: EelHelperFactory,
        implementationFactory: FusionObjectImplementationFactory
    ): TimeMeasureUtil.ResultAndDuration<out FusionRuntime> {
        val runtimeAndTime = TimeMeasureUtil.measureTime {
            val semanticallyNormalizedFusionModel = parseFusionPackages(classLoader)
            DefaultFusionRuntime(
                semanticallyNormalizedFusionModel,
                JexlEelEvaluator(
                    semanticallyNormalizedFusionModel.rawIndex.pathIndex,
                    eelHelperFactory,
                    runtimeProperties.strictEelMode,
                    fusionProfiler
                ),
                // TODO make injectable
                DefaultFusionContextInitializer(),
                implementationFactory,
                fusionProfiler
            )
        }

        log.info { runtimeAndTime.buildDurationMessage("parse and initialize Fusion runtime") }

        return runtimeAndTime
    }

    override fun toString(): String {
        return "Spring FusionRuntimeFactory:\n" +
                " packageLoadOrder: ${packageLoadOrder.joinToString("") { "\n  - ${it.name}" }}\n" +
                " dslParsers: ${dslParsers.keys.joinToString(", ") { it.name }}\n" +
                " prototypeStoreConfiguration: $prototypeStoreConfiguration\n" +
                " eelHelperClassMapping: ${
                    eelHelpers.entries.sortedBy { it.key.name }
                        .joinToString { "\n  - ${it.key.name} => ${it.value.name}" }
                }\n" +
                " fileSystemPackages: ${
                    fileSystemPackages.entries.sortedBy { it.key.name }
                        .joinToString { "\n  - ${it.key} => ${it.value}" }
                }\n" +
                " entrypoints: ${
                    entrypoints.entries.sortedBy { it.key.name }
                        .joinToString { "\n  - ${it.key} => ${it.value}" }
                }\n" +
                ")"
    }

    companion object {

        fun initialize(
            parserProperties: ParserProperties,
            semanticProperties: SemanticProperties,
            fusionProfiler: FusionProfiler,
            customDsls: List<CustomFusionDsl>,
            localDevPackages: Map<String, String>,
            classLoader: ClassLoader
        ): FusionRuntimeFactory {
            val scanPackages = parserProperties.javaBasePackages.ifEmpty {
                FusionPackageLoader.DEFAULT_PACKAGES_TO_SCAN
            }
            val scanner = FusionPackageLoader.ClasspathScanner(
                classLoader,
                scanPackages
            )
            log.debug { "Scanning Fusion package loaders for java packages: $scanPackages" }
            val allClasspathPackages = scanner.classpathPackages
                .ifEmpty {
                    throw IllegalStateException("No Fusion packages found on classpath")
                }
                .associateBy { it.packageName }

            log.debug {
                "Fusion packages on classpath:\n" + allClasspathPackages.entries.joinToString("\n") {
                    "  - '${it.key}' => ${it.value}"
                }
            }
            val packageLoadOrder = parserProperties.fusionPackages
                .map(::FusionPackageName)
                .ifEmpty {
                    throw IllegalStateException("No Fusion packages configured")
                }
            log.info { "Fusion load order:\n" + packageLoadOrder.joinToString("\n") { "  - ${it.name}" } }

            val customDslParsers = customDsls
                .associate { it.dslName to it.dslParser }
            val allDslParsers = customDslParsers + if (parserProperties.enableDefaultDslsWithDefault) {
                DEFAULT_DSL_PARSERS
            } else {
                log.warn { "Fusion default DSLs like AFX are disabled via configuration" }
                emptyMap()
            }
            log.info { "Fusion DSLs: " + allDslParsers.keys.joinToString(", ") { it.name } }

            // filter configured packages
            val packageLoaders = packageLoadOrder
                .map { configuredPackageName ->
                    val packageDescriptor = allClasspathPackages[configuredPackageName]
                        ?: throw IllegalArgumentException(
                            "Configured Fusion package $configuredPackageName not found " +
                                    "on classpath; scanned packages: $allClasspathPackages"
                        )
                    val loader = FusionPackageLoader.instantiatePackageLoader(packageDescriptor)
                    configuredPackageName to loader
                }
            log.info { "successfully initialized ${packageLoaders.size} Fusion package loaders" }

            val classpathEelHelper: Map<EelHelperContextName, Class<*>> =
                eelHelperDescriptorsToClassMapping(scanner.eelHelpers, emptyMap())

            val eelHelperClassMapping = packageLoaders
                .fold(classpathEelHelper) { result, pair ->
                    eelHelperDescriptorsToClassMapping(
                        pair.second.descriptor.additionalRegisteredEelHelpers,
                        result
                    )
                }

            return FusionRuntimeFactory(
                packageLoadOrder,
                allDslParsers,
                packageLoaders,
                PrototypeStoreConfiguration(
                    errorOnMultiInheritance = semanticProperties.errorOnMultiInheritance
                ),
                eelHelperClassMapping,
                localDevPackages
                    .mapKeys { FusionPackageName(it.key) }
                    .mapValues { Path.of(it.value) },
                fusionProfiler
            )
        }

        private fun eelHelperDescriptorsToClassMapping(
            descriptors: Set<FusionPackageLoader.EelHelperDescriptor>,
            existingDescriptors: Map<EelHelperContextName, Class<*>>
        ): Map<EelHelperContextName, Class<out Any>> =
            descriptors.fold(existingDescriptors) { result, current ->
                val existing = result[current.contextName]
                if (existing != null) {
                    throw IllegalArgumentException(
                        "" +
                                "Duplicate classpath EEL helper context name: ${current.contextName}; " +
                                "first: ${existing.name}, second: ${current.eelHelperClass.name}"
                    )
                }
                result + (current.contextName to current.eelHelperClass)
            }

    }

}