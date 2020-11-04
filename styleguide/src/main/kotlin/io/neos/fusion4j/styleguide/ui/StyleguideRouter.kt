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

import io.neos.fusion4j.lang.annotation.FusionApi
import io.neos.fusion4j.lang.model.FusionPackageName
import io.neos.fusion4j.runtime.model.FusionDataStructure
import jakarta.servlet.http.HttpServletRequest
import mu.KLogger
import mu.KotlinLogging

private val log: KLogger = KotlinLogging.logger {}

data class StyleguideRouter(
    @FusionApi
    val routes: FusionDataStructure<Route>,
    @FusionApi
    val currentRoute: Route?,
    private val routeDefinitions: FusionDataStructure<RouteDefinition>,
    private val httpRequest: HttpServletRequest,
    private val routesByUrl: Map<String, Route>,
    val routesByName: Map<String, Route>
) {

    @FusionApi
    val requestUri: String = httpRequest.requestURI

    @FusionApi
    val referrerUrl: String? =
        httpRequest.getHeader("Referer")

    @FusionApi
    val referrer: String? = referrerUrl

    @FusionApi
    fun componentPackageRouteUrl(packageName: String): String {
        val packageRouteName = RouteNames.packageNameToRouteName(packageName)
        val route = routesByName[packageRouteName]
            ?: throw IllegalArgumentException(
                "Could not create route to styleguide component package '$packageName'; " +
                        "route '$packageRouteName' does not exist"
            )
        return route.url
    }

    interface RouteNames {
        companion object {
            const val OVERVIEW = "overview"
            const val STYLEGUIDE = "styleguide"
            const val FUSION_MODEL = "fusion"
            const val FUSION_MODEL_PROTOTYPE_STORE = "fusionPrototypeStore"
            const val FUSION_MODEL_PATH_INDEX = "fusionPathIndex"
            const val FUSION_MODEL_PARSER = "fusionParser"
            const val FUSION_MODEL_AFX = "fusionAfx"

            internal fun packageNameToRouteName(packageName: String): String =
                STYLEGUIDE + "_" + packageName
                    .lowercase()
                    .replace('.', '_')
        }
    }

    interface RouteUrls {
        companion object {
            const val OVERVIEW = "/overview"
            const val STYLEGUIDE = "/styleguide"

            internal fun styleguidePackageUrl(packageName: String): String =
                "$STYLEGUIDE/${packageNameToUrlSlug(packageName)}"

            const val FUSION_MODEL = "/fusion"
            const val FUSION_MODEL_PROTOTYPE_STORE = "/fusion/prototype-store"
            const val FUSION_MODEL_PATH_INDEX = "/fusion/path-index"
            const val FUSION_MODEL_PARSER = "/fusion/parser"
            const val FUSION_MODEL_AFX = "/fusion/afx"

            fun packageNameToUrlSlug(value: String): String =
                value
                    .replace(".", "-")
                    .lowercase()
        }
    }

    interface Routes {
        companion object {
            val OVERVIEW: RouteDefinition = RouteNames.OVERVIEW.routeDefinition(
                "Overview",
                RouteUrls.OVERVIEW
            )

            val FUSION_MODEL_PROTOTYPE_STORE = RouteNames.FUSION_MODEL_PROTOTYPE_STORE.routeDefinition(
                "Prototype Store",
                RouteUrls.FUSION_MODEL_PROTOTYPE_STORE
            )
            val FUSION_MODEL_PATH_INDEX = RouteNames.FUSION_MODEL_PATH_INDEX.routeDefinition(
                "Path Index",
                RouteUrls.FUSION_MODEL_PATH_INDEX
            )
            val FUSION_MODEL_PARSER = RouteNames.FUSION_MODEL_PARSER.routeDefinition(
                "Parser",
                RouteUrls.FUSION_MODEL_PARSER
            )
            val FUSION_MODEL_AFX = RouteNames.FUSION_MODEL_AFX.routeDefinition(
                "AFX",
                RouteUrls.FUSION_MODEL_AFX
            )

            val FUSION_MODEL: RouteDefinition = RouteNames.FUSION_MODEL.routeDefinition(
                "Fusion Model",
                RouteUrls.FUSION_MODEL,
                listOf(
                    FUSION_MODEL_PROTOTYPE_STORE,
                    FUSION_MODEL_PATH_INDEX,
                    FUSION_MODEL_PARSER,
                    FUSION_MODEL_AFX
                )
            )

            fun createStyleguideRoute(
                componentPackages: List<FusionPackageName>
            ): RouteDefinition =
                RouteNames.STYLEGUIDE.routeDefinition(
                    "Styleguide",
                    RouteUrls.STYLEGUIDE,
                    componentPackages.map { componentPackage ->
                        val packageRouteName = RouteNames.packageNameToRouteName(componentPackage.name)
                        packageRouteName.routeDefinition(
                            RouteNames.STYLEGUIDE,
                            componentPackage.name,
                            RouteUrls.styleguidePackageUrl(componentPackage.name)
                        )
                    }
                )

            fun createRoutes(
                componentPackages: List<FusionPackageName>
            ): List<RouteDefinition> = listOf(
                OVERVIEW,
                createStyleguideRoute(componentPackages),
                FUSION_MODEL
            )
        }
    }

    companion object {
        fun fromHttpServletRequest(
            httpRequest: HttpServletRequest,
            routeDefinitions: List<RouteDefinition>
        ): StyleguideRouter {
            log.debug { "Creating router for request $httpRequest; routes: $routeDefinitions" }

            val currentUrl = httpRequest.requestURI
            val routes = FusionDataStructure(
                routeDefinitions
                    .map { it.routeName to it.initializeRoute(currentUrl) }
            )

            val allRoutesFlat = routes.data
                .flatMap { it.second.subRoutes.values + it.second }

            val routesByUrl = allRoutesFlat
                .associateBy { it.url }
            if (routesByUrl.size != allRoutesFlat.size) {
                throw IllegalStateException("Non unique route URLs: " + allRoutesFlat.map { it.url })
            }

            val routesByName = allRoutesFlat
                .associateBy { it.name }
            if (routesByName.size != allRoutesFlat.size) {
                throw IllegalStateException("Non unique route names: " + allRoutesFlat.map { it.name })
            }

            val currentRoute = routesByUrl[currentUrl] ?: run {
                log.warn { "No styleguide route found for request $currentUrl" }
                null
            }

            val routesData = FusionDataStructure(routeDefinitions.map { it.routeName to it })
            return StyleguideRouter(
                routes,
                currentRoute,
                routesData,
                httpRequest,
                routesByUrl,
                routesByName
            )
        }

        fun String.routeDefinition(
            parentRouteName: String?,
            title: String,
            url: String,
            subRoutes: List<RouteDefinition> = emptyList()
        ): RouteDefinition =
            RouteDefinition(this, parentRouteName, title, url, FusionDataStructure(subRoutes.map { it.routeName to it }))

        fun String.routeDefinition(
            title: String,
            url: String,
            subRoutes: List<RouteDefinition> = emptyList()
        ): RouteDefinition =
            routeDefinition(null, title, url, subRoutes)
    }

    data class RouteDefinition(
        val routeName: String,
        val parentRouteName: String?,
        val title: String,
        val url: String,
        val subRouteDefinitions: FusionDataStructure<RouteDefinition>
    ) {

        fun initializeRoute(currentUrl: String): Route =
            Route(
                routeName,
                parentRouteName,
                title,
                url,
                isRouteOrSubRouteActive(currentUrl),
                FusionDataStructure(
                    subRouteDefinitions.data.map { it.first to it.second.initializeRoute(currentUrl) }
                )
            )

        private fun isRouteOrSubRouteActive(currentUrl: String): Boolean {
            // current route
            if (url == currentUrl) {
                return true
            }
            // sub route
            if (subRouteDefinitions.valueList.any { it.isRouteOrSubRouteActive(currentUrl) }) {
                return true
            }
            return false
        }

    }

    @FusionApi
    data class Route(
        @FusionApi
        val name: String,
        @FusionApi
        val parentRouteName: String?,
        @FusionApi
        val title: String,
        @FusionApi
        val url: String,
        @FusionApi
        val active: Boolean,
        @FusionApi
        val subRoutes: FusionDataStructure<Route>
    ) {
        @FusionApi
        fun getIsGroup(): Boolean = subRoutes.isNotEmpty()
    }
}