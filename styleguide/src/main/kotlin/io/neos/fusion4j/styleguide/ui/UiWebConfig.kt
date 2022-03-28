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

package io.neos.fusion4j.styleguide.ui

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.format.support.FormattingConversionService
import org.springframework.web.accept.ContentNegotiationManager
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import org.springframework.web.servlet.resource.ResourceUrlProvider

@EnableConfigurationProperties(
    StyleguideProperties::class
)
@Configuration
class UiWebConfig(
    private val styleguideProperties: StyleguideProperties
) : WebMvcConfigurationSupport() {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry
            .addResourceHandler("${styleguideProperties.assetsBaseUrl}/**")
            .addResourceLocations("${styleguideProperties.assetsResourcesPath}/", "classpath:${styleguideProperties.assetsResourcesPath}/")
        registry.setOrder(0)
    }

    override fun requestMappingHandlerMapping(
        contentNegotiationManager: ContentNegotiationManager,
        conversionService: FormattingConversionService,
        resourceUrlProvider: ResourceUrlProvider
    ): RequestMappingHandlerMapping {
        val mapping = super.requestMappingHandlerMapping(
            contentNegotiationManager,
            conversionService,
            resourceUrlProvider
        )

        mapping.order = 1
        return mapping
    }

}