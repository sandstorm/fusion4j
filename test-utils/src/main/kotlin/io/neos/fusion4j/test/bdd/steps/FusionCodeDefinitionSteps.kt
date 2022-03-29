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

package io.neos.fusion4j.test.bdd.steps

import io.cucumber.datatable.DataTable
import io.cucumber.java8.En
import io.neos.fusion4j.lang.file.*
import io.neos.fusion4j.lang.model.FusionPackageName
import io.neos.fusion4j.test.bdd.DisableLoggerUtil
import org.junit.Assert


class FusionCodeDefinitionSteps : En {
    companion object {

        init {
            DisableLoggerUtil.setLogLevelTo("WARN")
        }

        private var additionalPackages: MutableSet<FusionPackageLoader.FusionPackageLoaderDescriptor> = mutableSetOf()
        private var lastInstance: FusionCodeDefinitionSteps? = null
        fun getInstance(): FusionCodeDefinitionSteps =
            lastInstance ?: throw IllegalStateException("No instance of FusionCodeDefinitionSteps found")

        fun getFusionPackages(): Set<FusionPackageDefinition> {
            val mockPackages = lastInstance?.fusionPackages ?: emptyMap()

            // build fixture packages
            // and add all additional packages (externally added by other test code)
            val allPackageLoaders: Set<FusionPackageLoader> = mockPackages.values
                .map { it.toInMemoryPackageLoader() }
                .toSet() +
                    additionalPackages
                        .map { FusionPackageLoader.instantiatePackageLoader(it) }
            val allPackages = allPackageLoaders
                .map { loader ->
                    loader.loadPackage(FusionCodeDefinitionSteps::class.java.classLoader)
                }
                .toSet()
            if (allPackages.isEmpty()) {
                Assert.fail("No fusion code defined for parsing, define code with 'Given the package / Fusion file ...' or add packages programmatically")
            }
            return allPackages
        }

        fun getPackageLoadOrder(): List<FusionPackageName> {
            val packageLoadOrder = getInstance().packageLoadOrder
            return packageLoadOrder.ifEmpty {
                val allPackages = getFusionPackages()
                // For simple scenarios we usually have only one package
                // in that case you don't need to specify a load order.
                if (allPackages.size == 1) {
                    listOf(allPackages.single().packageName)
                } else {
                    throw IllegalStateException("No Fusion package load order; define with 'Given the following Fusion package load order'")
                }
            }
        }

        fun getPackageEntrypoints(): Map<FusionPackageName, FusionResourceName> =
            getFusionPackages().associateBy { it.packageName }.mapValues { it.value.entrypoint }

        fun registerPackage(fusionPackageDefinition: FusionPackageLoader.FusionPackageLoaderDescriptor) {
            additionalPackages.add(fusionPackageDefinition)
        }
    }

    private val fusionPackages: MutableMap<String, MockPackage> = mutableMapOf()
    private var packageLoadOrder: List<FusionPackageName> = listOf()

    init {

        Before { _ ->
            lastInstance = this
            additionalPackages.clear()
        }

        After { _ ->
            lastInstance = null
        }

        Given("the package {string} with entrypoint {string}") { packageName: String, entrypoint: String ->
            if (fusionPackages.contains(packageName)) {
                throw IllegalArgumentException("Fusion package $packageName already defined in current scenario")
            }
            fusionPackages[packageName] = MockPackage(packageName, entrypoint, mutableMapOf())
        }

        Given("the package {string} contains a Fusion file {string} with the following code") { packageName: String, fileName: String, code: String ->
            addMockFusionFile(packageName, fileName, code)
        }

        Given("the Fusion file {string} contains the following code") { fileName: String, code: String ->
            if (fusionPackages.size != 1) {
                throw IllegalStateException("This step can only be used when a single package is defined; packages: $fusionPackages")
            }
            addMockFusionFile(fusionPackages.keys.single(), fileName, code)
        }

        Given("the following Fusion package load order") { loadOrder: DataTable ->
            packageLoadOrder = loadOrder.asList()
                .map { FusionPackageName(it) }
        }

    }

    private fun addMockFusionFile(packageName: String, fileName: String, code: String) {
        val mockPackage = fusionPackages[packageName]
            ?: throw IllegalArgumentException("Fusion package $packageName not defined in current scenario, define a package with 'Given the package ...'")
        if (mockPackage.files.contains(fileName)) {
            throw IllegalArgumentException("Fusion file $fileName for package $packageName already defined in current scenario")
        }
        mockPackage.files[fileName] = code
    }

}

data class MockPackage(
    val packageName: String,
    val entrypoint: String,
    val files: MutableMap<String, String>
) {
    fun toInMemoryPackageLoader() = InMemoryFusionPackage(
        FusionPackageLoader.FusionPackageLoaderDescriptor(
            FusionPackageName(packageName),
            FusionResourceName.fromString(entrypoint),
            InMemoryFusionPackage::class.java,
            emptySet()
        ),
        files
            .map {
                InMemoryFusionFile(
                    it.value,
                    FusionPackageName(packageName),
                    FusionResourceName.fromString(it.key)
                )
            }
            .toSet()
    )
}