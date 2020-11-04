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

import mu.KLogger
import mu.KotlinLogging
import org.springframework.context.ApplicationContext

private val log: KLogger = KotlinLogging.logger {}

class SpringBeanUtil {

    companion object {
        fun <T> getSingleBeanOrFallback(
            description: String,
            applicationContext: ApplicationContext,
            beanClass: Class<*>,
            beanMapper: (Any) -> T,
            fallbackCode: () -> T,
        ): T {
            val helperBeans = applicationContext.getBeansOfType(beanClass)
            return when (helperBeans.size) {
                // single bean found
                1 -> {
                    val bean: Any = helperBeans.entries.single().value
                    beanMapper(bean)
                }
                // fallback
                0 -> {
                    log.debug { "No Spring bean found for $description class ${beanClass.name}; fallback to default factory" }
                    fallbackCode()
                }
                else -> throw IllegalStateException(
                    "Could not create $description; bean class ${beanClass.name} must" +
                            "be unique but multiple beans (${helperBeans.size}) found: $helperBeans"
                )
            }
        }

        fun getSingleBeanOrFallback(
            description: String,
            applicationContext: ApplicationContext,
            beanClass: Class<*>,
            fallbackCode: () -> Any,
        ): Any = getSingleBeanOrFallback(description, applicationContext, beanClass, { it }, fallbackCode)
    }

}