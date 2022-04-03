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
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Path
import java.util.*
import kotlin.io.path.absolute
import kotlin.io.path.pathString

class FilesystemFusionFile(
    packageName: FusionPackageName,
    packagePath: Path,
    private val filePath: Path
) : FusionFile {

    override val identifier: FusionSourceFileIdentifier = createIdentifier(packageName, filePath, packagePath)

    override fun getInputStream(): InputStream = FileInputStream(filePath.toFile())

    init {
        if (!validateChildPathOf(packagePath, filePath)) {
            throw IllegalArgumentException("package path $packagePath must be a child of $filePath")
        }
    }

    override fun equals(other: Any?): Boolean =
        other is FilesystemFusionFile && Objects.equals(identifier, other.identifier)

    override fun hashCode(): Int = identifier.hashCode()

    override fun toString(): String = identifier.identifierAsString

    companion object {
        const val TYPE = "file"

        fun findAllFusionFilesInBasePath(packageName: FusionPackageName, basePath: Path): Set<FilesystemFusionFile> =
            basePath.toFile()
                .walk()
                .filter(::isFusionFile)
                .map { FilesystemFusionFile(packageName, basePath, it.toPath()) }
                .toSet()

        fun isFusionFile(file: File): Boolean =
            file.isFile && file.extension == "fusion"

        fun createIdentifier(packageName: FusionPackageName, filePath: Path, packagePath: Path) =
            FusionSourceFileIdentifier(
                TYPE,
                packageName,
                FusionResourceName(packagePath.relativize(filePath).pathString),
                mapOf(
                    "file" to filePath
                        .absolute()
                        .normalize()
                        .pathString
                )
            )

        fun classpathToFilesystemIdentifier(
            packageBasePath: Path,
            source: FusionSourceFileIdentifier
        ): FusionSourceFileIdentifier {
            if (source.type != ClasspathFusionFile.TYPE) {
                throw IllegalArgumentException("Could not convert identifier $source to filesystem identifier; type must be '$TYPE' but was '${source.type}'")
            } else {
                return createIdentifier(
                    source.packageName,
                    packageBasePath.resolve(source.resourceName.name),
                    packageBasePath
                )
            }
        }

        private fun validateChildPathOf(parent: Path, child: Path?): Boolean {
            if (child == null) return false
            val parentNormalized = parent.absolute().normalize()
            val childNormalized = child.absolute().normalize()
            if (parentNormalized == childNormalized) return true
            return validateChildPathOf(parent, child.parent)
        }
    }
}