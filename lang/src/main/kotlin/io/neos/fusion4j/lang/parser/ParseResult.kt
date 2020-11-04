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

package io.neos.fusion4j.lang.parser

import io.neos.fusion4j.lang.file.FusionFile
import io.neos.fusion4j.lang.model.decl.RootFusionDecl

/**
 * Parsed Fusion file which is not post-processed/normalized in any way:
 * - prototype inheritance is not applied yet
 * - loading order is not applied yet (between packages, files, file includes)
 * - includes are not resolved yet.
 *
 * This data still includes the AST/file references (useful for stack traces etc).
 *
 * This is the result of [FusionLang::parseFusionPackages]. This data class is then wrapped in [io.neos.fusion4j.runtime.DefaultFusionRuntimeState],
 * which is needed for instantiating the runtime.
 */
data class ParseResult(
    val input: FusionFile,
    val success: RootFusionDecl?,
    val error: FusionParseException?
) {
    companion object {
        fun success(input: FusionFile, parsedFusion: RootFusionDecl): ParseResult = ParseResult(
            input,
            parsedFusion,
            null
        )

        fun parseError(input: FusionFile, error: FusionParseException): ParseResult = ParseResult(
            input,
            null,
            error
        )
    }
}
