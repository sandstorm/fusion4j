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

package io.neos.fusion4j.spring.dev

import io.neos.fusion4j.lang.file.FilesystemFusionFile
import jakarta.annotation.PreDestroy
import mu.KLogger
import mu.KotlinLogging
import org.springframework.boot.devtools.filewatch.ChangedFile
import org.springframework.boot.devtools.filewatch.ChangedFiles
import org.springframework.boot.devtools.filewatch.FileChangeListener
import org.springframework.boot.devtools.filewatch.FileSystemWatcher
import java.io.File
import java.time.Duration
import kotlin.io.path.Path

private val log: KLogger = KotlinLogging.logger {}

class LocalFusionFileWatcher(
    private val watcher: FileSystemWatcher,
    private val watchDirectories: Set<File>,
    private val reloadingRuntime: ReloadingFusionRuntimeContainer
) : FileChangeListener {

    override fun onChange(changeSet: MutableSet<ChangedFiles>) {
        log.debug { "Fusion file change triggered: $changeSet" }
        val changedFusionPackages: Set<ChangedFusionPackage> = changeSet
            .filter { it.files.isNotEmpty() }
            .mapNotNull { files ->
                val fusionFiles = files.files
                    .filter { FilesystemFusionFile.isFusionFile(it.file) }
                    .toSet()
                if (fusionFiles.isNotEmpty()) {
                    ChangedFusionPackage(files.sourceDirectory, fusionFiles)
                } else {
                    null
                }
            }
            .toSet()

        changedFusionPackages.forEach { changedPackage ->
            log.info { "Fusion files changed for $changedPackage " }
        }
        if (changedFusionPackages.isNotEmpty()) {
            reloadingRuntime.reloadRuntime()
        }
    }

    internal fun startFusionFileWatcher() {
        if (watchDirectories.isNotEmpty()) {
            log.info { "Start watching Fusion file directories\n  - " + watchDirectories.joinToString("\n  - ") { it.path } }
            watcher.start()
        } else {
            log.info { "No directories configured for watching local dev Fusion files" }
        }
    }

    @PreDestroy
    fun stopFusionFileWatcher() {
        if (watchDirectories.isNotEmpty()) {
            log.info { "stopping Fusion file watcher" }
            watcher.stop()
        }
    }

    internal data class ChangedFusionPackage(
        val sourceDirectory: File,
        val changedFiles: Set<ChangedFile>
    ) {
        override fun toString(): String =
            "package path $sourceDirectory:\n  - " + changedFiles.joinToString("\n  - ") { it.file.path }
    }

    companion object {
        fun initialize(
            localDevProperties: LocalDevProperties,
            fusionRuntimeContainer: ReloadingFusionRuntimeContainer
        ): LocalFusionFileWatcher {
            val watcher = FileSystemWatcher(
                true,
                Duration.ofMillis(localDevProperties.watcherPollIntervalInMillisWithDefault),
                Duration.ofMillis(localDevProperties.watcherQuietPeriodInMillisWithDefault)
            )
            val watchDirectories = localDevProperties.fileSystemPackagesWithDefault.values
                .map { Path(it).toFile() }
                .toSet()
            val watcherService = LocalFusionFileWatcher(watcher, watchDirectories, fusionRuntimeContainer)
            watcher.addListener(watcherService)
            watcher.addSourceDirectories(
                watchDirectories
            )
            return watcherService
        }
    }

}