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

import io.neos.fusion4j.lang.model.FusionPathName.Companion.attribute
import io.neos.fusion4j.runtime.FusionObjectImplementation
import io.neos.fusion4j.runtime.FusionRuntimeImplementationAccess
import io.neos.fusion4j.runtime.evaluateRequiredAttributeOptionalValue
import io.neos.fusion4j.runtime.evaluateRequiredAttributeValue
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.function.ServerResponse

@Component
class HttpResponseImplementation : FusionObjectImplementation {
    companion object {
        private val ATTRIBUTE_STATUS_CODE = attribute("statusCode")
        private val ATTRIBUTE_HEADERS = attribute("headers")
        private val ATTRIBUTE_BODY = attribute("body")

        fun evaluateHttpResponse(runtime: FusionRuntimeImplementationAccess): FusionHttpResponse {
            val statusCode = getStatusCode(runtime)
            val headers = getHeaders(runtime)
            val body = getBody(runtime)
            return FusionHttpResponse(statusCode, headers, body)
        }

        private fun getStatusCode(runtime: FusionRuntimeImplementationAccess): Int =
            runtime.evaluateRequiredAttributeValue(ATTRIBUTE_STATUS_CODE)

        private fun getHeaders(runtime: FusionRuntimeImplementationAccess): List<Pair<String, Any?>> =
            runtime.evaluateRequiredAttributeValue(ATTRIBUTE_HEADERS)

        private fun getBody(runtime: FusionRuntimeImplementationAccess): Any? =
            runtime.evaluateRequiredAttributeOptionalValue(ATTRIBUTE_BODY)
    }

    override fun evaluate(runtime: FusionRuntimeImplementationAccess): FusionHttpResponse =
        evaluateHttpResponse(runtime)

    data class FusionHttpResponse(
        val statusCode: Int,
        val headers: List<Pair<String, Any?>>,
        val body: Any?
    ) {

        private fun handleHeaders(handler: (String, String) -> Unit) {
            headers.forEach {
                val headerValueLazy = it.second
                val headerValue = if (headerValueLazy is Lazy<*>) {
                    headerValueLazy.value
                } else {
                    headerValueLazy
                }
                if (headerValue != null) {
                    handler(it.first, headerValue.toString())
                }
            }
        }

        fun toServerResponse(): ServerResponse {
            val response = ServerResponse
                .status(statusCode)
                .headers { httpHeaders ->
                    handleHeaders(httpHeaders::set)
                }
            return if (body != null) {
                response.body(body)
            } else {
                response.build()
            }
        }

        fun writeToHttpServletResponse(
            httpServletResponse: HttpServletResponse
        ) {
            httpServletResponse.status = statusCode
            handleHeaders(httpServletResponse::setHeader)
            if (body != null) {
                httpServletResponse.writer.write(body.toString())
                httpServletResponse.writer.flush()
            }
        }
    }
}