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

package io.neos.fusion4j.lang.util

import io.neos.fusion4j.lang.file.FusionSourceIdentifier
import io.neos.fusion4j.lang.parser.FusionSyntaxError

fun toReadableParseExceptionOutput(source: FusionSourceIdentifier, errors: List<FusionSyntaxError>) =
    templateParseException(source, toReadableSyntaxErrors(errors))

fun toReadableSyntaxErrorOutput(syntaxError: FusionSyntaxError) =
    templateSyntaxError(syntaxError)

private fun toReadableSyntaxErrors(errors: List<FusionSyntaxError>): List<String> =
    errors
        .map(FusionSyntaxError::getReadableDescription)
        .mapIndexed(::templateSyntaxErrorIndexed)

private fun templateParseException(
    source: FusionSourceIdentifier,
    errorsReadable: List<String>
) = """
Fusion parse error(s)
|-----------
| $source
| errors:
${errorsReadable.joinToString("\n").replace("\n", "\n|  ")}
""".trimIndent()

private fun templateSyntaxErrorIndexed(
    index: Int,
    syntaxErrorReadable: String
) = """
|  - error #$index:
$syntaxErrorReadable
--- end error #$index
""".trimIndent()

private fun templateSyntaxError(
    syntaxError: FusionSyntaxError
) = """
###### Fusion syntax error at ${syntaxError.codePosition}:
# type: ${syntaxError.errorType}
# message: ${syntaxError.message}
# offending symbol: ${templateOffendingSymbol(syntaxError.offendingSymbol)}
# parser class: ${syntaxError.parserClass}
###
cause: ${syntaxError.cause}
""".trimIndent()

private fun templateOffendingSymbol(
    offendingSymbol: FusionSyntaxError.OffendingSymbol?
) = if (offendingSymbol == null) "no symbol" else {
    "position ${offendingSymbol.codePosition}, text: ${offendingSymbol.text}"
}