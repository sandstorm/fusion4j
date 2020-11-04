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

package io.neos.fusion4j.test.bdd.impl

import io.neos.fusion4j.lang.model.FusionPathName
import io.neos.fusion4j.runtime.FusionObjectImplementation
import io.neos.fusion4j.runtime.FusionRuntime
import io.neos.fusion4j.runtime.FusionRuntimeImplementationAccess
import io.neos.fusion4j.runtime.evaluateAttribute

@Suppress("unused")
class TestPrintDataStructureImplementation : FusionObjectImplementation {
    override fun evaluate(runtime: FusionRuntimeImplementationAccess): Any {
        val attribute = runtime.propertyAttributes[PATH_DATA]
        return if (attribute != null) {
            val dataEvalResult = runtime.evaluateAttribute<Any?>(attribute)
            if (!dataEvalResult.cancelled) {
                when (val data = dataEvalResult.lazyValue.value) {
                    is List<*> -> print(data)
                    null -> "[RESOLVED DATA IS NULL]"
                    else -> "[RESOLVED DATA IS NO LIST: ${data::class.java}]"
                }
            } else {
                "[RESOLVED DATA IS CANCELLED]"
            }
        } else {
            "[NO '${PATH_DATA.propertyName}' PROPERTY DECLARED]"
        }
    }

    private fun print(data: List<*>, level: Int = 0): String =
        data
            .map {
                if (it is Pair<*, *>) {
                    it.first to FusionRuntime.unwrapLazy(it.second)
                } else {
                    it
                }
            }
            .joinToString("\n") {
                val indent = " ".repeat(level * 4)
                indent + if (it is Pair<*, *>) {
                    when (val value = it.second) {
                        is List<*> -> "${it.first} {\n$indent" + print(value, level + 1) + "$indent\n}"
                        else -> "${it.first} = ${it.second}"
                    }
                } else {
                    it.toString()
                }
            }

    companion object {
        val PATH_DATA = FusionPathName.attribute("data")
    }
}