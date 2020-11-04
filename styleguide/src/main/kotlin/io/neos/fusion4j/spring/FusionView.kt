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

import io.neos.fusion4j.lang.FusionError
import io.neos.fusion4j.lang.util.TimeMeasureUtil
import io.neos.fusion4j.lang.model.AbsoluteFusionPathName
import io.neos.fusion4j.runtime.FusionContext
import io.neos.fusion4j.runtime.FusionRuntime
import io.neos.fusion4j.runtime.evaluateTyped
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KLogger
import mu.KotlinLogging
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.View

private val log: KLogger = KotlinLogging.logger {}

class FusionView(
    private val runtime: FusionRuntime,
    private val fusionPath: AbsoluteFusionPathName
) : View {

    private val errorHandler = FusionErrorHandler(runtime)

    override fun render(model: MutableMap<String, *>?, request: HttpServletRequest, response: HttpServletResponse) {
        val fusionViewContext =
            when (val fusionViewContextUntyped: Any? = model?.get(FUSION_CONTEXT_MODEL_NAME)) {
                null -> {
                    log.warn { "Could not get Fusion context for Fusion view; no key '$FUSION_CONTEXT_MODEL_NAME' found in model $model" }
                    FusionContext.empty()
                }
                is FusionContext -> fusionViewContextUntyped
                else -> throw IllegalStateException("Could not render Fusion view; unsupported model type ${fusionViewContextUntyped::class.java.name}")
            }

        val fusionResponseAndTime = TimeMeasureUtil.measureTime {
            try {
                runtime.evaluateTyped<Any?>(fusionPath, fusionViewContext)
            } catch (error: FusionError) {
                errorHandler.handleFusionError("error", error, request)
            } ?: throw IllegalStateException("Fusion runtime returned null response for request: $request")
        }

        log.info { fusionResponseAndTime.buildDurationMessage("Fusion rendering of '${request.requestURI}'") }

        when (val fusionResponse = fusionResponseAndTime.result) {
            is HttpResponseImplementation.FusionHttpResponse -> fusionResponse.writeToHttpServletResponse(response)
            else -> {
                log.warn { "Fusion runtime returned a value that is no FusionHttpResponse, response type is: ${fusionResponse::class.java.name}" }
                response.writer.write(fusionResponse.toString())
                response.writer.flush()
            }
        }
    }

    companion object {
        private const val FUSION_CONTEXT_MODEL_NAME = "ctx"
        private const val DEFAULT_RENDER_PATH = "root"

        fun defaultFusionView(
            fusionContext: FusionContext
        ): ModelAndView =
            fusionView(DEFAULT_RENDER_PATH, fusionContext)

        fun fusionView(
            renderPath: String,
            fusionContext: FusionContext
        ): ModelAndView = ModelAndView(
            renderPath,
            FUSION_CONTEXT_MODEL_NAME,
            fusionContext
        )
    }

}