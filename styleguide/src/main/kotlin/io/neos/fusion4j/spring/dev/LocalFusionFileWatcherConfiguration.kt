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

import io.neos.fusion4j.runtime.FusionObjectImplementationFactory
import io.neos.fusion4j.runtime.eel.EelHelperFactory
import io.neos.fusion4j.spring.FusionRuntimeContainer
import io.neos.fusion4j.spring.FusionRuntimeFactory
import io.neos.fusion4j.spring.RuntimeProperties
import mu.KLogger
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.util.ClassUtils
import javax.annotation.PostConstruct

private val log: KLogger = KotlinLogging.logger {}

@EnableConfigurationProperties(
    LocalDevProperties::class
)
@Configuration
@ConditionalOnProperty("io.neos.fusion4j.local-dev.enabled")
class LocalFusionFileWatcherConfiguration {

    @PostConstruct
    fun logLocalDevWarning() {
        log.warn { "############################################################################" }
        log.warn { "###   Running Fusion runtime in local dev mode - DISABLE IN PRODUCTION   ###" }
        log.warn { "############################################################################" }
    }

    @Bean
    fun fusionFileWatcher(
        localDevProperties: LocalDevProperties,
        fusionRuntimeContainer: FusionRuntimeContainer
    ): LocalFusionFileWatcher {
        if (fusionRuntimeContainer !is ReloadingFusionRuntimeContainer) {
            throw IllegalStateException("Could not create Fusion file watcher; local dev mode only supports container class: ${ReloadingFusionRuntimeContainer::class.java.name}")
        }
        val watcher = LocalFusionFileWatcher.initialize(localDevProperties, fusionRuntimeContainer)
        watcher.startFusionFileWatcher()
        return watcher
    }

    @Bean
    @Primary
    fun reloadingFusionRuntimeContainer(
        runtimeProperties: RuntimeProperties,
        runtimeFactory: FusionRuntimeFactory,
        eelHelperFactory: EelHelperFactory,
        fusionObjectImplementationFactory: FusionObjectImplementationFactory
    ): FusionRuntimeContainer {
        log.info { "Creating LOCAL DEVELOPMENT Fusion runtime with hot-code reloading" }
        return ReloadingFusionRuntimeContainer(
            ClassUtils.getDefaultClassLoader()!!,
            runtimeProperties,
            runtimeFactory,
            eelHelperFactory,
            fusionObjectImplementationFactory
        )
    }

}