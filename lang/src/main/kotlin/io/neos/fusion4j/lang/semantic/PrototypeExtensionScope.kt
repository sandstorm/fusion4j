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
 * Attribute declarations of prototypes that are only resolved when the current evaluation path and prototype
 * load path chain matches. Those declarations are called "Prototype Extensions".
 *
 * Prototype extension paths are a subset of all valid Fusion paths.
 *
 * A valid declared prototype extension path looks like this:
 * ( prototype-path-segment? scope-sub-path )+ extended-prototype-path-segment extended-sub-path
 *
 * In other words, a extension part contains tree values:
 *  1. the scope-part - ( prototype-path-segment? scope-sub-path )+
 *  2. the extended prototype part - extended-prototype-path-segment
 *  3. the extended sub path - extended-sub-path
 *
 * This class represents the scope-part of a prototype extension path.
 *
 * Example:
 * sub1.subN.prototype(A).sub1.subN.prototype(B).sub1.subN.prototype(N).sub1.subN.prototype(Extended).childPath1.childPathN
 *
 * The scope-part is: sub1.subN.prototype(A).sub1.subN.prototype(B).sub1.subN.prototype(N).sub1.subN
 *
 *
 * The scope of a prototype extension is used as key in the prototype to later access all active prototype
 * extensions for the current evaluation path.
 *
 * Examples:
 *
 * <code>
 * prototype(Foo).attribute1 = 1
 * myPath.prototype(Foo).attribute2 = 2
 *
 * // resolved attributes: attribute1 AND attribute2
 * myPath = Foo
 * // works nested as well
 * myPath.something = Foo
 * myPath.inner = Value {
 *   value = Foo
 * }
 * // resolved attributes: ONLY attribute1 (no extension)
 * otherPath = Foo
 * </code>
 *
 * <code>
 * some.prototype(SomePrototype).my.path.prototype(FooBar).attribute = 1
 * </code>
 */
data class PrototypeExtensionScope(
    val declarationPath: AbsoluteFusionPathName,
    val scopeList: List<PrototypeExtensionScopeSegment>
) : Comparable<PrototypeExtensionScope> {

    val scopeParts: List<PrototypeExtensionScopePart> =
        scopeList
            .fold(PrototypeExtensionScopePartBuilder()) { result, segment ->
                when (segment) {
                    is PrototypeExtensionScopePathSegment -> result.nextProperty(segment.pathSegment)
                    is PrototypeExtensionScopePrototypeSegment -> result.nextPrototype(segment.inheritedPrototypes.size + 1)
                    else -> throw IllegalArgumentException("Could not build scope parts; unknown segment type $segment")
                }
            }
            .build()

    companion object {
        fun createFromDeclarationPath(
            pathName: AbsoluteFusionPathName,
            inheritanceIndex: Map<QualifiedPrototypeName, List<QualifiedPrototypeName>>
        ): PrototypeExtensionScope {
            return PrototypeExtensionScope(
                pathName,
                pathName.segments
                    .map { segment ->
                        when (segment) {
                            is FusionPropertyPathSegment -> PrototypeExtensionScopePathSegment(segment)
                            is PrototypeCallPathSegment -> PrototypeExtensionScopePrototypeSegment(
                                segment.prototypeName,
                                inheritanceIndex[segment.prototypeName]
                                    ?: throw IllegalArgumentException(
                                        "Cannot build scope from path segment $segment;" +
                                                " prototype ${segment.prototypeName} not listed in inheritance index"
                                    )
                            )
                            else -> throw IllegalArgumentException("Cannot build scope from path segment $segment; unknown path segment type")
                        }
                    }
            )
        }

    }

    fun prototypePath(prototypeName: QualifiedPrototypeName): AbsoluteFusionPathName =
        declarationPath.appendPrototypeCallSegment(prototypeName)

    /**
     * A given evaluation path is "in scope" of this prototype extension scope, if all path segments of
     * the scope match the following conditions:
     *  - the paths are equal OR the scope path is any parent of the evaluation path
     *  - the prototypes are equal OR the evaluation prototype is any inherited prototype of the scope prototype
     */
    fun isInScope(evaluationPath: EvaluationPath): Boolean {
        val evaluationScopePath = evaluationPath.toScopePath()
        scopeList
            .fold(ScopeCheck(evaluationScopePath.segments)) { check, scopeSegment ->
                if (check.remainingEvaluationPath.isEmpty()) {
                    return false
                }
                when (scopeSegment) {
                    // path
                    is PrototypeExtensionScopePathSegment -> {
                        val nextEvaluationSegmentIdx =
                            check.remainingEvaluationPath.indexOfFirst { it is FusionPropertyPathSegment }
                        if (nextEvaluationSegmentIdx >= 0) {
                            val nextEvaluationSegment = check.remainingEvaluationPath[nextEvaluationSegmentIdx]
                            if (nextEvaluationSegment is FusionPropertyPathSegment &&
                                scopeSegment.isInScope(nextEvaluationSegment)
                            ) {
                                check.foundIndex(nextEvaluationSegmentIdx)
                            } else {
                                return false
                            }
                        } else {
                            return false
                        }
                    }
                    // prototype
                    is PrototypeExtensionScopePrototypeSegment -> {
                        val firstPrototypeIdx = check.remainingEvaluationPath
                            .indexOfFirst { it is PrototypeCallPathSegment && scopeSegment.isInScope(it.prototypeName) }
                        if (firstPrototypeIdx < 0) {
                            return false
                        } else {
                            check.foundIndex(firstPrototypeIdx)
                        }
                    }
                    else -> throw IllegalArgumentException("Cannot build scope from path segment $scopeSegment; unknown path segment type")
                }
            }
        return true
    }

    override fun compareTo(other: PrototypeExtensionScope): Int {
        if (this == other || declarationPath == other.declarationPath) {
            return 0
        }
        val sizeCompare = other.scopeParts.size.compareTo(scopeParts.size)
        if (sizeCompare != 0) {
            return sizeCompare
        }
        scopeParts.indices.forEach { idx ->
            val thisScopePart = scopeParts[idx]
            val otherScopePart = other.scopeParts[idx]
            val partSizeCompare = thisScopePart.relativePath.segments.size
                .compareTo(otherScopePart.relativePath.segments.size)
            if (partSizeCompare != 0) {
                return partSizeCompare
            }
            val prototypeCompare = otherScopePart.inheritanceDepth
                .compareTo(thisScopePart.inheritanceDepth)
            if (prototypeCompare != 0) {
                return partSizeCompare
            }
        }
        throw IllegalArgumentException("Could not compare scopes $declarationPath and ${other.declarationPath}")
    }

    override fun toString(): String = scopeList.joinToString("")
}

data class ScopeCheck(
    val remainingEvaluationPath: List<FusionPathNameSegment>
) {
    fun foundIndex(idx: Int): ScopeCheck =
        ScopeCheck(remainingEvaluationPath.subList(idx + 1, remainingEvaluationPath.size))
}

data class PrototypeExtensionScopePart(
    val relativePath: RelativeFusionPathName,
    val inheritanceDepth: Int
)

data class PrototypeExtensionScopePartBuilder(
    val currentPath: RelativeFusionPathName = FusionPathName.current(),
    val result: List<PrototypeExtensionScopePart> = emptyList()
) {
    fun nextProperty(segment: FusionPropertyPathSegment): PrototypeExtensionScopePartBuilder =
        PrototypeExtensionScopePartBuilder(currentPath.appendSegment(segment), result)

    fun nextPrototype(inheritanceDepth: Int): PrototypeExtensionScopePartBuilder =
        PrototypeExtensionScopePartBuilder(
            FusionPathName.current(),
            result + PrototypeExtensionScopePart(currentPath, inheritanceDepth)
        )

    fun build(): List<PrototypeExtensionScopePart> =
        result + if (currentPath.segments.isNotEmpty()) listOf(
            PrototypeExtensionScopePart(
                currentPath,
                Int.MAX_VALUE
            )
        ) else emptyList()
}

interface PrototypeExtensionScopeSegment {
}

data class PrototypeExtensionScopePathSegment(
    val pathSegment: FusionPropertyPathSegment
) : PrototypeExtensionScopeSegment {
    fun isInScope(segment: FusionPropertyPathSegment): Boolean = pathSegment == segment

    override fun toString(): String = "$pathSegment"
}

data class PrototypeExtensionScopePrototypeSegment(
    val prototypeScope: QualifiedPrototypeName,
    val inheritedPrototypes: List<QualifiedPrototypeName>
) : PrototypeExtensionScopeSegment {
    fun isInScope(prototypeName: QualifiedPrototypeName): Boolean =
        prototypeScope == prototypeName || inheritedPrototypes.contains(prototypeName)

    override fun toString(): String = "[$prototypeScope -> ${inheritedPrototypes.joinToString(" -> ") { "$it" }}]"
}