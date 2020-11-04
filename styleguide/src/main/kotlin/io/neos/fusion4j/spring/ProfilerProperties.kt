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

package io.neos.fusion4j.spring

import io.neos.fusion4j.lang.util.FusionProfiler
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "io.neos.fusion4j.profiler")
data class ProfilerProperties @ConstructorBinding constructor(
    private val enabled: Boolean?,
    private val profiles: Map<String, ProfileProperties>?
) {
    /**
     * Default false
     */
    val enabledWithDefault: Boolean = enabled ?: false

    /**
     * Default empty
     */
    val profilesWithDefault: Map<String, ProfileProperties> = profiles ?: emptyMap()

    data class ProfileProperties @ConstructorBinding constructor(
        val debugThresholdInNanos: Long?,
        val infoThresholdInNanos: Long?,
        val warnThresholdInNanos: Long?
    ) {
        /**
         * Default 100 µs
         */
        val debugThresholdInNanosWithDefault: Long = debugThresholdInNanos ?: FusionProfiler.DEFAULT_DEBUG_THRESHOLD

        /**
         * Default 5 ms
         */
        val infoThresholdInNanosWithDefault: Long = infoThresholdInNanos ?: FusionProfiler.DEFAULT_INFO_THRESHOLD

        /**
         * Default 50 ms
         */
        val warnThresholdInNanosWithDefault: Long = warnThresholdInNanos ?: FusionProfiler.DEFAULT_WARN_THRESHOLD
    }
}