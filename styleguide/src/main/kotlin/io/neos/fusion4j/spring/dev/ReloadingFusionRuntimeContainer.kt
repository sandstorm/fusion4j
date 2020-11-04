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

import io.neos.fusion4j.lang.util.TimeMeasureUtil
import io.neos.fusion4j.runtime.FusionObjectImplementationFactory
import io.neos.fusion4j.runtime.FusionRuntime
import io.neos.fusion4j.runtime.eel.EelHelperFactory
import io.neos.fusion4j.spring.FusionRuntimeContainer
import io.neos.fusion4j.spring.FusionRuntimeFactory
import io.neos.fusion4j.spring.RuntimeProperties
import mu.KLogger
import mu.KotlinLogging

private val log: KLogger = KotlinLogging.logger {}

class ReloadingFusionRuntimeContainer(
    private val classLoader: ClassLoader,
    private val runtimeProperties: RuntimeProperties,
    private val runtimeFactory: FusionRuntimeFactory,
    private val eelHelperFactory: EelHelperFactory,
    private val fusionObjectImplementationFactory: FusionObjectImplementationFactory
) : FusionRuntimeContainer {

    private var currentRuntime: FusionRuntime
    private var lastRuntimeReloadTimeInMillis: Long

    init {
        log.info { "creating initial Fusion runtime" }
        val runtimeAndDuration = createRuntime()
        currentRuntime = runtimeAndDuration.result
        lastRuntimeReloadTimeInMillis = runtimeAndDuration.duration.inWholeMilliseconds
    }

    private fun createRuntime(): TimeMeasureUtil.ResultAndDuration<out FusionRuntime> =
        runtimeFactory.createRuntime(
            classLoader,
            runtimeProperties,
            eelHelperFactory,
            fusionObjectImplementationFactory
        )

    @Synchronized
    fun reloadRuntime() {
        log.info { "reloading Fusion runtime ..." }
        try {
            val runtimeAndDuration = createRuntime()
            currentRuntime = runtimeAndDuration.result
            lastRuntimeReloadTimeInMillis = runtimeAndDuration.duration.inWholeMilliseconds
        } catch (error: Throwable) {
            log.error("Could not reload Fusion runtime", error)
            currentRuntime
        }
    }

    override val runtime: FusionRuntime
        @Synchronized get() = currentRuntime

    override val lastRuntimeLoadTimeInMillis: Long
        @Synchronized get() = lastRuntimeReloadTimeInMillis

}