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

package bdd

import ch.qos.logback.classic.Level
import io.cucumber.java8.En
import io.neos.fusion4j.lang.file.FusionPackageDefinition
import io.neos.fusion4j.lang.file.FusionPackageLoader
import io.neos.fusion4j.lang.model.FusionPackageName
import io.neos.fusion4j.neos.fusion.NeosFusionPackage
import io.neos.fusion4j.neos.neos.NeosNeosPackage
import io.neos.fusion4j.neos.nodetypes.NeosNodeTypesPackage
import io.neos.fusion4j.test.bdd.steps.FusionCodeDefinitionSteps
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Suppress("unused")
class PackageDefinitionSteps : En {
    companion object {
        var lastInstance: PackageDefinitionSteps? = null
        private val scanner = FusionPackageLoader.ClasspathScanner(
            PackageDefinitionSteps::class.java.classLoader,
            // TODO configure via steps
            FusionPackageLoader.DEFAULT_PACKAGES_TO_SCAN
        )
        val defaultTestPackages = scanner.classpathPackages.associateBy { it.packageName }
    }

    init {

        Before { _ ->
            lastInstance = this
            val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
            rootLogger.level = Level.INFO
        }

        After { _ ->
            lastInstance = null
        }

        Given("the default Fusion package {string} is registered") { packageNameString: String ->
            val packageName = FusionPackageName(packageNameString)
            val pkg = defaultTestPackages[packageName]
                ?: throw IllegalArgumentException("Unknown default Fusion package $packageNameString")
            FusionCodeDefinitionSteps.registerPackage(pkg)
        }

    }

}
