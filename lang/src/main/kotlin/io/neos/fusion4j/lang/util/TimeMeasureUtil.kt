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

import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

class TimeMeasureUtil {

    companion object {
        fun <TResult> measureTime(code: () -> TResult): ResultAndDuration<TResult> {
            val startTime = System.nanoTime()
            val result = code()
            val elapsedTime = System.nanoTime() - startTime
            return ResultAndDuration(
                result,
                elapsedTime.nanoseconds
            )
        }

    }

    data class ResultAndDuration<T>(
        val result: T,
        val duration: Duration
    ) {
        fun buildDurationMessage(description: String): String {
            val nanos = duration.inWholeNanoseconds
            return "$description took " + when {
                nanos < 1000 -> "$nanos ns"
                nanos < 1000000 -> "${duration.inWholeMicroseconds} μs"
                else -> "${duration.inWholeMilliseconds} ms"
            }
        }
    }

}