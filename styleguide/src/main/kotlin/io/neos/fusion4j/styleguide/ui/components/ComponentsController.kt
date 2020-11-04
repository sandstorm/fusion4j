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

package io.neos.fusion4j.styleguide.ui.components

import io.neos.fusion4j.runtime.FusionContext
import io.neos.fusion4j.spring.FusionRuntimeContainer
import io.neos.fusion4j.styleguide.ui.StyleguideFusionViewService
import io.neos.fusion4j.styleguide.ui.StyleguideProperties
import io.neos.fusion4j.styleguide.ui.StyleguideRouteNotFoundException
import io.neos.fusion4j.styleguide.ui.StyleguideRouter
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.servlet.ModelAndView

@Controller
class ComponentsController(
    private val styleguideProperties: StyleguideProperties,
    private val fusionRuntimeContainer: FusionRuntimeContainer,
    private val styleguideView: StyleguideFusionViewService
) {

    @GetMapping("${StyleguideRouter.RouteUrls.STYLEGUIDE}/{packageSlug}")
    fun detailPage(
        @PathVariable("packageSlug")
        packageSlug: String,
        request: HttpServletRequest
    ): ModelAndView {
        val packageConfig = styleguideProperties.componentPackages
            .find { StyleguideRouter.RouteUrls.packageNameToUrlSlug(it.packageName) == packageSlug }
            ?: throw StyleguideRouteNotFoundException("component package '$packageSlug' not found")

        val packageModel = ComponentPackageModel(
            name = packageConfig.packageName,
            prototypes = fusionRuntimeContainer.prototypeStore.prototypeNames
                .filter { it.namespace.name == packageConfig.packageName }
                .map { it.qualifiedName }
        )

        return styleguideView.defaultFusionView(
            request,
            FusionContext.create(
                mapOf(
                    "componentPackage" to packageModel
                )
            )
        )
    }

}