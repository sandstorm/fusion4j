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
import io.neos.fusion4j.runtime.FusionObjectImplementationFactory
import io.neos.fusion4j.runtime.eel.EelHelperFactory
import io.neos.fusion4j.spring.dev.LocalDevProperties
import io.neos.fusion4j.spring.dev.LocalFusionFileWatcherConfiguration
import mu.KLogger
import mu.KotlinLogging
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.ClassUtils
import org.springframework.web.servlet.ViewResolver

private val log: KLogger = KotlinLogging.logger {}

@Configuration
@EnableConfigurationProperties(
    ParserProperties::class,
    ProfilerProperties::class,
    SemanticProperties::class,
    RuntimeProperties::class
)
class FusionSpringConfiguration {

    @Bean
    fun fusionProfiler(
        profilerProperties: ProfilerProperties
    ): FusionProfiler {
        log.debug { "Creating default Fusion profiler ..." }

        return if (profilerProperties.enabledWithDefault) {
            val mergedProfilers = profilerProperties.profilesWithDefault.entries
                // merge default profilers with spring config
                .fold(FusionProfiler.DEFAULT_PROFILERS) { result, current ->
                    val existing = result[current.key]
                    result + if (existing == null) {
                        val newProfiler = FusionProfiler.Profiler(
                            current.key,
                            current.value.debugThresholdInNanosWithDefault,
                            current.value.infoThresholdInNanosWithDefault,
                            current.value.warnThresholdInNanosWithDefault
                        )
                        current.key to newProfiler
                    } else {
                        val mergedProfiler = existing.merge(
                            current.value.debugThresholdInNanos,
                            current.value.infoThresholdInNanos,
                            current.value.warnThresholdInNanos
                        )
                        current.key to mergedProfiler
                    }
                }
                .onEach {
                    log.info { "Configured Fusion profiler '${it.key}' with config: ${it.value}" }
                }
            log.info { "Created default Fusion profiler" }
            FusionProfiler(
                profilerProperties.enabledWithDefault,
                mergedProfilers
            )
        } else {
            log.info { "Fusion profiling is disabled" }
            FusionProfiler.disabled()
        }
    }

    @Bean
    fun fusionRuntimeFactory(
        parserProperties: ParserProperties,
        semanticProperties: SemanticProperties,
        customDsls: List<CustomFusionDsl>,
        optionalLocalDevProperties: ObjectProvider<LocalDevProperties>,
        fusionProfiler: FusionProfiler
    ): FusionRuntimeFactory {
        log.debug { "Creating default Fusion runtime container factory ..." }
        val localDevProperties = optionalLocalDevProperties.ifAvailable

        val factory = FusionRuntimeFactory.initialize(
            parserProperties,
            semanticProperties,
            fusionProfiler,
            customDsls,
            localDevProperties?.fileSystemPackagesWithDefault ?: emptyMap(),
            ClassUtils.getDefaultClassLoader()!!
        )
        log.debug { "Created default Fusion runtime container factory $factory" }
        return factory
    }

    @Bean
    fun eelHelperFactory(
        applicationContext: ApplicationContext,
        runtimeContainerFactory: FusionRuntimeFactory
    ): EelHelperFactory {
        log.info { "Creating default spring EEL helper factory" }
        return SpringEelHelperFactory(
            applicationContext,
            runtimeContainerFactory.eelHelpers
        )
    }

    @Bean
    fun fusionObjectImplementationFactory(
        applicationContext: ApplicationContext
    ): FusionObjectImplementationFactory {
        log.info { "Creating default spring Fusion object implementation factory" }
        return SpringFusionObjectImplementationFactory(
            applicationContext,
            ClassUtils.getDefaultClassLoader()!!
        )
    }

    @Bean
    @ConditionalOnMissingBean(LocalFusionFileWatcherConfiguration::class)
    fun immutableFusionRuntimeContainer(
        runtimeProperties: RuntimeProperties,
        runtimeFactory: FusionRuntimeFactory,
        eelHelperFactory: EelHelperFactory,
        fusionObjectImplementationFactory: FusionObjectImplementationFactory
    ): FusionRuntimeContainer {
        log.info { "###############################################################" }
        log.info { "###   Running immutable Fusion runtime in production mode   ###" }
        log.info { "###############################################################" }
        return ImmutableFusionRuntimeContainer(
            runtimeFactory.createRuntime(
                ClassUtils.getDefaultClassLoader()!!,
                runtimeProperties,
                eelHelperFactory,
                fusionObjectImplementationFactory
            )
        )
    }

    @Bean
    fun fusionViewResolver(runtimeContainer: FusionRuntimeContainer): ViewResolver {
        return FusionViewResolver(runtimeContainer)
    }

}