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

import io.neos.fusion4j.runtime.FusionContextLayer
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.ModelAndView

@ControllerAdvice
class UiErrorHandler(
    private val viewService: StyleguideFusionViewService
) {

    companion object {
        private const val NOT_FOUND_ERROR_PATH = "notFound"
    }

    @ExceptionHandler(StyleguideRouteNotFoundException::class)
    fun handleNotFoundError(
        notFoundException: StyleguideRouteNotFoundException,
        request: HttpServletRequest
    ): ModelAndView =
        viewService.fusionView(
            NOT_FOUND_ERROR_PATH,
            request,
            notFoundException.additionalErrorPageContext
                .push(
                    FusionContextLayer.layerOf(
                        "ui-error-handler",
                        mapOf(
                            "notFoundMessage" to notFoundException.uiErrorMessage
                        )
                    )
                )
        )

}