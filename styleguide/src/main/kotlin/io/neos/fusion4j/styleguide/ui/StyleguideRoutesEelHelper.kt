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

import io.neos.fusion4j.lang.annotation.EelHelper
import io.neos.fusion4j.lang.annotation.FusionApi

@EelHelper("StyleguideRoutes")
class StyleguideRoutesEelHelper {

    @FusionApi
    fun overview(): String =
        StyleguideRouter.RouteNames.OVERVIEW

    @FusionApi
    fun overviewUrl(): String =
        StyleguideRouter.RouteUrls.OVERVIEW

    @FusionApi
    fun fusionModel(): String =
        StyleguideRouter.RouteNames.FUSION_MODEL

    @FusionApi
    fun fusionModelUrl(): String =
        StyleguideRouter.RouteUrls.FUSION_MODEL

    @FusionApi
    fun fusionPrototypeStore(): String =
        StyleguideRouter.RouteNames.FUSION_MODEL_PROTOTYPE_STORE

    @FusionApi
    fun fusionPrototypeStoreUrl(): String =
        StyleguideRouter.RouteUrls.FUSION_MODEL_PROTOTYPE_STORE

    @FusionApi
    fun fusionPathIndex(): String =
        StyleguideRouter.RouteNames.FUSION_MODEL_PATH_INDEX

    @FusionApi
    fun fusionPathIndexUrl(): String =
        StyleguideRouter.RouteUrls.FUSION_MODEL_PATH_INDEX

    @FusionApi
    fun fusionParser(): String =
        StyleguideRouter.RouteNames.FUSION_MODEL_PARSER

    @FusionApi
    fun fusionParserUrl(): String =
        StyleguideRouter.RouteUrls.FUSION_MODEL_PARSER

    @FusionApi
    fun fusionAfx(): String =
        StyleguideRouter.RouteNames.FUSION_MODEL_AFX

    @FusionApi
    fun fusionAfxUrl(): String =
        StyleguideRouter.RouteUrls.FUSION_MODEL_AFX

    @FusionApi
    fun styleguide(): String =
        StyleguideRouter.RouteNames.STYLEGUIDE

    @FusionApi
    fun styleguideUrl(): String =
        StyleguideRouter.RouteUrls.STYLEGUIDE

}