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

import io.neos.fusion4j.lang.model.EelHelperContextName
import io.neos.fusion4j.runtime.eel.ClassLoadingEelHelperFactory
import io.neos.fusion4j.runtime.eel.EelHelperFactory
import org.springframework.context.ApplicationContext

class SpringEelHelperFactory(
    private val applicationContext: ApplicationContext,
    private val classMapping: Map<EelHelperContextName, Class<out Any>>
) : EelHelperFactory {
    private val defaultFactory: EelHelperFactory =
        ClassLoadingEelHelperFactory(
            classMapping
        )

    override fun createEelHelperInstance(contextName: EelHelperContextName): Any {
        val eelHelperClass = classMapping[contextName]
            ?: throw IllegalArgumentException(
                "Could not create EEL helper '$contextName'; no EEL helper " +
                        "class mapping found in: $classMapping"
            )
        return SpringBeanUtil.getSingleBeanOrFallback(
            "EEL helper '$contextName'",
            applicationContext,
            eelHelperClass
        ) {
            defaultFactory.createEelHelperInstance(contextName)
        }
    }

    override fun exists(contextName: EelHelperContextName): Boolean =
        defaultFactory.exists(contextName)
}