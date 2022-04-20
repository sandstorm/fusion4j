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

package io.neos.fusion4j.neos

import io.neos.fusion4j.lang.file.FusionPackageLoader
import io.neos.fusion4j.lang.file.FusionResourceName
import io.neos.fusion4j.lang.model.FusionPackageName
import io.neos.fusion4j.neos.fusion.NeosFusionPackage
import io.neos.fusion4j.neos.neos.NeosNeosPackage
import io.neos.fusion4j.neos.nodetypes.NeosNodeTypesPackage
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.StringWriter

class DefaultPackageLoaderTest {

    @Test
    fun test_scanAllPackagesOnClasspath() {
        val packageScanner = FusionPackageLoader.ClasspathScanner(
            DefaultPackageLoaderTest::class.java.classLoader
        )
        assertEquals(
            "packages mismatch",
            setOf(
                FusionPackageLoader.FusionPackageLoaderDescriptor(
                    FusionPackageName("Neos.Fusion"),
                    FusionResourceName.fromString("Root.Override.fusion"),
                    NeosFusionPackage::class.java,
                    emptySet()
                ),
                FusionPackageLoader.FusionPackageLoaderDescriptor(
                    FusionPackageName("Neos.Neos"),
                    FusionResourceName.fromString("Resources/Private/Fusion/Root.fusion"),
                    NeosNeosPackage::class.java,
                    emptySet()
                ),
                FusionPackageLoader.FusionPackageLoaderDescriptor(
                    FusionPackageName("Neos.NodeTypes"),
                    FusionResourceName.fromString("Resources/Private/Fusion/Root.fusion"),
                    NeosNodeTypesPackage::class.java,
                    emptySet()
                )
            ),
            packageScanner.classpathPackages
        )
    }

}