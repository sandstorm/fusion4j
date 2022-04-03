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

import io.neos.fusion4j.lang.model.FusionPackageName
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import org.reflections.util.FilterBuilder
import java.io.InputStream
import java.util.*


data class ClasspathFusionFile(
    private val classLoader: ClassLoader,
    private val packageName: FusionPackageName,
    private val fullResourceName: String,
    private val resourceName: FusionResourceName
) : FusionFile {

    override val identifier: FusionSourceFileIdentifier =
        createFileIdentifier(packageName, resourceName, fullResourceName)

    override fun getInputStream(): InputStream = classLoader.getResourceAsStream(fullResourceName)
        ?: throw IllegalStateException("No classpath resource found for Fusion file $this")

    override fun toString(): String = "${identifier.identifierAsString} (resource: $fullResourceName)"

    override fun equals(other: Any?): Boolean =
        other is ClasspathFusionFile && Objects.equals(identifier, other.identifier)

    override fun hashCode(): Int = identifier.hashCode()

    companion object {
        const val TYPE = "classpath"

        fun createFileIdentifier(
            packageName: FusionPackageName,
            resourceName: FusionResourceName,
            fullResourceName: String
        ): FusionSourceFileIdentifier = FusionSourceFileIdentifier(
            TYPE,
            packageName,
            resourceName,
            mapOf(
                "resource" to fullResourceName
            )
        )

        fun scanAllFusionFilesOnClasspath(
            classLoader: ClassLoader,
            packageName: FusionPackageName,
            loadPackageName: String = packageName.name,
            excludedPatterns: Set<String> = emptySet()
        ): Set<ClasspathFusionFile> {
            val filter = excludedPatterns.fold(FilterBuilder().includePackage(loadPackageName)) { filter, pattern ->
                filter.excludePattern(pattern)
            }
            val reflections = Reflections(
                ConfigurationBuilder()
                    .addClassLoaders(classLoader)
                    .forPackage(loadPackageName)
                    .filterInputsBy(filter)
                    .setScanners(Scanners.Resources)
            )
            return reflections.get(Scanners.Resources.with(".*\\.fusion"))
                .map {
                    ClasspathFusionFile(
                        classLoader,
                        packageName,
                        it,
                        FusionResourceName(it.substring(loadPackageName.length + 1))
                    )
                }
                .toSet()
        }

        fun scanClasspathExcludingTests(
            classLoader: ClassLoader,
            packageName: FusionPackageName,
            loadPackageName: String = packageName.name,
            excludedPatterns: Set<String> = emptySet()
        ): Set<ClasspathFusionFile> =
            scanAllFusionFilesOnClasspath(
                classLoader,
                packageName,
                loadPackageName,
                excludedPatterns + setOf(
                    ".*\\.Tests\\.Unit\\..*",
                    ".*\\.Tests\\.Functional\\..*",
                    ".*\\.Tests\\.Behavior\\..*"
                )
            )

        fun scanClasspathExcludingTests(
            classLoader: ClassLoader,
            packageName: FusionPackageName,
            loadPackageNames: Set<String>,
            excludedPatterns: Set<String> = emptySet()
        ): Set<ClasspathFusionFile> =
            loadPackageNames
                .flatMap {
                    scanClasspathExcludingTests(
                        classLoader,
                        packageName,
                        it,
                        excludedPatterns
                    )
                }
                .toSet()
    }

}