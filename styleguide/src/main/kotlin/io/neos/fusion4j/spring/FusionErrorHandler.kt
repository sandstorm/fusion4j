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
import io.neos.fusion4j.lang.model.AbsoluteFusionPathName
import io.neos.fusion4j.lang.model.FusionPathName
import io.neos.fusion4j.runtime.FusionContext
import io.neos.fusion4j.runtime.FusionRuntime
import io.neos.fusion4j.runtime.evaluateTyped
import mu.KLogger
import mu.KotlinLogging
import javax.servlet.http.HttpServletRequest

private val log: KLogger = KotlinLogging.logger {}

class FusionErrorHandler(
    private val runtime: FusionRuntime
) {

    fun handleFusionError(
        errorRenderPath: String,
        error: FusionError,
        httpRequest: HttpServletRequest
    ): HttpResponseImplementation.FusionHttpResponse {
        log.error("Fusion runtime error: $error", error)
        return renderErrorResponseBody(
            FusionPathName.parseAbsolute(errorRenderPath),
            unpackErrorMessages(error),
            error.stackTraceToString(),
            httpRequest
        )
    }

    private fun unpackErrorMessages(error: Throwable, level: Int = 0): String {
        val cause = error.cause
        return error.message + if (cause != null) {
            "\n ${" ".repeat(level)}- " + unpackErrorMessages(cause, level + 1)
        } else {
            ""
        }
    }

    private fun renderErrorResponseBody(
        errorRenderPath: AbsoluteFusionPathName,
        errorMessage: String,
        stacktrace: String,
        httpRequest: HttpServletRequest
    ): HttpResponseImplementation.FusionHttpResponse {
        val errorContext = FusionContext.create(
            mapOf(
                "errorMessage" to errorMessage,
                "stacktrace" to stacktrace,
                "requestUri" to httpRequest.requestURI
            )
        )
        val fallbackErrorProvider = { error: Throwable? ->
            HttpResponseImplementation.FusionHttpResponse(
                500,
                headers = listOf(
                    "content-type" to "text/html"
                ),
                fallbackError(errorMessage, stacktrace, httpRequest, error)
            )
        }
        return try {
            runtime.evaluateTyped(errorRenderPath, errorContext)
                ?: fallbackErrorProvider(null)
        } catch (error: Throwable) {
            log.error("Could not render Fusion error page:", error)
            fallbackErrorProvider(error)
        }
    }

    private fun fallbackError(
        errorMessage: String,
        stacktrace: String,
        request: HttpServletRequest,
        criticalError: Throwable? = null
    ): String {
        val criticalPart = if (criticalError != null) {
            val criticalErrorMessage = unpackErrorMessages(criticalError)
            val criticalStacktrace = criticalError.stackTraceToString()
            """
                <h1>Error page could not be rendered</h1>
                <div>
                    <h2>Critical message:</h2>
                    <pre>$criticalErrorMessage</pre>
                </div>
                <div>
                    <h2>Stacktrace:</h2>
                    <pre>$criticalStacktrace</pre>
                </div>
            """.trimIndent()
        } else {
            ""
        }
        val requestUri = request.requestURI

        return """
            <html>
                <head>
                    <title>Critical Fusion Error</title>
                </head>
                <body>
                    <div>
                        <h1>Critical Fusion Error</h1>
                        <p>URI: $requestUri</p>
                        <div>
                            <h2>Message:</h2>
                            <pre>$errorMessage</pre>
                        </div>
                        <div>
                            <h2>Stacktrace:</h2>
                            <pre>$stacktrace</pre>
                        </div>
                        $criticalPart
                    </div>
                </body>
            </html>
        """.trimIndent()
    }

}