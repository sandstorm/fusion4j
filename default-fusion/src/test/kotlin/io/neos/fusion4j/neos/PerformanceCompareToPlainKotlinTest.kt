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

import org.junit.Ignore
import org.junit.Test
import java.io.StringWriter

@Ignore
class PerformanceCompareToPlainKotlinTest {

    @Test
    fun test_stringLoop() {
        testWithTiming("string concat") {
            var value = ""
            (0..100000).forEach {
                value += "foo bar $it"
            }
            println("${value.length} bytes")
        }
    }

    @Test
    fun teststring() {
        testWithTiming("joinToString") {
            println(
                (1..100000)
                    .joinToString {
                        "$it aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                    }
                    .length
            )
        }
    }

    @Test
    fun test2() {
        testWithTiming("string writer") {
            val writer = StringWriter()
            for (idx in (1..100000)) {
                writer.append("$idx aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
            }
            println(writer.toString().length)
        }
    }

}

private inline fun <reified TResult> testWithTiming(description: String, action: () -> TResult): TResult {
    val start = System.currentTimeMillis()
    val result = action()
    val duration = System.currentTimeMillis() - start
    println("$description took $duration ms")
    return result
}