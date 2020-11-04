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
import java.nio.file.Path
import kotlin.io.path.exists

data class FusionPackageDefinition(
    val fusionFiles: Set<FusionFile>,
    val descriptor: FusionPackageLoader.FusionPackageLoaderDescriptor
) {
    val packageName: FusionPackageName = descriptor.packageName
    val entrypoint: FusionResourceName = descriptor.entrypoint

    companion object {
        fun loadAsFileSystemPackage(
            descriptor: FusionPackageLoader.FusionPackageLoaderDescriptor,
            packageBasePath: Path
        ): FusionPackageDefinition {
            if (!packageBasePath.exists()) {
                throw IllegalArgumentException("Could not load Fusion package from filesystem; package base path $packageBasePath does not exist")
            }

            return FusionPackageDefinition(
                FilesystemFusionFile.findAllFusionFilesInBasePath(descriptor.packageName, packageBasePath),
                descriptor
            )
        }
    }

    init {
        if (fusionFiles.none { entrypoint == it.identifier.resourceName }) {
            throw IllegalArgumentException("invalid package $packageName; entrypoint $entrypoint not found in Fusion files: $fusionFiles")
        }
    }

    val entrypointFile: FusionFile get() = fusionFiles.single { it.identifier.resourceName == entrypoint }

    override fun equals(other: Any?): Boolean = other is FusionPackageDefinition && packageName == other.packageName
    override fun hashCode(): Int = packageName.hashCode()

}