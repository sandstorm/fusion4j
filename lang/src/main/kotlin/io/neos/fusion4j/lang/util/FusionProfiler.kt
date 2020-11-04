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

package io.neos.fusion4j.lang.util

import mu.KLogger
import mu.KotlinLogging
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds

// TODO make interface and abstract -> you may want to implement custom profiling output
class FusionProfiler(
    val enabled: Boolean,
    private val profilers: Map<String, Profiler>
) {

    companion object {
        const val LOGGER_BASE_NAME = "fusion4j.Profiler"
        val DEFAULT_DEBUG_THRESHOLD = 100.microseconds.inWholeNanoseconds
        val DEFAULT_INFO_THRESHOLD = 5.milliseconds.inWholeNanoseconds
        val DEFAULT_WARN_THRESHOLD = 50.milliseconds.inWholeNanoseconds

        const val PROFILER_EVALUATE_EEL_EXPRESSION = "EVAL_EEL"
        const val PROFILER_EVALUATE_FUSION_OBJECT = "EVAL_OBJ"
        const val PROFILER_EVALUATE_PRIMITIVE = "EVAL_PRIM"

        val DEFAULT_PROFILERS = mapOf(
            Profiler.defaultProfilerAndName(PROFILER_EVALUATE_EEL_EXPRESSION),
            Profiler.defaultProfilerAndName(PROFILER_EVALUATE_FUSION_OBJECT),
            Profiler.defaultProfilerAndName(PROFILER_EVALUATE_PRIMITIVE),
        )

        fun disabled(): FusionProfiler = FusionProfiler(false, emptyMap())

    }

    fun <T> profile(profilerName: String, description: String, code: () -> T): T {
        if (!enabled) return code()
        val profiler = profilers[profilerName]
            ?: throw IllegalArgumentException("Profiler $profilerName no found")
        val resultAndTime = TimeMeasureUtil.measureTime {
            code()
        }
        profiler.log(resultAndTime, description)
        return resultAndTime.result
    }

    class Profiler(
        private val profilerName: String,
        private val debugThresholdInNanos: Long,
        private val infoThresholdInNanos: Long,
        private val warnThresholdInNanos: Long
    ) {

        private val logger: KLogger = KotlinLogging.logger("$LOGGER_BASE_NAME.$profilerName")

        init {
            if (warnThresholdInNanos < infoThresholdInNanos) {
                throw IllegalArgumentException(
                    "Could not create profiler '$profilerName'; warn threshold must be " +
                            "greater than info threshold, but was warn: $warnThresholdInNanos, info: $infoThresholdInNanos"
                )
            }
            if (infoThresholdInNanos < debugThresholdInNanos) {
                throw IllegalArgumentException(
                    "Could not create profiler '$profilerName'; info threshold must be " +
                            "greater than debug threshold, but was warn: $warnThresholdInNanos, info: $infoThresholdInNanos"
                )
            }
        }

        fun merge(
            newDebugThresholdInNanos: Long?,
            newInfoThresholdInNanos: Long?,
            newWarnThresholdInNanos: Long?
        ): Profiler =
            Profiler(
                profilerName,
                newDebugThresholdInNanos ?: debugThresholdInNanos,
                newInfoThresholdInNanos ?: infoThresholdInNanos,
                newWarnThresholdInNanos ?: warnThresholdInNanos
            )

        companion object {
            fun defaultProfiler(profileName: String): Profiler =
                Profiler(
                    profileName,
                    DEFAULT_DEBUG_THRESHOLD,
                    DEFAULT_INFO_THRESHOLD,
                    DEFAULT_WARN_THRESHOLD
                )

            fun defaultProfilerAndName(profileName: String): Pair<String, Profiler> =
                profileName to defaultProfiler(profileName)
        }

        internal fun log(resultAndTime: TimeMeasureUtil.ResultAndDuration<*>, description: String) {
            val durationInNanos = resultAndTime.duration.inWholeNanoseconds
            val message = { resultAndTime.buildDurationMessage(description) }

            when {
                durationInNanos < debugThresholdInNanos -> {}
                durationInNanos < infoThresholdInNanos -> logger.debug(message)
                durationInNanos < warnThresholdInNanos -> logger.info(message)
                else -> logger.warn(message)
            }
        }
    }

}