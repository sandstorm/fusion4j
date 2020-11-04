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

package io.neos.fusion4j.lang.file

import io.neos.fusion4j.lang.annotation.EelHelper
import io.neos.fusion4j.lang.annotation.FusionPackage
import io.neos.fusion4j.lang.model.EelHelperContextName
import io.neos.fusion4j.lang.model.FusionPackageName
import mu.KLogger
import mu.KotlinLogging
import org.reflections.ReflectionUtils
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder

private val log: KLogger = KotlinLogging.logger {}

interface FusionPackageLoader {

    val descriptor: FusionPackageLoaderDescriptor

    fun loadPackageFiles(classLoader: ClassLoader): Set<FusionFile>

    fun loadPackage(
        classLoader: ClassLoader
    ): FusionPackageDefinition = FusionPackageDefinition(
        loadPackageFiles(classLoader),
        descriptor
    )

    companion object {
        val DEFAULT_PACKAGES_TO_SCAN: Set<String> = setOf(
            "io.neos.fusion4j"
        )

        fun instantiatePackageLoader(descriptor: FusionPackageLoaderDescriptor): FusionPackageLoader {
            val constructor = try {
                descriptor.packageLoaderClass.getConstructor(FusionPackageLoaderDescriptor::class.java)
            } catch (error: Throwable) {
                throw IllegalStateException(
                    "Could not instantiate loader class ${descriptor.packageLoaderClass} for Fusion package ${descriptor.packageName}; " +
                            "it must have a single parameter constructor with parameter type ${FusionPackageLoaderDescriptor::class.java.name}",
                    error
                )
            }
            return try {
                constructor.newInstance(descriptor)
            } catch (error: Throwable) {
                throw IllegalStateException(
                    "Could not instantiate loader class ${descriptor.packageLoaderClass} for Fusion package ${descriptor.packageName}; " +
                            "exception during constructor call",
                    error
                )
            }
        }
    }

    class ClasspathScanner(
        private val classLoader: ClassLoader,
        private val packagesToScan: Set<String> = DEFAULT_PACKAGES_TO_SCAN
    ) {
        private val reflections = Reflections(
            ConfigurationBuilder()
                .forPackages(*packagesToScan.toTypedArray())
                .addClassLoaders(classLoader)
        )

        val classpathPackages: Set<FusionPackageLoaderDescriptor> = findAllPackagesOnClasspath()

        private fun findAllPackagesOnClasspath(): Set<FusionPackageLoaderDescriptor> {
            @Suppress("UNCHECKED_CAST")
            val allPackageClasses: Set<Class<FusionPackageLoader>> = reflections
                .get(Scanners.SubTypes.of(FusionPackageLoader::class.java.name).asClass<FusionPackageLoader>())
                .map { it as Class<FusionPackageLoader> }
                .toSet()

            return allPackageClasses
                .mapNotNull { packageClass ->
                    val packageAnnotation = ReflectionUtils.get(
                        ReflectionUtils.Annotations
                            .of(packageClass) {
                                it.annotationClass == FusionPackage::class
                            }
                            .map {
                                it as FusionPackage
                            }
                    ).singleOrNull()
                        ?: run {
                            log.warn { "Fusion package class ${packageClass.name} has no @FusionPackage annotation; it is ignored" }
                            return@mapNotNull null
                        }

                    FusionPackageLoaderDescriptor(
                        FusionPackageName(packageAnnotation.name),
                        FusionResourceName(packageAnnotation.entrypoint),
                        packageClass,
                        packageAnnotation.additionalEelHelpers
                            .map {
                                EelHelperDescriptor(
                                    EelHelperContextName(it.name),
                                    it.type.java
                                )
                            }
                            .toSet()
                    )
                }
                .toSet()
        }

        private fun findAllEelHelpersOnClasspath(): Set<EelHelperDescriptor> {
            val eelHelperClasses: Set<Class<out Any>> = reflections
                .get(Scanners.TypesAnnotated.with(EelHelper::class.java).asClass<Any>())

            return eelHelperClasses
                .map { eelHelperClass ->
                    val eelHelperAnnotation = ReflectionUtils.get(
                        ReflectionUtils.Annotations
                            .of(eelHelperClass) {
                                it.annotationClass == EelHelper::class
                            }
                            .map {
                                it as EelHelper
                            }
                    ).singleOrNull()
                        ?: throw IllegalStateException("EEL helper class $eelHelperClass must be annotated with @EelHelper")

                    EelHelperDescriptor(
                        EelHelperContextName(eelHelperAnnotation.name),
                        eelHelperClass
                    )
                }
                .toSet()
        }

        val eelHelpers: Set<EelHelperDescriptor> = findAllEelHelpersOnClasspath()


        override fun toString(): String {
            return "ClasspathScanner(\n" +
                    " classLoader: $classLoader\n" +
                    " packagesToScan: ${packagesToScan.joinToString("") { "\n  - $it" }}\n" +
                    " classpathPackages: ${classpathPackages.joinToString("") { "\n  - $it" }}\n" +
                    " eelHelpers: ${eelHelpers.joinToString("") { "\n  - $it" }}\n" +
                    ")"
        }

    }

    data class FusionPackageLoaderDescriptor(
        val packageName: FusionPackageName,
        val entrypoint: FusionResourceName,
        val packageLoaderClass: Class<out FusionPackageLoader>,
        val additionalRegisteredEelHelpers: Set<EelHelperDescriptor>
    ) {
        override fun toString(): String {
            return "FusionPackageLoaderDescriptor(packageName=$packageName, entrypoint='$entrypoint', packageLoaderClass=$packageLoaderClass)"
        }
    }

    data class EelHelperDescriptor(
        val contextName: EelHelperContextName,
        val eelHelperClass: Class<out Any>
    ) {
        override fun toString(): String {
            return "EelHelperDescriptor(contextName=$contextName, eelHelperClass=$eelHelperClass)"
        }
    }
}