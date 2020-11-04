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

import io.neos.fusion4j.lang.model.FusionPackageName
import io.neos.fusion4j.runtime.FusionContext
import io.neos.fusion4j.runtime.FusionContextLayer
import io.neos.fusion4j.spring.FusionRuntimeContainer
import io.neos.fusion4j.spring.FusionView
import io.neos.fusion4j.spring.dev.LocalDevProperties
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component
import org.springframework.web.servlet.ModelAndView

@Component
class StyleguideFusionViewService(
    private val styleguideProperties: StyleguideProperties,
    private val runtimeContainer: FusionRuntimeContainer,
    private val optionalLocalDevProperties: ObjectProvider<LocalDevProperties>
) {

    private val componentPackages: List<FusionPackageName> = styleguideProperties.componentPackages
        .map { FusionPackageName(it.packageName) }

    companion object {
        private const val ROUTER_CONTEXT_VAR_NAME = "router"
        private const val INFO_CONTEXT_VAR_NAME = "info"
    }

    fun fusionView(
        renderPath: String,
        request: HttpServletRequest,
        fusionContext: FusionContext
    ): ModelAndView =
        createFusionView(request, fusionContext) {
            FusionView.fusionView(renderPath, it)
        }

    fun defaultFusionView(
        request: HttpServletRequest,
        fusionContext: FusionContext
    ): ModelAndView =
        createFusionView(request, fusionContext, FusionView::defaultFusionView)

    private fun createFusionView(
        request: HttpServletRequest,
        fusionContext: FusionContext,
        viewFactory: (FusionContext) -> ModelAndView
    ): ModelAndView {
        val routes = StyleguideRouter.Routes.createRoutes(componentPackages)
        val router = StyleguideRouter.fromHttpServletRequest(request, routes)
        val newContext = fusionContext
            .push(
                FusionContextLayer.layerOf(
                    "styleguide-fusion-view",
                    mapOf(
                        ROUTER_CONTEXT_VAR_NAME to router,
                        INFO_CONTEXT_VAR_NAME to createInfoModel()
                    )
                )
            )
        return viewFactory(newContext)
    }

    private fun createInfoModel() = FusionMetaInfoModel(
        numberOfLoadedPrototypes = runtimeContainer.prototypeStore.size,
        numberOfIndexedPaths = runtimeContainer.rawIndex.pathIndex.size,
        numberOfStyleguideComponentPackages = styleguideProperties.componentPackages.size,
        localDevEnabled = optionalLocalDevProperties.ifUnique?.enabledWithDefault ?: false,
        lastRuntimeLoadTimeInMillis = runtimeContainer.lastRuntimeLoadTimeInMillis
    )

}