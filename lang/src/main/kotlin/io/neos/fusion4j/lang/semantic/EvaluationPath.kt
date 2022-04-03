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

package io.neos.fusion4j.lang.semantic

import io.neos.fusion4j.lang.model.*

/**
 * Evaluation path at runtime. Contains all nested evaluation paths and its types.
 */
data class EvaluationPath(
    val segments: List<EvaluationPathSegment>
) {

    val currentType: QualifiedPrototypeName? get() = segments.findLast { it.type != null }?.type

    val currentPrototypeName: QualifiedPrototypeName
        get() = currentType ?: throw IllegalStateException("Evaluation path $this must have an explicit type")

    fun toDeclarationPath(): AbsoluteFusionPathName =
        AbsoluteFusionPathName.fromSegments(
            segments.flatMap { it.nestedPath.segments }
        )

    fun toScopePath(): AbsoluteFusionPathName =
        AbsoluteFusionPathName.fromSegments(
            segments.flatMap {
                it.nestedPath.segments + if (it.type != null) listOf(PrototypeCallPathSegment.create(it.type)) else emptyList()
            }
        )

    // TODO print on error
    fun print(): String {
        return segments.joinToString("") { "${it.nestedPath}${if (it.type != null) "<${it.type}>" else ""}" }
    }


    companion object {
        fun initialAbsolute(absolutePath: AbsoluteFusionPathName, type: QualifiedPrototypeName?): EvaluationPath {
            return EvaluationPath(
                listOf(
                    EvaluationPathSegment(
                        absolutePath.relativeToRoot(),
                        type
                    )
                )
            )
        }

        fun parseFromString(pathString: String): EvaluationPath {
            val parts = pathString.split("/")
                .map {
                    if (it.matches(Regex(".*?<.*?>"))) {
                        val idx = it.indexOf('<')
                        it.substring(0, idx) to it.substring(idx + 1, it.length - 1)
                    } else {
                        it to null
                    }
                }
            return EvaluationPath(
                parts.subList(0, parts.size).map {
                    EvaluationPathSegment(
                        FusionPathName.parseRelativePrependDot(it.first),
                        if (it.second != null) QualifiedPrototypeName.fromString(it.second!!) else null,
                    )
                }
            )
        }
    }

}

data class EvaluationPathSegment(
    val nestedPath: RelativeFusionPathName,
    val type: QualifiedPrototypeName?
)